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
import org.openjdk.jmh.util.ScoreFormatter;
import org.openjdk.jmh.util.Utils;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPerfProfiler implements ExternalProfiler {

    private final boolean isDelayed;
    private final int delayMs;
    private final List<String> events;

    public LinuxPerfProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("perf"));

        OptionSpec<Integer> optDelay = parser.accepts("delay",
                "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<String> optEvents = parser.accepts("events",
                        "Events to gather.")
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(",").describedAs("event");

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            events = set.valuesOf(optEvents);
            delayMs = set.valueOf(optDelay);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }

        Collection<String> msgs = Utils.tryWith(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "echo", "1");
        if (!msgs.isEmpty()) {
            throw new ProfilerException(msgs.toString());
        }

        Collection<String> delay = Utils.tryWith(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--delay", "1", "echo", "1");
        isDelayed = delay.isEmpty();
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        long delay;
        if (delayMs == -1) { // not set
            delay = TimeUnit.NANOSECONDS.toMillis(params.getWarmup().getCount() *
                            params.getWarmup().getTime().convertTo(TimeUnit.NANOSECONDS))
                    + TimeUnit.SECONDS.toMillis(1); // loosely account for the JVM lag
        } else {
            delay = delayMs;
        }

        List<String> invokeOptions = new ArrayList<>(Arrays.asList(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--detailed", "--detailed", "--detailed"));

        if (isDelayed) {
            invokeOptions.add("--delay");
            invokeOptions.add(String.valueOf(delay));
        }
        if (!events.isEmpty()) {
            invokeOptions.add("-e");
            invokeOptions.add(Utils.join(events, ","));
        }

        return invokeOptions;
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
        PerfResult result = process(stdOut, stdErr);
        return Collections.singleton(result);
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
        return "Linux perf Statistics";
    }

    private PerfResult process(File stdOut, File stdErr) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        final Pattern hashLinePattern = Pattern.compile("(.*)#(.*)");

        try (FileReader fr = new FileReader(stdErr);
             BufferedReader reader = new BufferedReader(fr)) {

            long cycles = 0;
            long insns = 0;
            double joulesCpu = Double.NaN;
            double joulesRam = Double.NaN;

            boolean printing = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (printing) {
                    pw.println(line);
                }
                if (line.contains("Performance counter stats")) {
                    printing = true;
                }

                Matcher m = hashLinePattern.matcher(line);
                if (m.matches()) {
                    String pair = m.group(1).trim();
                    if (pair.contains(" cycles")) {
                        try {
                            cycles = NumberFormat.getInstance().parse(pair.split("[ ]+")[0]).longValue();
                        } catch (ParseException e) {
                            // do nothing, processing code will handle
                        }
                    }
                    if (line.contains(" instructions")) {
                        try {
                            insns = NumberFormat.getInstance().parse(pair.split("[ ]+")[0]).longValue();
                        } catch (ParseException e) {
                            // do nothing, processing code will handle
                        }
                    }
                }

                if (line.contains("power/energy-cores/")) {
                    try {
                        joulesCpu = NumberFormat.getInstance().parse(line.trim().split(" ")[0]).doubleValue();
                    } catch (ParseException e) {
                        // do nothing, processing code will handle
                    }
                }
                if (line.contains("power/energy-ram/")) {
                    try {
                        joulesRam = NumberFormat.getInstance().parse(line.trim().split(" ")[0]).doubleValue();
                    } catch (ParseException e) {
                        // do nothing, processing code will handle
                    }
                }
            }

            if (!isDelayed) {
                pw.println();
                pw.println("WARNING: Your system uses old \"perf\", which can not delay data collection.\n" +
                        "Therefore, perf performance data includes benchmark warmup.");
            }

            pw.flush();
            pw.close();

            return new PerfResult(
                    sw.toString(),
                    cycles,
                    insns,
                    joulesCpu,
                    joulesRam
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static class PerfResult extends Result<PerfResult> {
        private static final long serialVersionUID = -1262685915873231436L;

        private final String output;
        private final long cycles;
        private final long instructions;
        private final double joulesCpu;
        private final double joulesRam;

        public PerfResult(String output, long cycles, long instructions, double joulesCpu, double joulesRam) {
            super(ResultRole.SECONDARY, "perf", of(Double.NaN), "---", AggregationPolicy.AVG);
            this.output = output;
            this.cycles = cycles;
            this.instructions = instructions;
            this.joulesCpu = joulesCpu;
            this.joulesRam = joulesRam;
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
        protected Collection<? extends Result> getDerivativeResults() {
            List<Result<?>> res = new ArrayList<>(4);

            if (cycles != 0 && instructions != 0) {
                res.add(new ScalarDerivativeResult("ipc", 1.0 * instructions / cycles, "insns/clk", AggregationPolicy.AVG));
                res.add(new ScalarDerivativeResult("cpi", 1.0 * cycles / instructions, "clks/insn", AggregationPolicy.AVG));

            }

            if(!Double.isNaN(joulesCpu)) {
                res.add(new ScalarDerivativeResult("power/energy-cores/", joulesCpu, "Joules", AggregationPolicy.AVG));
            }
            if(!Double.isNaN(joulesRam)) {
                res.add(new ScalarDerivativeResult("power/energy-ram/", joulesRam, "Joules", AggregationPolicy.AVG));
            }

            return res;
        }

        @Override
        public String toString() {
            if (cycles != 0 && instructions != 0) {
                StringBuilder str = new StringBuilder();

                str.append(String.format("%s IPC, %s CPI",
                        ScoreFormatter.format(1.0 * instructions / cycles),
                        ScoreFormatter.format(1.0 * cycles / instructions)));


                if(!Double.isNaN(joulesCpu)) {
                    str.append(String.format(", %s CPU Joules", ScoreFormatter.format(joulesCpu)));
                }
                if(!Double.isNaN(joulesRam)) {
                    str.append(String.format(", %s RAM Joules", ScoreFormatter.format(joulesRam)));
                }

                return str.toString();
            } else {
                return "N/A";
            }
        }

        @Override
        public String extendedInfo() {
            return "Perf stats:\n--------------------------------------------------\n" + output;
        }
    }

    static class PerfResultAggregator implements Aggregator<PerfResult> {

        @Override
        public PerfResult aggregate(Collection<PerfResult> results) {
            long cycles = 0;
            long instructions = 0;
            double joulesCpu = 0.0;
            double joulesRam = 0.0;
            StringBuilder output = new StringBuilder();
            for (PerfResult r : results) {
                cycles += r.cycles;
                instructions += r.instructions;
                joulesCpu += r.joulesCpu;
                joulesRam += r.joulesRam;

                output.append(r.output);
            }
            return new PerfResult(output.toString(), cycles, instructions, joulesCpu, joulesRam);
        }
    }

}
