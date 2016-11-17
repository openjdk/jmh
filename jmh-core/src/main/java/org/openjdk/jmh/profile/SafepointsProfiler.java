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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafepointsProfiler implements ExternalProfiler {

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
        return Collections.singletonList("-Xlog:safepoint=info");
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        long skip = br.getMetadata().getMeasurementTime() - br.getMetadata().getStartTime();

        SampleBuffer pauseBuff = new SampleBuffer();
        SampleBuffer ttspBuff = new SampleBuffer();

        try (BufferedReader reader =
                     Files.newBufferedReader(stdOut.toPath(), Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                ParsedData data = parse(line);
                if (data != null) {
                    if (data.timestamp < skip) continue;
                    pauseBuff.add(data.stopTime);
                    ttspBuff.add(data.ttspTime);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return Arrays.asList(
                new SafepointProfilerResult("pause", pauseBuff),
                new SafepointProfilerResult("ttsp", ttspBuff)
        );
    }

    static long parseNs(String str) {
        return (long) (Double.parseDouble(str) * TimeUnit.SECONDS.toNanos(1));
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

    /**
     * Parse the line into the triplet. This is tested with unit tests, make sure to
     * update those if changing this code.
     */
    static ParsedData parse(String line) {
        Pattern p = Pattern.compile("\\[(.*?)s\\]\\[info\\]\\[safepoint\\] (.*) stopped: (.*) seconds, (.*) took: (.*) seconds");
        if (line.contains("[info][safepoint]")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                return new ParsedData(
                        parseNs(m.group(1)),
                        parseNs(m.group(3)),
                        parseNs(m.group(5))
                );
            }
        }

        return null;
    }

    static class ParsedData {
        long timestamp;
        long stopTime;
        long ttspTime;

        public ParsedData(long timestamp, long stopTime, long ttspTime) {
            this.timestamp = timestamp;
            this.stopTime = stopTime;
            this.ttspTime = ttspTime;
        }
    }

}
