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
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.util.InputStreamDrainer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPerfProfiler implements ExternalProfiler {

    /** Delay collection for given time; -1 to detect automatically */
    private static final int DELAY_MSEC = Integer.getInteger("jmh.perf.delayMs", -1);

    private static final boolean IS_SUPPORTED;
    private static final boolean IS_DELAYED;
    private static final Collection<String> INIT_MSGS;

    static {
        INIT_MSGS = tryWith("perf", "stat", "echo", "1");
        IS_SUPPORTED = INIT_MSGS.isEmpty();

        Collection<String> delay = tryWith("perf", "stat", "-D 1", "echo", "1");
        IS_DELAYED = delay.isEmpty();
    }

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
            return Arrays.asList("perf", "stat", "-d", "-d", "-d", "-D " + delay);
        } else {
            return Arrays.asList("perf", "stat", "-d", "-d", "-d");
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
    public Collection<? extends Result> afterTrial(BenchmarkParams params, File stdOut, File stdErr) {
        PerfResult result = process(stdOut, stdErr);
        return Collections.singleton(result);
    }

    @Override
    public Collection<String> checkSupport() {
        return IS_SUPPORTED ? INIT_MSGS : Collections.<String>emptyList();
    }

    private static Collection<String> tryWith(String... cmd) {
        Collection<String> messages = new ArrayList<String>();
        try {
            Process p = Runtime.getRuntime().exec(cmd);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), baos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), baos);

            errDrainer.start();
            outDrainer.start();

            int err = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            if (err > 0) {
                messages.add(baos.toString());
            }
        } catch (IOException ex) {
            return Collections.singleton(ex.getMessage());
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return messages;
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

        try {
            FileInputStream fis = new FileInputStream(stdErr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

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
                    String pair = m.group(1).replace(",", "").trim();
                    if (pair.contains(" cycles")) {
                        cycles = Long.valueOf(pair.split("[ ]+")[0]);
                    }
                    if (line.contains(" instructions")) {
                        insns = Long.valueOf(pair.split("[ ]+")[0]);
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
        }
    }

    static class PerfResult extends Result<PerfResult> {
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
            return String.format("%.3f cycles per instruction", 1.0 * cycles / instructions);
        }

        @Override
        public String extendedInfo(String label) {
            return "Perf stats:\n--------------------------------------------------\n" + output;
        }
    }

    static class PerfResultAggregator implements Aggregator<PerfResult> {

        @Override
        public Result aggregate(Collection<PerfResult> results) {
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
