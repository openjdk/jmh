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

import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.Defaults;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * Base class for all microbenchmarks handlers.
 */
public abstract class BaseMicroBenchmarkHandler implements MicroBenchmarkHandler{

    /**
     * Name of micro benchmark
     */
    protected final BenchmarkRecord microbenchmark;

    /**
     * Thread-pool for threads executing the benchmark tasks
     */
    protected final ExecutorService executor;

    // (Aleksey) Forgive me, Father, for I have sinned.
    protected final ThreadLocal<Object> instances;

    protected final OutputFormat format;
    protected final TimeUnit timeUnit;
    protected final Long opsPerInvocation;

    private final List<InternalProfiler> registeredProfilers;

    public BaseMicroBenchmarkHandler(OutputFormat format, BenchmarkRecord microbenchmark, final Class<?> clazz, Options options, BenchmarkParams executionParams) {
        this.microbenchmark = microbenchmark;
        this.registeredProfilers = createProfilers(options);
        this.instances = new ThreadLocal<Object>() {
            @Override
            protected Object initialValue() {
                try {
                    return clazz.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Class " + clazz.getName() + " instantiation error ", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Class " + clazz.getName() + " instantiation error ", e);
                }
            }
        };
        this.format = format;
        this.timeUnit = options.getTimeUnit().orElse(null);
        this.opsPerInvocation = options.getOperationsPerInvocation().orElse(null);
        try {
            this.executor = EXECUTOR_TYPE.createExecutor(executionParams.getThreads(), microbenchmark.getUsername());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<InternalProfiler> createProfilers(Options options) {
        List<InternalProfiler> list = new ArrayList<InternalProfiler>();
        // register the profilers
        for (Class<? extends Profiler> prof : options.getProfilers()) {
            if (!ProfilerFactory.isInternal(prof)) continue;
            list.add((InternalProfiler) ProfilerFactory.prepareProfiler(prof, options.verbosity().orElse(Defaults.VERBOSITY)));
        }
        return list;
    }

    private static final ExecutorType EXECUTOR_TYPE = Enum.valueOf(ExecutorType.class, System.getProperty("harness.executor", ExecutorType.FIXED_TPE.name()));

    private enum ExecutorType {

        /**
         * Use Executors.newCachedThreadPool
         */
        CACHED_TPE {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) {
                return Executors.newCachedThreadPool(new HarnessThreadFactory(prefix));
            }
        },

        /**
         * Use Executors.newFixedThreadPool
         */
        FIXED_TPE {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) {
                return Executors.newFixedThreadPool(maxThreads, new HarnessThreadFactory(prefix));
            }
        },

        /**
         * Use new ForkJoinPool (JDK 7+)
         */
        FJP {
            @Override
            ExecutorService createExecutor(int maxThreads, String prefix) throws Exception {
                // (Aleksey):
                // requires some of the reflection magic to untie from JDK 7 compile-time dependencies
                Constructor<?> c = Class.forName("java.util.concurrent.ForkJoinPool").getConstructor(int.class);
                return (ExecutorService) c.newInstance(maxThreads);
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
                String className = System.getProperty("harness.executor.class");
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

    /**
     * Forces ExecutorService to terminate.
     * This method returns only if requested executor had shut down.
     * If executor is failing to shut down regardless of multiple shutdown()s, this method will never return.
     *
     * @param executor service to shutdown
     */
    static void shutdownExecutor(ExecutorService executor) {
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

            Logger.getLogger(BaseMicroBenchmarkHandler.class.getName())
                    .warning("Failed to stop executor service " + executor + ", trying again; check for the unaccounted running threads");
        }
    }

    protected void stopProfilers(IterationResult iterationResults) {
        // stop profilers
        for (InternalProfiler prof : registeredProfilers) {
            try {
                iterationResults.addResults(prof.afterIteration());
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    protected void startProfilers() {
        // start profilers
        for (InternalProfiler prof : registeredProfilers) {
            try {
                prof.beforeIteration();
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    @Override
    public BenchmarkRecord getBenchmark() {
        return microbenchmark;
    }

    /**
     * Status of the ExecutorPool
     *
     * @return true if it is shut down, else false
     */
    boolean isExecutorShutdown() {
        return executor.isShutdown();
    }

    @Override
    public void shutdown() {
        shutdownExecutor(executor);
    }

}
