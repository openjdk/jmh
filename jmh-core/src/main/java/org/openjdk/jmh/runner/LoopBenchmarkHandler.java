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
import org.openjdk.jmh.infra.InfraControl;
import org.openjdk.jmh.infra.ThreadControl;
import org.openjdk.jmh.infra.results.IterationResult;
import org.openjdk.jmh.infra.results.Result;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;

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
    private final boolean shouldSynchIterations;

    LoopBenchmarkHandler(OutputFormat format, BenchmarkRecord microbenchmark, Class<?> clazz, Method method, Options options, BenchmarkParams executionParams) {
        super(format, microbenchmark, clazz, options, executionParams);
        this.method = method;
        this.shouldSynchIterations = (microbenchmark.getMode() != Mode.SingleShotTime) && executionParams.shouldSynchIterations();
    }

    @Override
    public IterationResult runIteration(IterationParams params, boolean last) {
        int numThreads = params.getBenchmarkParams().getThreads();
        TimeValue runtime = params.getTime();

        CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);

        // result object to accumulate the results in
        IterationResult iterationResults = new IterationResult(microbenchmark, params);

        InfraControl control = new InfraControl(numThreads, shouldSynchIterations, runtime, preSetupBarrier, preTearDownBarrier, last, timeUnit, params.getBatchSize(), opsPerInvocation, microbenchmark.getActualParams());

        // preparing the worker runnables
        BenchmarkTask[] runners = new BenchmarkTask[numThreads];

        int[] groups = params.getBenchmarkParams().getThreadGroups();
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
            runners[i] = new BenchmarkTask(control, threadControl);
        }

        // profilers start way before the workload starts to capture
        // the edge behaviors.
        startProfilers();

        // submit tasks to threadpool
        Map<BenchmarkTask, Future<Collection<? extends Result>>> results = new HashMap<BenchmarkTask, Future<Collection<? extends Result>>>();
        for (BenchmarkTask runner : runners) {
            results.put(runner, executor.submit(runner));
        }

        // wait for all workers to transit to measurement
        while (control.warmupShouldWait) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        // wait for the iteration time to expire
        try {
            runtime.sleep();
        } catch (InterruptedException e) {
            // regardless...
        }

        // now we communicate all worker threads should stop
        control.isDone = true;

        // wait for all workers to transit to teardown
        while (control.warmdownShouldWait) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        // Adjust waiting intervals:
        //  - We don't know the running time for SingleShot benchmarks,
        //    we wait for at least 10 minutes for benchmark to stop; this
        //    can be adjusted with usual warmup/measurement duration settings;
        //  - For other benchmarks, we wait for twice the run time,
        //    but at least 5 seconds to cover for low run times.
        long timeToWait;
        switch (microbenchmark.getMode()) {
            case SingleShotTime:
                timeToWait = Math.max(TimeUnit.SECONDS.toNanos(600), runtime.convertTo(TimeUnit.NANOSECONDS));
                break;
            default:
                timeToWait = Math.max(runtime.convertTo(TimeUnit.NANOSECONDS) * 2, TimeUnit.SECONDS.toNanos(5));
        }

        // Wait for the result, continuously polling the worker threads.
        // The abrupt exception in any worker will float up here.
        int expected = numThreads;
        while (expected > 0) {
            for (BenchmarkTask task : results.keySet()) {
                Future<Collection<? extends Result>> fr = results.get(task);
                try {
                    fr.get(timeToWait, TimeUnit.NANOSECONDS);
                    expected--;
                } catch (InterruptedException ex) {
                    throw new BenchmarkException(ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause().getCause(); // unwrap
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
        stopProfilers(iterationResults);

        return iterationResults;
    }

    /**
     * Worker body.
     */
    class BenchmarkTask implements Callable<Collection<? extends Result>> {

        private volatile Thread runner;
        private final InfraControl control;
        private final ThreadControl threadControl;

        BenchmarkTask(InfraControl control, ThreadControl threadControl) {
            this.control = control;
            this.threadControl = threadControl;
        }

        @Override
        public Collection<? extends Result> call() throws Exception {
            try {
                // bind the executor thread
                runner = Thread.currentThread();

                // go for the run
                return (Collection<? extends Result>) method.invoke(instances.get(), control, threadControl);
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
            } finally {
                // unbind the executor thread
                runner = null;
            }
        }

    }

}
