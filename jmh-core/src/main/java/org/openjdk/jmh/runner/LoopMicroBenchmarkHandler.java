/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.runner;


import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.logic.Global;
import org.openjdk.jmh.logic.Loop;
import org.openjdk.jmh.logic.results.IterationData;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.BaseOptions;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Handler for a single micro benchmark (with Loop).
 * Handles name and execution information (# iterations, et c). Executes the
 * benchmark according to above parameters.
 *
 * @author staffan.friberg@oracle.com
 * @author anders.astrand@oracle.com
 * @author aleksey.shipilev@oracle.com
 * @author sergey.kuksenko@oracle.com
 */
public class LoopMicroBenchmarkHandler extends BaseMicroBenchmarkHandler {

    private final Method method;

    private final boolean shouldSynchIterations;

    /* output options */
    private final boolean shouldFailOnError;

    private final Mode mode;

    /**
     * Constructor
     *
     * @param microbenchmark  Name of micro benchmark
     * @param clazz
     * @param method
     * @param options      Options from the command line
     * @param executionParams
     */
    LoopMicroBenchmarkHandler(OutputFormat format, BenchmarkRecord microbenchmark, Class<?> clazz, Method method, BaseOptions options, MicroBenchmarkParameters executionParams) {
        super(format, microbenchmark, clazz, options, executionParams);
        this.method = method;
        this.shouldSynchIterations = (microbenchmark.getMode() != Mode.SingleShotTime) && executionParams.shouldSynchIterations();
        this.shouldFailOnError = options.shouldFailOnError();
        this.mode = microbenchmark.getMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IterationData runIteration(int numThreads, TimeValue runtime, boolean last) {
        // bring up the barrier
        CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);

        Global global = new Global(numThreads, shouldSynchIterations);

        IterationData iterationResults = new IterationData(microbenchmark, numThreads, runtime);

        BenchmarkTask[] runners = new BenchmarkTask[numThreads];
        for (int i = 0; i < runners.length; i++) {
            runners[i] = new BenchmarkTask(threadLocal, new Loop(global, runtime, preSetupBarrier, preTearDownBarrier, last, timeUnit));
        }

        // submit tasks to threadpool
        List<Future<Result>> resultList = new ArrayList<Future<Result>>(numThreads);
        for (BenchmarkTask runner : runners) {
            resultList.add(executor.submit(runner));
        }

        // legacy mode has no knowledge about setup/teardown barriers
        if (mode != Mode.Legacy) {
            // wait for all threads to start executing
            try {
                preSetupBarrier.await();
            } catch (InterruptedException ex) {
                log(ex);
            }
            startProfilers();

            // wait for all threads to stop executing
            try {
                preTearDownBarrier.await();
            } catch (InterruptedException ex) {
                log(ex);
            }
            stopProfilers(iterationResults);
        }

        // wait for the result, continuously polling the worker threads.
        //
        int expected = numThreads;
        while (expected > 0) {
            for (Future<Result> fr : resultList) {
                try {
                    fr.get(runtime.getTime() * 2, runtime.getTimeUnit());
                    expected--;
                } catch (InterruptedException ex) {
                    log(ex);
                    iterationResults.clearResults();
                    return iterationResults;
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause().getCause(); // unwrap
                    log(cause);
                    iterationResults.clearResults();
                    if (shouldFailOnError) {
                        throw new IllegalStateException(cause.getMessage(), cause);
                    }
                    return iterationResults;
                } catch (TimeoutException e) {
                    // do nothing, respin
                }
            }
        }

        // get the results
        for (Future<Result> fr : resultList) {
            try {
                iterationResults.addResult(fr.get());
            } catch (InterruptedException ex) {
                throw new IllegalStateException("Impossible to be here");
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Impossible to be here");
            }
        }

        return iterationResults;
    }

    /**
     * Task to submit to the ExecutorService. Will execute one iteration and return the Result.
     *
     * @author anders.astrand@oracle.com
     */
    class BenchmarkTask implements Callable<Result> {

        /**
         * Microbenchmark instance to execute on
         */
        private final ThreadLocal<InstanceProvider> invocationHandler;
        /**
         * Loop variable
         */
        private final Loop loop;

        /**
         * Constructor
         *
         * @param invocationHandler    instance to execute on
         * @param loop       Loop variable
         */
        BenchmarkTask(ThreadLocal<InstanceProvider> invocationHandler, Loop loop) {
            this.invocationHandler = invocationHandler;
            this.loop = loop;
        }

        @Override
        public Result call() throws Exception {
            Result r;
            try {
                r = invokeBenchmark(invocationHandler.get().getInstance(), loop);

            } catch (Throwable e) {
                // about to fail the iteration;
                // compensate for missed sync-iteration latches, we don't care about that anymore
                loop.preSetupForce();
                loop.preTearDownForce();

                if (shouldSynchIterations) {
                    try {
                        loop.global.announceWarmupReady();
                    } catch (Exception e1) {
                        // more threads than expected
                    }

                    try {
                        loop.global.announceWarmdownReady();
                    } catch (Exception e1) {
                        // more threads than expected
                    }
                }

                throw new Exception(e); // wrapping Throwable
            }

            return r;
        }

        /**
         * Helper method for running the benchmark in a given instance.
         *
         * @param loop      Loop logic instance
         * @return the Result of the execution
         * @throws Exception if something went wrong
         */

        private Result invokeBenchmark(Object instance, Loop loop) throws Throwable {
            Result result;
            if (method != null) {
                try {
                    result = (Result) method.invoke(instance, loop);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Can't invoke " + method.getDeclaringClass().getName() + "." + method.getName(), e);
                } catch (InvocationTargetException e) {
                    throw e.getCause(); // unwrap
                }
            } else {
                throw new IllegalStateException("Unable to find method to run");
            }
            return result;
        }


    }

}
