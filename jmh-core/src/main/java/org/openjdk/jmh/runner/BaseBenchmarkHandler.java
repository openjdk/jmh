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
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Base class for all benchmarks handlers.
 */
abstract class BaseBenchmarkHandler implements BenchmarkHandler {

    /**
     * Thread-pool for threads executing the benchmark tasks
     */
    protected final ExecutorService executor;

    // (Aleksey) Forgive me, Father, for I have sinned.
    protected final ThreadLocal<Object> instances;

    protected final OutputFormat out;

    private final List<InternalProfiler> registeredProfilers;

    public BaseBenchmarkHandler(OutputFormat out, final Class<?> clazz, Options options, BenchmarkParams executionParams) {
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
        this.out = out;
        try {
            this.executor = EXECUTOR_TYPE.createExecutor(executionParams.getThreads(), executionParams.getBenchmark());
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

    static ThreadParams[] distributeThreads(int threads, int[] groups) {
        ThreadParams[] result = new ThreadParams[threads];
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

            result[t] = new ThreadParams(
                    t, threads,
                    currentGroup, totalGroups,
                    currentSubgroup, totalSubgroups,
                    currentGroupThread, totalGroupThreads,
                    currentSubgroupThread, groups[currentSubgroup]
                  );

            currentGroupThread++;
            currentSubgroupThread++;
        }
        return result;
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

    protected void stopProfilers(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iterationResults) {
        // stop profilers
        for (InternalProfiler prof : registeredProfilers) {
            try {
                iterationResults.addResults(prof.afterIteration(benchmarkParams, iterationParams));
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    protected void startProfilers(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        // start profilers
        for (InternalProfiler prof : registeredProfilers) {
            try {
                prof.beforeIteration(benchmarkParams, iterationParams);
            } catch (Throwable ex) {
                throw new BenchmarkException(ex);
            }
        }
    }

    @Override
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

}
