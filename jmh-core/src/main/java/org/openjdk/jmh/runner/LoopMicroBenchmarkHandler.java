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
import org.openjdk.jmh.logic.InfraControl;
import org.openjdk.jmh.logic.ThreadControl;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;
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
 * Handler for a single micro benchmark (with InfraControl).
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
    private final boolean shouldFailOnError;

    LoopMicroBenchmarkHandler(OutputFormat format, BenchmarkRecord microbenchmark, Class<?> clazz, Method method, Options options, BenchmarkParams executionParams) {
        super(format, microbenchmark, clazz, options, executionParams);
        this.method = method;
        this.shouldSynchIterations = (microbenchmark.getMode() != Mode.SingleShotTime) && executionParams.shouldSynchIterations();
        this.shouldFailOnError = options.shouldFailOnError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IterationResult runIteration(IterationParams params, boolean last) {
        int numThreads = params.getThreads();
        TimeValue runtime = params.getTime();

        CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);

        // result object to accumulate the results in
        IterationResult iterationResults = new IterationResult(microbenchmark, params);

        InfraControl control = new InfraControl(numThreads, shouldSynchIterations, runtime, preSetupBarrier, preTearDownBarrier, last, timeUnit);

        // preparing the worker runnables
        BenchmarkTask[] runners = new BenchmarkTask[numThreads];

        int[] groups = params.getThreadGroups();
        int currentGroup = 0;
        int currentSubgroup = 0;
        int remainingSubgroupThreads = groups[currentSubgroup];
        for (int i = 0; i < runners.length; i++) {
            while (remainingSubgroupThreads == 0) {
                currentSubgroup++;
                if (currentSubgroup == groups.length) {
                    currentSubgroup = 0;
                    currentGroup++;
                }
                remainingSubgroupThreads = groups[currentSubgroup];
            }
            remainingSubgroupThreads--;

            ThreadControl threadControl = new ThreadControl(currentGroup, currentSubgroup);
            runners[i] = new BenchmarkTask(threadLocal, control, threadControl);
        }

        // submit tasks to threadpool
        List<Future<Result>> results = new ArrayList<Future<Result>>(numThreads);
        for (BenchmarkTask runner : runners) {
            results.add(executor.submit(runner));
        }

        // wait for all workers to initialize and ready to go
        try {
            preSetupBarrier.await();
        } catch (InterruptedException ex) {
            log(ex);
        }

        // profilers start when iteration starts
        startProfilers();

        // wait for the iteration time to expire, then set the termination flag
        try {
            runtime.sleep();
        } catch (InterruptedException e) {
            // regardless...
        }
        control.isDone = true;

        // wait for all workers to complete run and ready to proceed
        try {
            preTearDownBarrier.await();
        } catch (InterruptedException ex) {
            log(ex);
        }

        // profilers stop when iteration ends
        stopProfilers(iterationResults);

        // Wait for the result, continuously polling the worker threads.
        // The abrupt exception in any worker will float up here.
        int expected = numThreads;
        while (expected > 0) {
            for (Future<Result> fr : results) {
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

        // Get the results.
        // Should previous loop allow us to get to this point, we can fully expect
        // all the results ready without the exceptions.
        for (Future<Result> fr : results) {
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
     * Worker body.
     */
    class BenchmarkTask implements Callable<Result> {

        private final ThreadLocal<InstanceProvider> invocationHandler;
        private final InfraControl control;
        private final ThreadControl threadControl;

        BenchmarkTask(ThreadLocal<InstanceProvider> invocationHandler, InfraControl control, ThreadControl threadControl) {
            this.invocationHandler = invocationHandler;
            this.control = control;
            this.threadControl = threadControl;
        }

        @Override
        public Result call() throws Exception {
            try {
                return invokeBenchmark(invocationHandler.get().getInstance(), control, threadControl);
            } catch (Throwable e) {
                // about to fail the iteration;
                // compensate for missed sync-iteration latches, we don't care about that anymore
                control.preSetupForce();
                control.preTearDownForce();

                if (shouldSynchIterations) {
                    try {
                        control.announceWarmupReady();
                    } catch (Exception e1) {
                        // more threads than expected
                    }

                    try {
                        control.announceWarmdownReady();
                    } catch (Exception e1) {
                        // more threads than expected
                    }
                }

                throw new Exception(e); // wrapping Throwable
            }
        }

        /**
         * Helper method for running the benchmark in a given instance.
         */
        private Result invokeBenchmark(Object instance, InfraControl control, ThreadControl threadControl) throws Throwable {
            Result result;
            if (method != null) {
                try {
                    result = (Result) method.invoke(instance, control, threadControl);
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
