/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.runner.options.IntegerValueConverter;
import org.openjdk.jmh.util.SampleBuffer;
import org.openjdk.jmh.util.Statistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class PausesProfiler implements InternalProfiler {

    private Ticker ticker;
    private SampleBuffer buffer;
    private long expectedNs;
    private long thresh;

    @Override
    public String getDescription() {
        return "Pauses profiler";
    }

    public PausesProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter(PausesProfiler.class.getCanonicalName()));

        OptionSpec<Integer> optSamplePeriod = parser.accepts("period", "Sampling period, in us. " +
                "Smaller values improve accuracy, at the expense of more profiling overhead.")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int").defaultsTo(50);

        OptionSpec<Integer> optThreshold = parser.accepts("threshold", "Threshold to filter pauses, in us. " +
                "If unset, the threshold is figured during the initial calibration.")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int").defaultsTo(-1);

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            expectedNs = TimeUnit.MICROSECONDS.toNanos(set.valueOf(optSamplePeriod));
            if (set.valueOf(optThreshold) != -1) {
                thresh = TimeUnit.MICROSECONDS.toNanos(set.valueOf(optThreshold));
            } else {
                thresh = calibrate();
            }
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        buffer = new SampleBuffer();
        ticker = new Ticker(buffer);
        ticker.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        ticker.interrupt();
        try {
            ticker.join();
        } catch (InterruptedException e) {
            // do nothing, proceed
        }

        return Collections.singletonList(new PausesProfilerResult(buffer));
    }

    private long calibrate() {
        SampleBuffer buf = new SampleBuffer();

        long lastTime = System.nanoTime();
        for (int c = 0; c < 10000; c++) {
            LockSupport.parkNanos(expectedNs);
            long time = System.nanoTime();

            long actualNs = time - lastTime;
            long delta = actualNs - expectedNs;
            if (delta > 0) {
                buf.add(delta);
            }
            lastTime = time;
        }

        // The max observed pause during calibration must be our measurement
        // threshold. We cannot reliably guess the pauses lower than this are
        // caused by the benchmark pressure.
        Statistics stat = buf.getStatistics(1);
        return (long) stat.getMax();
    }


    private class Ticker extends Thread {
        private final SampleBuffer buffer;
        public Ticker(SampleBuffer buffer) {
            this.buffer = buffer;
            setPriority(Thread.MAX_PRIORITY);
            setDaemon(true);
        }

        @Override
        public void run() {
            long lastTime = System.nanoTime();
            while (!Thread.interrupted()) {
                LockSupport.parkNanos(expectedNs);
                long time = System.nanoTime();

                long actualNs = time - lastTime;
                long delta = actualNs - expectedNs;
                if (delta > thresh) {
                    // assume the actual pause starts within the sleep interval,
                    // we can adjust the measurement by a half the expected time
                    buffer.add(delta + expectedNs/2);
                }
                lastTime = time;
            }
        }
    }

    static class PausesProfilerResult extends Result<PausesProfilerResult> {
        private final SampleBuffer buffer;

        public PausesProfilerResult(SampleBuffer buffer) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "pauses", buffer.getStatistics(1D / 1000000), "ms", AggregationPolicy.SUM);
            this.buffer = buffer;
        }

        @Override
        protected Aggregator<PausesProfilerResult> getThreadAggregator() {
            return new JoiningAggregator();
        }

        @Override
        protected Aggregator<PausesProfilerResult> getIterationAggregator() {
            return new JoiningAggregator();
        }

        @Override
        protected Collection<? extends Result> getDerivativeResults() {
            return Arrays.asList(
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.avg",      statistics.getMean(),           "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.count",    statistics.getN(),              "#",  AggregationPolicy.SUM),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.00",    statistics.getMin(),            "ms", AggregationPolicy.MIN),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.50",    statistics.getPercentile(50),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.90",    statistics.getPercentile(90),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.95",    statistics.getPercentile(95),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.99",    statistics.getPercentile(99),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.999",   statistics.getPercentile(99.9), "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p0.9999",  statistics.getPercentile(99.99),"ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "pauses.p1.00",    statistics.getMax(),            "ms", AggregationPolicy.MAX)
            );
        }

        /**
         * Always add up all the samples into final result.
         * This will allow aggregate result to achieve better accuracy.
         */
        private static class JoiningAggregator implements Aggregator<PausesProfilerResult> {

            @Override
            public PausesProfilerResult aggregate(Collection<PausesProfilerResult> results) {
                SampleBuffer buffer = new SampleBuffer();
                for (PausesProfilerResult r : results) {
                    buffer.addAll(r.buffer);
                }
                return new PausesProfilerResult(buffer);
            }
        }
    }

}
