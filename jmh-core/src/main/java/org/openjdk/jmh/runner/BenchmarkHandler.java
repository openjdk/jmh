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
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;


/**
 * Base class for all benchmarks handlers.
 */
class BenchmarkHandler {

    /**
     * Thread-pool for threads executing the benchmark tasks
     */
    private final ExecutorService executor;

    // (Aleksey) Forgive me, Father, for I have sinned.
    private final ThreadLocal<ThreadData> threadData;

    private final OutputFormat out;
    private final List<InternalProfiler> profilers;
    private final List<InternalProfiler> profilersRev;

    private final Method method;

    public BenchmarkHandler(OutputFormat out, Options options, BenchmarkParams executionParams) {
        String target = executionParams.generatedBenchmark();
        int lastDot = target.lastIndexOf('.');
        final Class<?> clazz = ClassUtils.loadClass(target.substring(0, lastDot));

        this.method = BenchmarkHandler.findBenchmarkMethod(clazz, target.substring(lastDot + 1));
        this.profilers = ProfilerFactory.getSupportedInternal(options.getProfilers());
        this.profilersRev = new ArrayList<>(profilers);
        Collections.reverse(profilersRev);

        final BlockingQueue<ThreadParams> tps = new ArrayBlockingQueue<>(executionParams.getThreads());
        tps.addAll(distributeThreads(executionParams.getThreads(), executionParams.getThreadGroups()));

        this.threadData = new ThreadLocal<ThreadData>() {
            @Override
            protected ThreadData initialValue() {
                try {
                    Object o = clazz.getConstructor().newInstance();
                    ThreadParams t = tps.poll();
                    if (t == null) {
                        throw new IllegalStateException("Cannot get another thread params");
                    }
                    return new ThreadData(o, t);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException("Class " + clazz.getName() + " instantiation error ", e);
                }
            }
        };

        this.out = out;
        try {
            this.executor = EXECUTOR_TYPE.createExecutor(executionParams.getThreads(), executionParams.getBenchmark());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static List<ThreadParams> distributeThreads(int threads, int[] groups) {
        List<ThreadParams> result = new ArrayList<>();
        int totalGroupThreads = Utils.sum(groups);
        int totalGroups = (int) Math.ceil(1D * threads / totalGroupThreads);
        int totalSubgroups = groups.length;

        int currentGroupThread = 0;
        int currentSubgroupThread = 0;
        int currentGroup = 0;
        int currentSubgroup = 0;
        for (int t = 0; t < threads; t++) {
            while (currentSubgroupThread >= groups[currentSubgroup]) {
                currentSubgroup++;
                if (currentSubgroup == groups.length) {
                    currentGroup++;
                    currentSubgroup = 0;
                    currentGroupThread = 0;
                }
                currentSubgroupThread = 0;
            }

            result.add(new ThreadParams(
                    t, threads,
                    currentGroup, totalGroups,
                    currentSubgroup, totalSubgroups,
                    currentGroupThread, totalGroupThreads,
                    currentSubgroupThread, groups[currentSubgroup]
                  )
            );

            currentGroupThread++;
            currentSubgroupThread++;
        }
        return result;
    }

    public static Method findBenchmarkMethod(Class<?> clazz, String methodName) {
        Method method = null;
        for (Method m : ClassUtils.enumerateMethods(clazz)) {
            if (m.getName().equals(methodName)) {
                if (isValidBenchmarkSignature(m)) {
                    if (method != null) {
                        throw new IllegalArgumentException("Ambiguous methods: \n" + method + "\n and \n" + m + "\n, which one to execute?");
                    }
                    method = m;
                } else {
                    throw new IllegalArgumentException("Benchmark parameters do not match the signature contract.");
                }
            }
        }
        if (method == null) {
            throw new IllegalArgumentException("No matching methods found in benchmark");
        }
        return method;
    }

    /**
     * checks if method signature is valid benchmark signature,
     * besited checks if method signature corresponds to benchmark type.
     * @param m
     * @return
     */
    private static boolean isValidBenchmarkSignature(Method m) {
        if (m.getReturnType() != BenchmarkTaskResult.class) {
            return false;
        }
        final Class<?>[] parameterTypes = m.getParameterTypes();

        if (parameterTypes.length != 2) {
            return false;
        }

        if (parameterTypes[0] != InfraControl.class) {
            return false;
        }

        if (parameterTypes[1] != ThreadParams.class) {
            return false;
        }

        return true;
    }

    private static final ExecutorType EXECUTOR_TYPE = Enum.valueOf(ExecutorType.class, System.getProperty("jmh.executor", ExecutorType.FIXED_TPE.name()));

    private enum ExecutorType {

        /**
         * Use Executors.newCachedThreadPool
         */
        CACHED_TPE {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) {
                return Executors.newCachedThreadPool(new WorkerThreadFactory(prefix));
            }
        },

        /**
         * Use Executors.newFixedThreadPool
         */
        FIXED_TPE {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) {
                return Executors.newFixedThreadPool(maxThreads, new WorkerThreadFactory(prefix));
            }
        },

        /**
         * Use ForkJoinPool.
         */
        FJP {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) throws Exception {
                return new ForkJoinPool(maxThreads);
            }
        },

        /**
         * Use ForkJoinPool.commonPool (JDK 8+)
         */
        FJP_COMMON {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) throws Exception {
                // (Aleksey):
                // requires some of the reflection magic to untie from JDK 8 compile-time dependencies
                Method m = Class.forName("java.util.concurrent.ForkJoinPool").getMethod("commonPool");
                return (ExecutorService) m.invoke(null);
            }

            @Override
            boolean shutdownForbidden() {
                // this is a system-wide executor, don't shutdown
                return true;
            }

        },

        CUSTOM {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) throws Exception {
                String className = System.getProperty("jmh.executor.class");
                return (ExecutorService) Class.forName(className).getConstructor(int.class, String.class)
                        .newInstance(maxThreads, prefix);
            }
        },

        ;

        abstract ExecutorService createExecutor(int maxThreads, String prefix) throws Exception;

        boolean shutdownForbidden() {
            return false;
        }
    }

    protected void startProfilers(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        // start profilers
        for (InternalProfiler prof : profilers) {
            try {
                prof.beforeIteration(benchmarkParams, iterationParams);
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    protected void stopProfilers(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iterationResults) {
        // stop profilers
        for (InternalProfiler prof : profilersRev) {
            try {
                iterationResults.addResults(prof.afterIteration(benchmarkParams, iterationParams, iterationResults));
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    /**
     * Do required shutdown actions.
     */
    public void shutdown() {
        if (EXECUTOR_TYPE.shutdownForbidden() || (executor == null)) {
            return;
        }
        while (true) {
            executor.shutdown();

            try {
                if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            out.println("Failed to stop executor service " + executor + ", trying again; check for the unaccounted running threads");
        }
    }

    /**
     * Runs an iteration on the handled benchmark.
     *
     * @param benchmarkParams Benchmark parameters
     * @param params  Iteration parameters
     * @param last    Should this iteration considered to be the last
     * @return IterationResult
     */
    public IterationResult runIteration(BenchmarkParams benchmarkParams, IterationParams params, boolean last) {
        int numThreads = benchmarkParams.getThreads();
        TimeValue runtime = params.getTime();

        CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);

        // result object to accumulate the results in
        List<Result> iterationResults = new ArrayList<>();

        InfraControl control = new InfraControl(benchmarkParams, params,
                preSetupBarrier, preTearDownBarrier, last,
                new Control());

        // preparing the worker runnables
        BenchmarkTask[] runners = new BenchmarkTask[numThreads];
        for (int i = 0; i < runners.length; i++) {
            runners[i] = new BenchmarkTask(control);
        }

        long waitDeadline = System.nanoTime() + benchmarkParams.getTimeout().convertTo(TimeUnit.NANOSECONDS);

        // profilers start way before the workload starts to capture
        // the edge behaviors.
        startProfilers(benchmarkParams, params);

        // submit tasks to threadpool
        List<Future<BenchmarkTaskResult>> completed = new ArrayList<>();
        CompletionService<BenchmarkTaskResult> srv = new ExecutorCompletionService<>(executor);
        for (BenchmarkTask runner : runners) {
            srv.submit(runner);
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
                    Future<BenchmarkTaskResult> failing = srv.poll(runtime.convertTo(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                    if (failing != null) {
                        // Oops, some task has exited prematurely, without isDone check.
                        // Must be an exception. Record the failing result, and lift the
                        // timeout deadline: we care only to exit as fast as possible now.
                        completed.add(failing);
                        waitDeadline = System.nanoTime();
                    }
                } catch (InterruptedException e) {
                    // regardless...
                }
        }

        // now we communicate all worker threads should stop
        control.announceDone();

        // wait for all workers to transit to teardown
        control.awaitWarmdownReady();

        // Wait for the result, handling timeouts
        while (completed.size() < numThreads) {
            try {
                long waitFor = Math.max(TimeUnit.MILLISECONDS.toNanos(100), waitDeadline - System.nanoTime());
                Future<BenchmarkTaskResult> fr = srv.poll(waitFor, TimeUnit.NANOSECONDS);
                if (fr == null) {
                    // We are in the timeout mode now, kick all the still running threads.
                    out.print("(*interrupt*) ");
                    for (BenchmarkTask task : runners) {
                        Thread runner = task.runner;
                        if (runner != null) {
                            runner.interrupt();
                        }
                    }
                } else {
                    completed.add(fr);
                }
            } catch (InterruptedException ex) {
                throw new BenchmarkException(ex);
            }
        }

        // Process the results: we get here after all worker threads have quit,
        // either normally or abnormally. This means, Future.get() would never block.
        long allOps = 0;
        long measuredOps = 0;

        List<Throwable> errors = new ArrayList<>();
        for (Future<BenchmarkTaskResult> fr : completed) {
            try {
                BenchmarkTaskResult btr = fr.get();
                iterationResults.addAll(btr.getResults());
                allOps += btr.getAllOps();
                measuredOps += btr.getMeasuredOps();
            } catch (ExecutionException ex) {
                // unwrap: ExecutionException -> Throwable-wrapper -> InvocationTargetException
                Throwable cause = ex.getCause().getCause().getCause();

                // record exception, unless it is the assist exception
                if (!(cause instanceof FailureAssistException)) {
                    errors.add(cause);
                }
            } catch (InterruptedException ex) {
                // cannot happen here, Future.get() should never block.
                throw new BenchmarkException(ex);
            }
        }

        IterationResult result = new IterationResult(benchmarkParams, params, new IterationResultMetaData(allOps, measuredOps));
        result.addResults(iterationResults);

        // profilers stop when after all threads are confirmed to be
        // finished to capture the edge behaviors; or, on a failure path
        stopProfilers(benchmarkParams, params, result);

        if (!errors.isEmpty()) {
            throw new BenchmarkException("Benchmark error during the run", errors);
        }

        return result;
    }

    /**
     * Worker body.
     */
    class BenchmarkTask implements Callable<BenchmarkTaskResult> {
        private volatile Thread runner;
        private final InfraControl control;

        BenchmarkTask(InfraControl control) {
            this.control = control;
        }

        @Override
        public BenchmarkTaskResult call() throws Exception {
            try {
                // bind the executor thread
                runner = Thread.currentThread();

                // go for the run
                ThreadData td = threadData.get();
                return (BenchmarkTaskResult) method.invoke(td.instance, control, td.params);
            } catch (Throwable e) {
                // about to fail the iteration;

                // notify other threads we have failed
                control.isFailing = true;

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

    /**
     * Handles thread-local data for each worker that should not change
     * between the iterations.
     */
    private static class ThreadData {
        /**
         * Synthetic benchmark instance, which holds the benchmark metadata.
         * Expected to be touched by a single thread only.
         */
        final Object instance;

        /**
         * Thread parameters. Among other things, holds the thread's place
         * in group distribution, and thus should be the same for a given thread.
         */
        final ThreadParams params;

        public ThreadData(Object instance, ThreadParams params) {
            this.instance = instance;
            this.params = params;
        }
    }

}
