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
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class LinuxPerfNormProfiler implements ExternalProfiler {

    /** This is a non-exhaustive list of events we care about. */
    private static final String[] interestingEvents = new String[]{
            "cycles", "instructions",
            "branches", "branch-misses",
            "L1-dcache-loads",  "L1-dcache-load-misses",
            "L1-dcache-stores", "L1-dcache-store-misses",
            "L1-icache-loads", "L1-icache-load-misses",
            "LLC-loads", "LLC-load-misses",
            "LLC-stores", "LLC-store-misses",
            "dTLB-loads",  "dTLB-load-misses",
            "dTLB-stores", "dTLB-store-misses",
            "iTLB-loads",  "iTLB-load-misses",
            "stalled-cycles-frontend", "stalled-cycles-backend",
    };

    private final int delayMs;
    private final int lengthMs;
    private final boolean useDefaultStats;
    private final int incrementInterval;
    private final boolean doFilter;

    private final Collection<String> supportedEvents = new ArrayList<>();

    public LinuxPerfNormProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("perfnorm"));

        OptionSpec<String> optEvents = parser.accepts("events",
                        "Events to gather.")
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(",").describedAs("event+");

        OptionSpec<Integer> optDelay = parser.accepts("delay",
                        "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<Integer> optLength = parser.accepts("length",
                "Do the collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<Integer> optIncrementInterval = parser.accepts("interval",
                        "The interval between incremental updates from a concurrently running perf. " +
                        "Lower values may improve accuracy, while increasing the profiling overhead.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(100);

        OptionSpec<Boolean> optFilter = parser.accepts("filter",
                        "Filter problematic samples from infrastructure and perf itself.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(true);

        OptionSpec<Boolean> optDefaultStat = parser.accepts("useDefaultStat",
                        "Use \"perf stat -d -d -d\" instead of explicit counter list.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        Collection<String> userEvents;

        try {
            delayMs = set.valueOf(optDelay);
            lengthMs = set.valueOf(optLength);
            incrementInterval = set.valueOf(optIncrementInterval);
            doFilter = set.valueOf(optFilter);
            useDefaultStats = set.valueOf(optDefaultStat);
            userEvents = set.valuesOf(optEvents);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }

        Collection<String> msgs = Utils.tryWith(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--field-separator", ",", "echo", "1");
        if (!msgs.isEmpty()) {
            throw new ProfilerException(msgs.toString());
        }

        Collection<String> incremental = Utils.tryWith(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--field-separator", ",", "--interval-print", String.valueOf(incrementInterval), "echo", "1");
        if (!incremental.isEmpty()) {
            throw new ProfilerException("\\\"perf\\\" is too old, needs incremental mode (-I).");
        }

        Collection<String> candidateEvents = new ArrayList<>();
        if (userEvents != null) {
            for (String ev : userEvents) {
                if (ev.trim().isEmpty()) continue;
                candidateEvents.add(ev);
            }
        }

        if (candidateEvents.isEmpty()) {
            candidateEvents.addAll(Arrays.asList(interestingEvents));
        }

        for (String ev : candidateEvents) {
            String[] senseCmd = { PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--field-separator", ",", "--event", ev, "echo", "1" };
            Collection<String> res = Utils.tryWith(senseCmd);
            if (res.isEmpty()) {
                Collection<String> out = Utils.runWith(senseCmd);
                if (!PerfSupport.containsUnsupported(out, ev)) {
                    supportedEvents.add(ev);
                }
            }
        }

        if (!useDefaultStats && supportedEvents.isEmpty()) {
            throw new ProfilerException("No supported events.");
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        List<String> cmd = new ArrayList<>();
        if (useDefaultStats) {
            cmd.addAll(Arrays.asList(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--field-separator", ",", "--detailed", "--detailed", "--detailed"));
        } else {
            cmd.addAll(Arrays.asList(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--field-separator", ",", "--event", Utils.join(supportedEvents, ",")));
        }
        cmd.addAll(Arrays.asList("-I", String.valueOf(incrementInterval)));
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

    private static class EventRecord {
        final double time;
        final double value;
        public EventRecord(double time, double value) {
            this.time = time;
            this.value = value;
        }
    }

    private Collection<? extends Result> process(BenchmarkResult br, File stdOut, File stdErr) {
        Multimap<String, EventRecord> eventRecords = new HashMultimap<>();

        try (FileReader fr = new FileReader(stdErr);
             BufferedReader reader = new BufferedReader(fr)) {

            long skipMs;
            if (delayMs == -1) { // not set
                skipMs = ProfilerUtils.measurementDelayMs(br);
            } else {
                skipMs = delayMs;
            }

            double lenMs;
            if (lengthMs == -1) { // not set
                lenMs = ProfilerUtils.measuredTimeMs(br);
            } else {
                lenMs = lengthMs;
            }

            double readFrom = skipMs / 1000D;
            double readTo = (skipMs + lenMs + incrementInterval) / 1000D;

            NumberFormat nf = NumberFormat.getInstance();

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;

                String[] split = line.split(",");

                String time;
                String count;
                String event;

                if (split.length == 3) {
                    // perf 3.13: time,count,event
                    time = split[0].trim();
                    count = split[1].trim();
                    event = split[2].trim();
                } else if (split.length >= 4) {
                    // perf >3.13: time,count,<other>,event,<others>
                    time = split[0].trim();
                    count = split[1].trim();
                    event = split[3].trim();
                } else {
                    // Malformed line, ignore
                    continue;
                }

                double timeSec = 0D;
                try {
                    timeSec = nf.parse(time).doubleValue();
                } catch (ParseException e) {
                    continue;
                }

                if (timeSec < readFrom || timeSec > readTo) {
                    // Outside measurement window, ignore
                    continue;
                }

                long lValue = 0L;
                try {
                    lValue = nf.parse(count).longValue();
                } catch (ParseException e) {
                    continue;
                }

                eventRecords.put(event, new EventRecord(timeSec, lValue));
            }

            Map<String, Double> finalThroughputs = new HashMap<>();
            for (String key : eventRecords.keys()) {
                List<EventRecord> countedEvents = new ArrayList<>(eventRecords.get(key));

                // If needed and possible, filter out a few head and tail iterations.
                // Head iteration potentially contains the infrastructure startup.
                // That iteration would only be handled for getting time, not the counter itself.
                // Trailing iterations come with both infrastructure rampdown, and the final
                // profiler output on shutdown. Sometimes these manifest as two separate iterations.
                final int filteredCount = countedEvents.size() - 2;
                if (doFilter && filteredCount > 0) {
                    countedEvents = countedEvents.subList(0, filteredCount);
                }

                double s = 0;
                double minTime = Double.MIN_VALUE;
                double maxTime = Double.MIN_VALUE;

                for (int i = 0; i < countedEvents.size(); i++) {
                    EventRecord v = countedEvents.get(i);
                    if (i != 0) {
                        // Do not count the first event in the series, since time interval
                        // does not actually include it.
                        s += v.value;
                    }
                    minTime = Math.min(minTime, v.time);
                    maxTime = Math.max(maxTime, v.time);
                }
                double thr = s / (maxTime - minTime);
                finalThroughputs.put(key, thr);
            }

            BenchmarkResultMetaData md = br.getMetadata();
            if (md == null) {
                return emptyResults();
            }

            long timeMs = md.getStopTime() - md.getMeasurementTime();
            if (timeMs == 0) {
                return emptyResults();

            }
            double opsThroughput = 1000D * md.getMeasurementOps() / timeMs;
            if (opsThroughput == 0) {
                return emptyResults();
            }

            Collection<Result> results = new ArrayList<>();
            for (String key : finalThroughputs.keySet()) {
                results.add(new PerfResult(key, "#/op", finalThroughputs.get(key) / opsThroughput));
            }

            // Also figure out IPC/CPI, if enough counters available:
            {
                Double c1 = finalThroughputs.get("cycles");
                Double c2 = finalThroughputs.get("cycles:u");

                Double i1 = finalThroughputs.get("instructions");
                Double i2 = finalThroughputs.get("instructions:u");

                Double cycles = (c1 != null) ? c1 : c2;
                Double instructions = (i1 != null) ? i1 : i2;

                if (cycles != null && instructions != null &&
                       cycles != 0 && instructions != 0) {
                    results.add(new PerfResult("CPI", "clks/insn", cycles / instructions));
                    results.add(new PerfResult("IPC", "insns/clk", instructions / cycles));
                }
            }

            return results;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<PerfResult> emptyResults() {
        return Collections.singleton(new PerfResult("N/A", "", Double.NaN));
    }

    static class PerfResult extends ScalarResult {
        private static final long serialVersionUID = -1262685915873231436L;

        public PerfResult(String key, String unit, double value) {
            super(key, value, unit, AggregationPolicy.AVG);
        }

        @Override
        public String extendedInfo() {
            // omit printing in extended info
            return "";
        }
    }


}
