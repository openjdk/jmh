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

import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.BaseOptions;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MicroBenchmarkParametersFactory {

    private MicroBenchmarkParametersFactory() {
    }

    public static MicroBenchmarkParameters makeParams(BaseOptions options, BenchmarkRecord benchmark, Method method) {
        boolean shouldSynchIterations = getBoolean(options.getSynchIterations(), Defaults.SHOULD_SYNCH_ITERATIONS);
        IterationParams measurement = getMeasurement(options, benchmark, method);
        IterationParams warmup = getWarmup(options, benchmark, method);

        int threads = getThreads(options, method);
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        return new SameThreadParameters(
                shouldSynchIterations,
                warmup, measurement,
                threads);
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

    static class SameThreadParameters extends AbstractParameters {

        private final int threads;

        public SameThreadParameters(boolean synchIterations, IterationParams warmup, IterationParams iteration, int threads) {
            super(synchIterations, warmup, iteration);
            this.threads = threads;
        }

        @Override
        public int getThreads() {
            return threads;
        }

        @Override
        public MicroBenchmarkParameters warmupToIteration() {
            return new SameThreadParameters(
                    this.shouldSynchIterations(),
                    this.getWarmup(),
                    this.getWarmup(),
                    this.getThreads());
        }
    }

    private static boolean getBoolean(Boolean value, boolean defaultValue) {
        return value==null ? defaultValue : value;
    }

    private static int getInteger(int first, int second, int third) {
        return first >= 0 ? first : (second >= 0 ? second : third);
    }

}
