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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.SampleBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafepointsProfiler implements ExternalProfiler {

    private static final long NO_LONG_VALUE = Long.MIN_VALUE;

    @Override
    public String getDescription() {
        return "Safepoints profiler";
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Arrays.asList(
                // make sure old JVMs don't barf on Unified Logging
                "-XX:+IgnoreUnrecognizedVMOptions",

                // JDK 9+: preferred, Unified Logging
                "-Xlog:safepoint=info",

                // pre JDK-9: special options
                "-XX:+PrintGCApplicationStoppedTime", "-XX:+PrintGCTimeStamps"
        );
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        long measuredTimeMs = ProfilerUtils.measuredTimeMs(br);
        long measuredTimeNs = TimeUnit.MILLISECONDS.toNanos(measuredTimeMs);

        long measureFrom = TimeUnit.MILLISECONDS.toNanos(ProfilerUtils.warmupDelayMs(br));
        long measureTo = measureFrom + measuredTimeNs;

        List<ParsedData> ds = new ArrayList<>();

        try (BufferedReader reader =
                     Files.newBufferedReader(stdOut.toPath(), Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                ParsedData data = parse(line);
                if (data != null) {
                    ds.add(data);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // Only accept the lines from the highest version.
        long maxVer = Long.MIN_VALUE;
        for (ParsedData d : ds) {
            maxVer = Math.max(maxVer, d.ver);
        }

        SampleBuffer pauseBuff = new SampleBuffer();
        SampleBuffer ttspBuff = new SampleBuffer();

        for (ParsedData d : ds) {
            if (d.ver == maxVer &&
                    (d.timestamp > measureFrom) && (d.timestamp < measureTo)) {
                pauseBuff.add(d.stopTime);
                if (d.ttspTime != NO_LONG_VALUE) {
                    ttspBuff.add(d.ttspTime);
                }
            }
        }

        Collection<Result> results = new ArrayList<>();

        results.add(new ScalarResult(Defaults.PREFIX + "safepoints.interval",
                measuredTimeMs, "ms", AggregationPolicy.SUM));

        results.add(new SafepointProfilerResult("pause", pauseBuff));

        // JDK 7 does not have TTSP measurements, ignore the zero metric:
        if (maxVer > 7) {
            results.add(new SafepointProfilerResult("ttsp", ttspBuff));
        }
        return results;
    }

    static long parseNs(String str) {
        return (long) (Double.parseDouble(str.replace(',', '.')) * TimeUnit.SECONDS.toNanos(1));
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return true;
    }

    static class SafepointProfilerResult extends Result<SafepointProfilerResult> {
        private final String suffix;
        private final SampleBuffer buffer;

        public SafepointProfilerResult(String suffix, SampleBuffer buffer) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "safepoints." + suffix, buffer.getStatistics(1D / 1000000), "ms", AggregationPolicy.SUM);
            this.suffix = suffix;
            this.buffer = buffer;
        }

        @Override
        protected Aggregator<SafepointProfilerResult> getThreadAggregator() {
            return new JoiningAggregator();
        }

        @Override
        protected Aggregator<SafepointProfilerResult> getIterationAggregator() {
            return new JoiningAggregator();
        }

        @Override
        protected Collection<? extends Result> getDerivativeResults() {
            return Arrays.asList(
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".avg",      statistics.getMean(),           "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".count",    statistics.getN(),              "#",  AggregationPolicy.SUM),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.00",    statistics.getMin(),            "ms", AggregationPolicy.MIN),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.50",    statistics.getPercentile(50),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.90",    statistics.getPercentile(90),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.95",    statistics.getPercentile(95),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.99",    statistics.getPercentile(99),   "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.999",   statistics.getPercentile(99.9), "ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p0.9999",  statistics.getPercentile(99.99),"ms", AggregationPolicy.AVG),
                new ScalarDerivativeResult(Defaults.PREFIX + "safepoints." + suffix + ".p1.00",    statistics.getMax(),            "ms", AggregationPolicy.MAX)
            );
        }

        /**
         * Always add up all the samples into final result.
         * This will allow aggregate result to achieve better accuracy.
         */
        private static class JoiningAggregator implements Aggregator<SafepointProfilerResult> {

            @Override
            public SafepointProfilerResult aggregate(Collection<SafepointProfilerResult> results) {
                SampleBuffer buffer = new SampleBuffer();
                String suffix = null;
                for (SafepointProfilerResult r : results) {
                    buffer.addAll(r.buffer);
                    if (suffix == null) {
                        suffix = r.suffix;
                    } else if (!suffix.equals(r.suffix)) {
                        throw new IllegalStateException("Trying to aggregate results with different suffixes");
                    }
                }
                return new SafepointProfilerResult(suffix, buffer);
            }
        }
    }

    private static final Pattern JDK_7_LINE =
            Pattern.compile("([0-9\\.,]*): (.*) stopped: ([0-9\\.,]*) seconds");

    private static final Pattern JDK_8_LINE =
            Pattern.compile("([0-9\\.,]*): (.*) stopped: ([0-9\\.,]*) seconds, (.*) took: ([0-9\\.,]*) seconds");

    private static final Pattern JDK_9_LINE =
            Pattern.compile("\\[([0-9\\.,]*)s\\]\\[info\\]\\[safepoint\\] (.*) stopped: ([0-9\\.,]*) seconds, (.*) took: ([0-9\\.,]*) seconds");

    /**
     * Parse the line into the triplet. This is tested with unit tests, make sure to
     * update those if changing this code.
     */
    static ParsedData parse(String line) {
        {
            Matcher m = JDK_7_LINE.matcher(line);
            if (m.matches()) {
                return new ParsedData(
                        7,
                        parseNs(m.group(1)),
                        parseNs(m.group(3)),
                        NO_LONG_VALUE
                );
            }
        }

        {
            Matcher m = JDK_8_LINE.matcher(line);
            if (m.matches()) {
                return new ParsedData(
                        8,
                        parseNs(m.group(1)),
                        parseNs(m.group(3)),
                        parseNs(m.group(5))
                );
            }
        }

        {
            Matcher m = JDK_9_LINE.matcher(line);
            if (m.matches()) {
                return new ParsedData(
                        9,
                        parseNs(m.group(1)),
                        parseNs(m.group(3)),
                        parseNs(m.group(5))
                );
            }
        }

        return null;
    }

    static class ParsedData {
        int ver;
        long timestamp;
        long stopTime;
        long ttspTime;

        public ParsedData(int ver, long timestamp, long stopTime, long ttspTime) {
            this.ver = ver;
            this.timestamp = timestamp;
            this.stopTime = stopTime;
            this.ttspTime = ttspTime;
        }
    }

}
