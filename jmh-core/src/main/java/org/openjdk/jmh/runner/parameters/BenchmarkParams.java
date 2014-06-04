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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.ActionMode;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Utils;

import java.io.Serializable;
import java.util.Arrays;

public class BenchmarkParams implements Serializable {

    private final boolean synchIterations;
    private final int threads;
    private final int[] threadGroups;
    private final int forks;
    private final int warmupForks;
    private final IterationParams warmup;
    private final IterationParams measurement;

    public BenchmarkParams(boolean synchIterations, int threads, int[] threadGroups, int forks, int warmupForks,
                           int warmupIters, TimeValue warmupTime, int warmupBatchSize,
                           int measureIters, TimeValue measureTime, int measureBatchSize) {
        this.synchIterations = synchIterations;
        this.threads = threads;
        this.threadGroups = threadGroups;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.warmup = new IterationParams(warmupIters, warmupTime, warmupBatchSize);
        this.measurement = new IterationParams(measureIters, measureTime, measureBatchSize);
    }

    public BenchmarkParams(OutputFormat out, Options options, BenchmarkRecord benchmark, ActionMode mode) {
        this.threadGroups = options.getThreadGroups().orElse(benchmark.getThreadGroups());

        int threads = options.getThreads().orElse(
                benchmark.getThreads().orElse(
                        Defaults.THREADS));

        if (threads == Threads.MAX) {
            threads = Utils.figureOutHotCPUs(out);
        }

        this.threads = Utils.roundUp(threads, Utils.sum(threadGroups));

        this.synchIterations = options.shouldSyncIterations().orElse(
                Defaults.SYNC_ITERATIONS);

        this.measurement = mode.doMeasurement() ?
                getMeasurement(options, benchmark) :
                new IterationParams(0, TimeValue.NONE, 1);

        this.warmup = mode.doWarmup() ?
                getWarmup(options, benchmark) :
                new IterationParams(0, TimeValue.NONE, 1);

        this.forks = options.getForkCount().orElse(
                benchmark.getForks().orElse(
                        Defaults.MEASUREMENT_FORKS));

        this.warmupForks = options.getWarmupForkCount().orElse(
                benchmark.getWarmupForks().orElse(
                        Defaults.WARMUP_FORKS));
    }

    private IterationParams getWarmup(Options options, BenchmarkRecord benchmark) {
        return new IterationParams(
                options.getWarmupIterations().orElse(
                        benchmark.getWarmupIterations().orElse(
                            (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.WARMUP_ITERATIONS_SINGLESHOT : Defaults.WARMUP_ITERATIONS
                )),
                options.getWarmupTime().orElse(
                        benchmark.getWarmupTime().orElse(
                            (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.WARMUP_TIME
                )),
                (benchmark.getMode() != Mode.SingleShotTime) ? 1 :
                        options.getWarmupBatchSize().orElse(
                                benchmark.getWarmupBatchSize().orElse(
                                        Defaults.WARMUP_BATCHSIZE
                                )
                        )
        );
    }

    private IterationParams getMeasurement(Options options, BenchmarkRecord benchmark) {
        return new IterationParams(
                options.getMeasurementIterations().orElse(
                        benchmark.getMeasurementIterations().orElse(
                                (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT : Defaults.MEASUREMENT_ITERATIONS
                )),
                options.getMeasurementTime().orElse(
                        benchmark.getMeasurementTime().orElse(
                                (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.MEASUREMENT_TIME
                )),
                (benchmark.getMode() != Mode.SingleShotTime) ? 1 :
                        options.getMeasurementBatchSize().orElse(
                                benchmark.getMeasurementBatchSize().orElse(
                                        Defaults.MEASUREMENT_BATCHSIZE
                                )
                        )
        );
    }

    public boolean shouldSynchIterations() {
        return synchIterations;
    }

    public IterationParams getWarmup() {
        return warmup;
    }

    public IterationParams getMeasurement() {
        return measurement;
    }

    public int getThreads() {
        return threads;
    }

    public int[] getThreadGroups() {
        return Arrays.copyOf(threadGroups, threadGroups.length);
    }

    public int getForks() {
        return forks;
    }

    public int getWarmupForks() {
        return warmupForks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkParams that = (BenchmarkParams) o;

        if (forks != that.forks) return false;
        if (synchIterations != that.synchIterations) return false;
        if (threads != that.threads) return false;
        if (warmupForks != that.warmupForks) return false;
        if (!measurement.equals(that.measurement)) return false;
        if (!Arrays.equals(threadGroups, that.threadGroups)) return false;
        if (!warmup.equals(that.warmup)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (synchIterations ? 1 : 0);
        result = 31 * result + threads;
        result = 31 * result + Arrays.hashCode(threadGroups);
        result = 31 * result + forks;
        result = 31 * result + warmupForks;
        result = 31 * result + warmup.hashCode();
        result = 31 * result + measurement.hashCode();
        return result;
    }

}
