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
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.ScoreFormatter;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPerfProfiler implements ExternalProfiler {

    private static final boolean IS_SUPPORTED;
    private static final boolean IS_DELAYED;
    private static final Collection<String> FAIL_MSGS;

    static {
        FAIL_MSGS = Utils.tryWith("perf", "stat", "--log-fd", "2", "echo", "1");
        IS_SUPPORTED = FAIL_MSGS.isEmpty();

        Collection<String> delay = Utils.tryWith("perf", "stat", "--log-fd", "2", "-D", "1", "echo", "1");
        IS_DELAYED = delay.isEmpty();
    }

    /** Delay collection for given time; -1 to detect automatically */
    private static final int DELAY_MSEC = Integer.getInteger("jmh.perf.delayMs", -1);

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        long delay;
        if (DELAY_MSEC == -1) { // not set
            delay = TimeUnit.NANOSECONDS.toMillis(params.getWarmup().getCount() *
                            params.getWarmup().getTime().convertTo(TimeUnit.NANOSECONDS))
                    + TimeUnit.SECONDS.toMillis(1); // loosely account for the JVM lag
        } else {
            delay = DELAY_MSEC;
        }

        if (IS_DELAYED) {
            return Arrays.asList("perf", "stat", "--log-fd", "2", "-d", "-d", "-d", "-D", String.valueOf(delay));
        } else {
            return Arrays.asList("perf", "stat", "--log-fd", "2", "-d", "-d", "-d");
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
        return "perf";
    }

    @Override
    public String getDescription() {
        return "Linux perf Statistics";
    }

    private PerfResult process(File stdOut, File stdErr) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        FileReader fr = null;
        try {
            fr = new FileReader(stdErr);
            BufferedReader reader = new BufferedReader(fr);

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

            if (!IS_DELAYED) {
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
        } finally {
            FileUtils.safelyClose(fr);
        }
    }

    static class PerfResult extends Result<PerfResult> {
        private static final long serialVersionUID = -1262685915873231436L;

        private final String output;
        private final long cycles;
        private final long instructions;

        public PerfResult(String output, long cycles, long instructions) {
            super(ResultRole.SECONDARY, "@cpi", of(1.0 * cycles / instructions), "CPI", AggregationPolicy.AVG);
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
        public String toString() {
            return String.format("%s cycles per instruction", ScoreFormatter.format(1.0 * cycles / instructions));
        }

        @Override
        public String extendedInfo(String label) {
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
