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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LinuxPerfNormProfiler implements ExternalProfiler {

    /** This is a non-exhaustive list of events we care about. */
    private static final String[] interestingEvents = new String[]{
            "cycles", "instructions",
            "branches", "branch-misses",
            "bus-cycles", "ref-cycles",
            "context-switches", "cpu-migrations",
            "page-faults", "minor-faults", "major-faults", "alignment-faults", "emulation-faults",
            "L1-dcache-loads",  "L1-dcache-load-misses",
            "L1-dcache-stores", "L1-dcache-store-misses",
            "L1-icache-loads", "L1-icache-load-misses",
            "LLC-loads", "LLC-stores",
            "dTLB-loads",  "dTLB-load-misses",
            "dTLB-stores", "dTLB-store-misses",
            "iTLB-loads",  "iTLB-load-misses",
            "stalled-cycles-frontend", "stalled-cycles-backend",
    };

    private final int delayMs;
    private final boolean useDefaultStats;
    private final long highPassFilter;
    private final int incrementInterval;
    private final boolean isIncrementable;

    private final Collection<String> supportedEvents = new ArrayList<String>();

    public LinuxPerfNormProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("perfnorm"));

        OptionSpec<String> optEvents = parser.accepts("events",
                        "Events to gather.")
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(",").describedAs("event+").defaultsTo(interestingEvents);

        OptionSpec<Integer> optDelay = parser.accepts("delay",
                        "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<Integer> optIncrementInterval = parser.accepts("interval",
                        "The interval between incremental updates from a concurrently running perf. " +
                        "Lower values may improve accuracy, while increasing the profiling overhead.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(100);

        OptionSpec<Long> optHighPassFilter = parser.accepts("highPassFilter",
                        "Ignore event increments larger that this.")
                .withRequiredArg().ofType(Long.class).describedAs("#").defaultsTo(100000000000L);

        OptionSpec<Boolean> optDefaultStat = parser.accepts("useDefaultStat",
                        "Use \"perf stat -d -d -d\" instead of explicit counter list.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        Collection<String> userEvents;

        try {
            delayMs = set.valueOf(optDelay);
            incrementInterval = set.valueOf(optIncrementInterval);
            highPassFilter = set.valueOf(optHighPassFilter);
            useDefaultStats = set.valueOf(optDefaultStat);
            userEvents = set.valuesOf(optEvents);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }

        Collection<String> msgs = Utils.tryWith("perf", "stat", "--log-fd", "2", "--field-separator", ",", "echo", "1");
        if (!msgs.isEmpty()) {
            throw new ProfilerException(msgs.toString());
        }

        Collection<String> incremental = Utils.tryWith("perf", "stat", "--log-fd", "2", "--field-separator", ",", "--interval-print", String.valueOf(incrementInterval), "echo", "1");
        isIncrementable = incremental.isEmpty();

        if (userEvents != null) {
            for (String ev : userEvents) {
                if (ev.trim().isEmpty()) continue;
                supportedEvents.add(ev);
            }
        }

        if (supportedEvents.isEmpty()) {
            for (String ev : interestingEvents) {
                Collection<String> res = Utils.tryWith("perf", "stat", "--log-fd", "2", "--field-separator", ",", "--event", "cycles,instructions," + ev, "echo", "1");
                if (res.isEmpty()) {
                    supportedEvents.add(ev);
                }
            }
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        List<String> cmd = new ArrayList<String>();
        if (useDefaultStats) {
            cmd.addAll(Arrays.asList("perf", "stat", "--log-fd", "2", "--field-separator", ",", "--detailed", "--detailed", "--detailed"));
        } else {
            cmd.addAll(Arrays.asList("perf", "stat", "--log-fd", "2", "--field-separator", ",", "--event", Utils.join(supportedEvents, ",")));
        }
        if (isIncrementable) {
            cmd.addAll(Arrays.asList("-I", String.valueOf(incrementInterval)));
        }
        return cmd;
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
    public String getDescription() {
        return "Linux perf statistics, normalized by operation count";
    }

    public long getDelay(BenchmarkResult br) {
        if (delayMs == -1) { // not set
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
            return TimeUnit.MILLISECONDS.toNanos(delayMs);
        }
    }

    private Collection<? extends Result> process(BenchmarkResult br, File stdOut, File stdErr) {
        Multiset<String> events = new HashMultiset<String>();

        FileReader fr = null;
        try {
            fr = new FileReader(stdErr);
            BufferedReader reader = new BufferedReader(fr);

            long delayNs = getDelay(br);

            NumberFormat nf = NumberFormat.getInstance();

            String line;

            nextline:
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;

                if (isIncrementable) {
                    String[] split = line.split(",");

                    String time;
                    String count;
                    String event;

                    if (split.length == 3) {
                        // perf 3.13: time,count,event
                        time  = split[0].trim();
                        count = split[1].trim();
                        event = split[2].trim();
                    } else if (split.length >= 4) {
                        // perf >3.13: time,count,<other>,event,<others>
                        time  = split[0].trim();
                        count = split[1].trim();
                        event = split[3].trim();
                    } else {
                        // Malformed line, ignore
                        continue nextline;
                    }

                    try {
                        double timeSec = nf.parse(time).doubleValue();
                        if (timeSec * TimeUnit.SECONDS.toNanos(1) < delayNs) {
                            // warmup, ignore
                            continue nextline;
                        }
                    } catch (ParseException e) {
                        // don't care then, continue
                        continue nextline;
                    }

                    try {
                        long lValue = nf.parse(count).longValue();
                        if (lValue > highPassFilter) {
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

                    // Malformed line, ignore
                    if (idx == -1) continue nextline;

                    String count = line.substring(0, idx).trim();
                    String event = line.substring(idx + 1).trim();

                    try {
                        long lValue = nf.parse(count).longValue();
                        events.add(event, lValue);
                    } catch (ParseException e) {
                        // do nothing, continue
                        continue nextline;
                    }
                }

            }

            if (!isIncrementable) {
                System.out.println();
                System.out.println();
                System.out.println("WARNING: Your system uses old \"perf\", which cannot print data incrementally (-I).\n" +
                        "Therefore, perf performance data includes benchmark warmup.");
            }

            long totalOpts;

            BenchmarkResultMetaData md = br.getMetadata();
            if (md != null) {
                if (isIncrementable) {
                    totalOpts = md.getMeasurementOps();
                } else {
                    totalOpts = md.getWarmupOps() + md.getMeasurementOps();
                }
                Collection<Result> results = new ArrayList<Result>();
                for (String key : events.keys()) {
                    results.add(new PerfResult(key, events.count(key) * 1.0 / totalOpts));
                }

                // Also figure out CPI, if enough counters available:
                {
                    long cycles = events.count("cycles");
                    long instructions = events.count("instructions");
                    if (cycles != 0 && instructions != 0) {
                        results.add(new PerfResult("CPI", 1.0 * cycles / instructions));
                    }
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
            super(ResultRole.SECONDARY, Defaults.PREFIX + key, stat, "#/op", AggregationPolicy.AVG);
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
            return String.format(" %s %s/op", ScoreFormatter.format(getScore()), key);
        }

        @Override
        public String extendedInfo() {
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
