/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Multiset;
import org.openjdk.jmh.util.Statistics;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LinuxPerfNormProfiler implements ExternalProfiler {

    /** Delay collection for given time; -1 to detect automatically */
    private static final int DELAY_MSEC = Integer.getInteger("jmh.perfnorm.delayMs", -1);

    /** Ignore event increments larger that this */
    private static final long HIGH_PASS_FILTER = Long.getLong("jmh.perfnorm.filterHigh", 100000000000L);

    /** The interval between incremental updates from concurrently running perf */
    private static final int INCREMENT_INTERVAL = Integer.getInteger("jmh.perfnorm.intervalMs", 100);

    private static final boolean IS_SUPPORTED;
    private static final boolean IS_INCREMENTABLE;
    private static final Collection<String> FAIL_MSGS;

    static {
        FAIL_MSGS = Utils.tryWith("perf", "stat", "--log-fd", "2", "-x,", "echo", "1");
        IS_SUPPORTED = FAIL_MSGS.isEmpty();

        Collection<String> incremental = Utils.tryWith("perf", "stat", "--log-fd", "2", "-x,", "-I", String.valueOf(INCREMENT_INTERVAL), "echo", "1");
        IS_INCREMENTABLE = incremental.isEmpty();
    }


    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        if (IS_INCREMENTABLE) {
            return Arrays.asList("perf", "stat", "--log-fd", "2", "-d", "-d", "-d", "-x,", "-I", String.valueOf(INCREMENT_INTERVAL));
        } else {
            return Arrays.asList("perf", "stat", "--log-fd", "2", "-d", "-d", "-d", "-x,");
        }
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        return process(br, stdOut, stdErr);
    }

    @Override
    public boolean allowPrintOut() {
        return true;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        if (IS_SUPPORTED) {
            return true;
        } else {
            msgs.addAll(FAIL_MSGS);
            return false;
        }
    }

    @Override
    public String label() {
        return "perfnorm";
    }

    @Override
    public String getDescription() {
        return "Linux perf statistics, normalized by operation count";
    }

    public long getDelay(BenchmarkResult br) {
        if (DELAY_MSEC == -1) { // not set
            BenchmarkResultMetaData md = br.getMetadata();
            if (md != null) {
                // try to ask harness itself:
                return TimeUnit.MILLISECONDS.toNanos(md.getMeasurementTime() - md.getStartTime());
            } else {
                // metadata is not available, let's make a guess:
                IterationParams wp = br.getParams().getWarmup();
                return wp.getCount() * wp.getTime().convertTo(TimeUnit.NANOSECONDS)
                        + TimeUnit.SECONDS.toNanos(1); // loosely account for the JVM lag
            }
        } else {
            return TimeUnit.MILLISECONDS.toNanos(DELAY_MSEC);
        }
    }

    private Collection<? extends Result> process(BenchmarkResult br, File stdOut, File stdErr) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Multiset<String> events = new HashMultiset<String>();

        FileReader fr = null;
        try {
            fr = new FileReader(stdErr);
            BufferedReader reader = new BufferedReader(fr);

            long delayNs = getDelay(br);

            String line;

            nextline:
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;

                if (IS_INCREMENTABLE) {
                    int idx1 = line.indexOf(",");
                    int idx2 = line.lastIndexOf(",");
                    String time  = line.substring(0, idx1).trim();
                    String count = line.substring(idx1, idx2 + 1).trim();
                    String event = line.substring(idx2 + 1).trim();

                    try {
                        double timeSec = NumberFormat.getInstance().parse(time).doubleValue();
                        if (timeSec * TimeUnit.SECONDS.toNanos(1) < delayNs) {
                            // warmup, ignore
                            continue nextline;
                        }
                    } catch (ParseException e) {
                        // don't care then, continue
                        continue nextline;
                    }

                    try {
                        long lValue = NumberFormat.getInstance().parse(count).longValue();
                        if (lValue > HIGH_PASS_FILTER) {
                            // anomalous value, pretend we did not see it
                            continue nextline;
                        }
                        events.add(event, lValue);
                    } catch (ParseException e) {
                        // do nothing, continue
                        continue nextline;

                    }
                } else {
                    int idx = line.lastIndexOf(",");
                    String count = line.substring(0, idx).trim();
                    String event = line.substring(idx + 1).trim();

                    try {
                        long lValue = NumberFormat.getInstance().parse(count).longValue();
                        events.add(event, lValue);
                    } catch (ParseException e) {
                        // do nothing, continue
                        continue nextline;
                    }
                }

            }

            if (!IS_INCREMENTABLE) {
                pw.println();
                pw.println("WARNING: Your system uses old \"perf\", which can not print data incrementally (-I).\n" +
                        "Therefore, perf performance data includes benchmark warmup.");
            }

            pw.flush();
            pw.close();

            long totalOpts;

            BenchmarkResultMetaData md = br.getMetadata();
            if (md != null) {
                if (IS_INCREMENTABLE) {
                    totalOpts = md.getMeasurementOps();
                } else {
                    totalOpts = md.getWarmupOps() + md.getMeasurementOps();
                }
                Collection<Result> results = new ArrayList<Result>();
                for (String key : events.keys()) {
                    results.add(new PerfResult(key, events.count(key) * 1.0 / totalOpts));
                }
                return results;
            } else {
                return Collections.singleton(new PerfResult("N/A", Double.NaN));
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileUtils.safelyClose(fr);
        }
    }

    static class PerfResult extends Result<PerfResult> {
        private static final long serialVersionUID = -1262685915873231436L;

        private final String key;

        public PerfResult(String key, double value) {
            this(key, of(value));
        }

        public PerfResult(String key, Statistics stat) {
            super(ResultRole.SECONDARY, "@" + key, stat, "#/op", AggregationPolicy.AVG);
            this.key = key;
        }

        @Override
        protected Aggregator<PerfResult> getThreadAggregator() {
            return new PerfResultAggregator();
        }

        @Override
        protected Aggregator<PerfResult> getIterationAggregator() {
            return new PerfResultAggregator();
        }

        @Override
        public String toString() {
            return String.format(" %.3f %s/op", getScore(), key);
        }

        @Override
        public String extendedInfo(String label) {
            return "";
        }
    }

    static class PerfResultAggregator implements Aggregator<PerfResult> {

        @Override
        public PerfResult aggregate(Collection<PerfResult> results) {
            String key = "";
            ListStatistics stat = new ListStatistics();
            for (PerfResult r : results) {
                key = r.key;
                stat.addValue(r.getScore());
            }
            return new PerfResult(key, stat);
        }
    }

}
