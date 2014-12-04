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


import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handler for a single benchmark.
 *
 * Handles name and execution information (# iterations, etc).
 * Executes the benchmark according to above parameters.
 */
class LoopBenchmarkHandler extends BaseBenchmarkHandler {

    private final Method method;

    LoopBenchmarkHandler(OutputFormat format, Class<?> clazz, Method method, Options options, BenchmarkParams executionParams) {
        super(format, clazz, options, executionParams);
        this.method = method;
    }

    @Override
    public IterationResult runIteration(BenchmarkParams benchmarkParams, IterationParams params, boolean last) {
        int numThreads = benchmarkParams.getThreads();
        TimeValue runtime = params.getTime();

        CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);

        // result object to accumulate the results in
        IterationResult iterationResults = new IterationResult(benchmarkParams, params);

        InfraControl control = new InfraControl(benchmarkParams, params, preSetupBarrier, preTearDownBarrier, last);

        // preparing the worker runnables
        BenchmarkTask[] runners = new BenchmarkTask[numThreads];

        ThreadParams[] threadParamses = distributeThreads(numThreads, benchmarkParams.getThreadGroups());
        for (int i = 0; i < runners.length; i++) {
            runners[i] = new BenchmarkTask(control, threadParamses[i]);
        }

        long waitDeadline = System.nanoTime() + benchmarkParams.getTimeout().convertTo(TimeUnit.NANOSECONDS);

        // profilers start way before the workload starts to capture
        // the edge behaviors.
        startProfilers(benchmarkParams, params);

        // submit tasks to threadpool
        Map<BenchmarkTask, Future<Collection<? extends Result>>> results = new HashMap<BenchmarkTask, Future<Collection<? extends Result>>>();
        for (BenchmarkTask runner : runners) {
            results.put(runner, executor.submit(runner));
        }

        // wait for all workers to transit to measurement
        control.awaitWarmupReady();

        // wait for the iteration time to expire
        switch (benchmarkParams.getMode()) {
            case SingleShotTime:
                // don't wait here, block on timed result Future
                break;
            default:
                try {
                    runtime.sleep();
                } catch (InterruptedException e) {
                    // regardless...
                }
        }

        // now we communicate all worker threads should stop
        control.isDone = true;

        // wait for all workers to transit to teardown
        control.awaitWarmdownReady();

        // Wait for the result, continuously polling the worker threads.
        // The abrupt exception in any worker will float up here.
        int expected = numThreads;
        while (expected > 0) {
            for (Map.Entry<BenchmarkTask, Future<Collection<? extends Result>>> re : results.entrySet()) {
                BenchmarkTask task = re.getKey();
                Future<Collection<? extends Result>> fr = re.getValue();
                try {
                    long waitFor = Math.max(TimeUnit.MILLISECONDS.toNanos(100), waitDeadline - System.nanoTime());
                    fr.get(waitFor, TimeUnit.NANOSECONDS);
                    expected--;
                } catch (InterruptedException ex) {
                    throw new BenchmarkException(ex);
                } catch (ExecutionException ex) {
                    // unwrap: ExecutionException -> Throwable-wrapper -> InvocationTargetException
                    Throwable cause = ex.getCause().getCause().getCause();
                    throw new BenchmarkException(cause);
                } catch (TimeoutException e) {
                    // try to kick the thread, if it was already started
                    Thread runner = task.runner;
                    if (runner != null) {
                        out.print("(*interrupt*) ");
                        runner.interrupt();
                    }
                }
            }
        }

        // Get the results.
        // Should previous loop allow us to get to this point, we can fully expect
        // all the results ready without the exceptions.
        for (Future<Collection<? extends Result>> fr : results.values()) {
            try {
                iterationResults.addResults(fr.get());
            } catch (InterruptedException ex) {
                throw new IllegalStateException("Impossible to be here");
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Impossible to be here");
            }
        }

        // profilers stop when after all threads are confirmed to be
        // finished to capture the edge behaviors
        stopProfilers(benchmarkParams, params, iterationResults);

        return iterationResults;
    }

    /**
     * Worker body.
     */
    class BenchmarkTask implements Callable<Collection<? extends Result>> {

        private volatile Thread runner;
        private final InfraControl control;
        private final ThreadParams threadParams;

        BenchmarkTask(InfraControl control, ThreadParams threadParams) {
            this.control = control;
            this.threadParams = threadParams;
        }

        @Override
        public Collection<? extends Result> call() throws Exception {
            try {
                // bind the executor thread
                runner = Thread.currentThread();

                // go for the run
                return (Collection<? extends Result>) method.invoke(instances.get(), control, threadParams);
            } catch (Throwable e) {
                // about to fail the iteration;
                // compensate for missed sync-iteration latches, we don't care about that anymore
                control.preSetupForce();
                control.preTearDownForce();

                if (control.benchmarkParams.shouldSynchIterations()) {
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
            } finally {
                // unbind the executor thread
                runner = null;
            }
        }

    }

}
