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
package org.openjdk.jmh.runner.parameters;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.MicroBenchmark;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.BaseOptions;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class MicroBenchmarkParametersFactory {

    private MicroBenchmarkParametersFactory() {
    }

    public static MicroBenchmarkParameters makeParams(BaseOptions options, BenchmarkRecord benchmark, Method method) {
        boolean shouldSynchIterations = getBoolean(options.getSynchIterations(), Defaults.SHOULD_SYNCH_ITERATIONS);
        IterationParams measurement = getMeasurement(options, benchmark, method);
        IterationParams warmup = getWarmup(options, benchmark, method);

        List<Integer> threadCount = options.getThreadCounts();
        if (!threadCount.isEmpty()) {
            // -tc was set => -t & -sc are not working
            ListIterator<Integer> it = threadCount.listIterator();
            while (it.hasNext()) {
                int listValue = it.next();
                if (listValue == 0) {
                    it.set(Runtime.getRuntime().availableProcessors());
                } else if (listValue < 0) {
                    throw new IllegalArgumentException("-tc values shouldn't be negative");
                }
            }
            return new ThreadCountParameters(
                    shouldSynchIterations,
                    warmup, measurement,
                    threadCount);
        }
        int threads = getThreads(options, method);
        if (options.shouldScale()) {
            if(threads == 0 || threads == 1) { // that was written before me (c) SK
                threads = Runtime.getRuntime().availableProcessors();
            }
            return new ScaleParameters(
                    shouldSynchIterations,
                    warmup, measurement,
                    threads);
        } else {
            if(threads == 0) {
                threads = Runtime.getRuntime().availableProcessors();
            }
            return new SameThreadParameters(
                    shouldSynchIterations,
                    warmup, measurement,
                    threads);
        }
    }

    private static IterationParams getWarmup(BaseOptions options, BenchmarkRecord benchmark, Method method) {
        boolean isSingleShot = (benchmark.getMode() == Mode.SingleShotTime);
        Warmup warAnn = method.getAnnotation(Warmup.class);
        int iters = (warAnn == null) ? -1 : warAnn.iterations();
        if (isSingleShot) {
            return new IterationParams(
                    getInteger(options.getWarmupIterations(), iters, Defaults.SINGLE_SHOT_WARMUP_COUNT),
                    TimeValue.NONE);
        } else {
            TimeValue timeValue = options.getWarmupTime();
            if (timeValue == null || timeValue.getTime() == -1) {
                if (warAnn != null) {
                    timeValue = new TimeValue(warAnn.time(), warAnn.timeUnit());
                } else {
                    timeValue = Defaults.WARMUP_TIME;
                }
            }
            return new IterationParams(getInteger(options.getWarmupIterations(), iters, Defaults.WARMUP_COUNT),timeValue);
        }
    }

    private static IterationParams getMeasurement(BaseOptions options, BenchmarkRecord benchmark, Method method) {
        boolean isSingleShot = (benchmark.getMode() == Mode.SingleShotTime);
        Measurement meAnn = method.getAnnotation(Measurement.class);
        int iters = (meAnn == null) ? -1 : meAnn.iterations();
        if (isSingleShot) {
            return new IterationParams(
                    getInteger(options.getIterations(), iters, Defaults.SINGLE_SHOT_ITERATION_COUNT),
                    TimeValue.NONE);

        } else {
            TimeValue timeValue = options.getRuntime();
            if (timeValue == null || timeValue.getTime() == -1) {
                if (meAnn != null) {
                    timeValue = new TimeValue(meAnn.time(), meAnn.timeUnit());
                } else {
                    timeValue = Defaults.ITERATION_TIME;
                }
            }
            return new IterationParams(
                    getInteger(options.getIterations(), iters, Defaults.ITERATION_COUNT), timeValue);
        }
    }

    private static int getThreads(BaseOptions options, Method method) {
        Threads threadsAnn = method.getAnnotation(Threads.class);
        return getInteger(options.getThreads(), (threadsAnn == null) ? -1 : threadsAnn.value(), Defaults.THREADS);

    }

    static abstract class AbstractParameters implements MicroBenchmarkParameters, Serializable {

        private final boolean synchIterations;

        private final IterationParams warmup;
        private final IterationParams iteration;

        public AbstractParameters(boolean synchIterations, IterationParams warmup, IterationParams iteration) {
            this.synchIterations = synchIterations;
            this.warmup = warmup;
            this.iteration = iteration;
        }

        @Override
        public boolean shouldSynchIterations() {
            return synchIterations;
        }

        @Override
        public IterationParams getWarmup() {
            return warmup;
        }

        @Override
        public IterationParams getIteration() {
            return iteration;
        }
    }

    static class ThreadCountParameters extends AbstractParameters  {

        private final int maxThreads;
        private final List<Integer> threadCount;
        private List<ThreadIterationParams> threadSequence = null;

        public ThreadCountParameters(boolean synchIterations, IterationParams warmup, IterationParams iteration, List<Integer> threadCount) {
            super(synchIterations, warmup, new IterationParams(threadCount.size(), iteration.getTime()));
            this.threadCount = threadCount;
            this.maxThreads = Collections.max(threadCount);
        }

        @Override
        public List<Integer> getThreadCounts() {
            return threadCount;
        }

        @Override
        public boolean shouldScale() {
            return false;
        }

        @Override
        public int getMaxThreads() {
            return maxThreads;
        }

        @Override
        public List<ThreadIterationParams> getThreadIterationSequence() {
            if (threadSequence == null) {
                threadSequence = compressRawList(getThreadCounts(), getIteration().getTime());
            }
            return threadSequence;
        }

        @Override
        public MicroBenchmarkParameters warmupToIteration() {
            return new ThreadCountParameters(
                    this.shouldSynchIterations(),
                    this.getWarmup(),
                    this.getWarmup(),
                    this.getThreadCounts());
        }
    }

    static class ScaleParameters extends AbstractParameters {

        private final int threads;

        // lazy set in getThreadCount - not thread safe.
        private List<Integer> threadCount = null;
        private List<ThreadIterationParams> threadSequence = null;

        public ScaleParameters(boolean synchIterations, IterationParams warmup, IterationParams iteration, int threads) {
            super(synchIterations, warmup, iteration);
            this.threads = threads;
        }

        @Override
        public List<Integer> getThreadCounts() {
            if (threadCount == null) {
                threadCount = makeScaledThreadSequence(threads, getIteration().getCount());
            }
            return threadCount;
        }

        @Override
        public boolean shouldScale() {
            return true;
        }

        @Override
        public int getMaxThreads() {
            return threads;
        }

        @Override
        public List<ThreadIterationParams> getThreadIterationSequence() {
            if (threadSequence == null) {
                threadSequence = compressRawList(getThreadCounts(), getIteration().getTime());
            }
            return threadSequence ;
        }

        @Override
        public MicroBenchmarkParameters warmupToIteration() {
            return new ScaleParameters(
                    this.shouldSynchIterations(),
                    this.getWarmup(),
                    this.getWarmup(),
                    this.getMaxThreads());
        }

        /**
         * Returns a list of "threadcounts", one count per iteration. Where each count is the number of concurrent threads
         * that should execute the workload for the given iteration. Position 'i' in list == iteration i.
         * <p/>
         * Thanks to aleksey.shipilev@oracle.com for this idea!
         *
         * @return a list of threadcounts
         */
        private static List<Integer> makeScaledThreadSequence(int threads, int iterations) {
            assert threads > 0;
            assert iterations >= 0;
            if (iterations == 0) {
                return Collections.<Integer>emptyList();
            }
            if (iterations == 1) {
                return Collections.<Integer>singletonList(threads);
            }
            List<Integer> result = new ArrayList<Integer>(iterations);
            if (iterations > threads) {
                int step = Math.round((float) iterations / (float) threads);
                int t = 1;

                for (int i = 0; i < iterations - 1; i++) {
                    result.add(t);
                    // increase threadcount every step'd iteration
                    // example: 12 iterations, 4 threads:
                    // 1 1 1 1 2 2 2 2 3 3 3 3 4 4 4 4
                    if ((i + 1) % step == 0) {
                        t++;
                    }
                }
            } else {
                int step;
                int t;
                // handle special case
                if (threads == iterations) {
                    step = 1;
                    t = 2;
                } else {
                    // general
                    step = Math.round((float) threads / ((float) iterations - 1));
                    t = step;
                }
                // first round always == 1 in this case
                result.add(1);
                for (int i = 1; i < iterations - 1; i++) {
                    result.add(t);
                    // just plain interpolate
                    // example: 5 iterations, 64 threads:
                    // 1 16 32 48 64
                    t += step;
                }
            }
            // add last iteration, always == getThreads()
            result.add(threads);
            return result;
        }
    }

    static class SameThreadParameters extends AbstractParameters {

        private final int threads;

        public SameThreadParameters(boolean synchIterations, IterationParams warmup, IterationParams iteration, int threads) {
            super(synchIterations, warmup, iteration);
            this.threads = threads;
        }

        @Override
        public List<Integer> getThreadCounts() {
            return Collections.<Integer>nCopies(getIteration().getCount(), threads);
        }

        @Override
        public boolean shouldScale() {
            return false;
        }

        @Override
        public int getMaxThreads() {
            return threads;
        }

        @Override
        public List<ThreadIterationParams> getThreadIterationSequence() {
            return Collections.<ThreadIterationParams>singletonList(getIteration().addThreads(threads));
        }

        @Override
        public MicroBenchmarkParameters warmupToIteration() {
            return new SameThreadParameters(
                    this.shouldSynchIterations(),
                    this.getWarmup(),
                    this.getWarmup(),
                    this.getMaxThreads());
        }
    }


    private static List<ThreadIterationParams> compressRawList(List<Integer> rawThreadCount, TimeValue timeValue) {
        if (rawThreadCount == null || rawThreadCount.isEmpty()) {
            return Collections.<ThreadIterationParams>emptyList();
        }
        List<ThreadIterationParams> res = new ArrayList<ThreadIterationParams>();
        int prevThreads = -1;
        int iterCount = 0;
        for (int threads : rawThreadCount) {
            assert threads > 0 : "thread count should be positive ";
            if (threads == prevThreads) {
                iterCount++;
            } else {
                if (prevThreads > 0) {
                    res.add(new ThreadIterationParams(prevThreads, iterCount, timeValue));
                }
                prevThreads = threads;
                iterCount = 1;
            }
        }
        if (prevThreads > 0) {
            res.add(new ThreadIterationParams(prevThreads, iterCount, timeValue));
        }
        return res;
    }

    private static boolean getBoolean(Boolean value, boolean defaultValue) {
        return value==null ? defaultValue : value;
    }

    private static int getInteger(int first, int second, int third) {
        return first >= 0 ? first : (second >= 0 ? second : third);
    }

}
