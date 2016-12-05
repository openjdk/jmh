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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPerfProfiler implements ExternalProfiler {

    private final boolean isDelayed;
    private final int delayMs;

    public LinuxPerfProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("perf"));

        OptionSpec<Integer> optDelay = parser.accepts("delay",
                "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
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

        if (isDelayed) {
            return Arrays.asList(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--detailed", "--detailed", "--detailed", "--delay", String.valueOf(delay));
        } else {
            return Arrays.asList(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "--detailed", "--detailed", "--detailed");
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

        try (FileReader fr = new FileReader(stdErr);
             BufferedReader reader = new BufferedReader(fr)) {

            long cycles = 0;
            long insns = 0;

            boolean printing = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (printing) {
                    pw.println(line);
                }
                if (line.contains("Performance counter stats")) {
                    printing = true;
                }

                Matcher m = Pattern.compile("(.*)#(.*)").matcher(line);
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
                    insns
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

        public PerfResult(String output, long cycles, long instructions) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "perf", of(Double.NaN), "---", AggregationPolicy.AVG);
            this.output = output;
            this.cycles = cycles;
            this.instructions = instructions;
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
            return Collections.singletonList(
                    new ScalarDerivativeResult(Defaults.PREFIX + "cpi", 1.0 * cycles / instructions, "CPI", AggregationPolicy.AVG)
            );
        }

        @Override
        public String toString() {
            return String.format("%s cycles per instruction", ScoreFormatter.format(1.0 * cycles / instructions));
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
            String output = "";
            for (PerfResult r : results) {
                cycles += r.cycles;
                instructions += r.instructions;
                output += r.output;
            }
            return new PerfResult(output, cycles, instructions);
        }
    }

}
