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

import org.openjdk.jmh.logic.results.AggregationPolicy;
import org.openjdk.jmh.logic.results.Aggregator;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.ResultRole;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.util.internal.Multiset;
import org.openjdk.jmh.util.internal.TreeMultiset;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class LinuxPerfAsmProfiler implements ExternalProfiler {
    private static final String[] EVENTS = {"cycles", "instructions"};
    private static final String[] EVENTS_SHORT = {"clk", "insn"};

    private String perfBinData;
    private String perfParsedData;
    private boolean useDelay;

    public LinuxPerfAsmProfiler() throws IOException {
        perfBinData = FileUtils.tempFile("perfbin").getAbsolutePath();
        perfParsedData = FileUtils.tempFile("perfparsed").getAbsolutePath();
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        long delay = TimeUnit.NANOSECONDS.toMillis(
                params.getWarmup().getCount() *
                        params.getWarmup().getTime().convertTo(TimeUnit.NANOSECONDS)
                        + 1000 // loosely account for the JVM lag
        );

        if (useDelay) {
            return Arrays.asList("perf", "record", "-c 100000", "-e " + Utils.join(EVENTS, ","), "-o" + perfBinData, "-D " + delay);
        } else {
            return Arrays.asList("perf", "record", "-c 100000", "-e " + Utils.join(EVENTS, ","), "-o" + perfBinData);
        }
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Arrays.asList(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+PrintAssembly");
    }

    @Override
    public void beforeTrial() {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(File stdOut, File stdErr) {
        PerfResult result = processAssembly(stdOut, stdErr);
        return Collections.singleton(result);
    }

    @Override
    public Collection<String> checkSupport() {
        Collection<String> delay = tryWith("perf stat -D 1 echo 1");
        if (delay.isEmpty()) {
            useDelay = true;
            return delay;
        }

        return tryWith("perf stat echo 1");
    }

    public Collection<String> tryWith(String cmd) {
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
        return "perfasm";
    }

    @Override
    public String getDescription() {
        return "Linux perf + PrintAssembly Profiler";
    }

    private PerfResult processAssembly(File stdOut, File stdErr) {
        /*
         * 1. Call perf to produce the machine-readable data.
         */

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            Process p = Runtime.getRuntime().exec("perf script -i " + perfBinData);

            // drain streams, else we might lock up
            FileOutputStream fos = new FileOutputStream(perfParsedData);

            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fos);

            errDrainer.start();
            outDrainer.start();

            p.waitFor();

            errDrainer.join();
            outDrainer.join();

            fos.flush();
            fos.close();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }

        try {

            /*
             * 2. Read the perf data
             */

            FileInputStream fis = new FileInputStream(perfParsedData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            Map<String, Multiset<Long>> events = new TreeMap<String, Multiset<Long>>();
            Multiset<Long> eventAddrs = new TreeMultiset<Long>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                line = line.trim();
                String[] elements = line.split("[ ]+");

                if (elements[0].equalsIgnoreCase("java")) {
                    String evName = elements[3].replace(":", "");
                    Multiset<Long> evs = events.get(evName);
                    if (evs == null) {
                        evs = new TreeMultiset<Long>();
                        events.put(evName, evs);
                    }
                    try {
                        Long element = Long.valueOf(elements[4], 16);
                        evs.add(element);
                        eventAddrs.add(element);
                    } catch (NumberFormatException e) {
                        // should it be kernel code?
                    }
                }
            }

            /*
             * 3. Compute threshold cutoff for events.
             */

            final int CONTEXT_BUFFER = 10;
            final double FILTER_THRESHOLD = 0.001;

            long sum = 0L;
            for (long a : eventAddrs.keys()) {
                sum += eventAddrs.count(a);
            }

            final int THRESHOLD = (int) (sum * FILTER_THRESHOLD);

            /*
             * 4. Read the disassembly and selectively print the hottest blocks:
             */

            ArrayDeque<String> buffer = new ArrayDeque<String>(CONTEXT_BUFFER);

            int linesToPrint = 0;

            BufferedReader br = new BufferedReader(new FileReader(stdOut));
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] elements = line.trim().split(" ");

                String bufferedLine = "";

                boolean eventsPrinted = false;
                if (elements.length >= 1) {
                    try {
                        Long addr = Long.valueOf(elements[0].replace("0x", "").replace(":", ""), 16);
                        int eventCount = eventAddrs.count(addr);

                        if (eventCount > 0) {
                            for (String eventType : EVENTS) {
                                int count = events.get(eventType).count(addr);
                                bufferedLine += String.format("%6d ", count);
                            }
                            eventsPrinted = true;
                        }

                        if (eventCount > THRESHOLD)  {
                            for (String bl : buffer) {
                                pw.println(bl);
                            }
                            buffer.clear();
                            linesToPrint = CONTEXT_BUFFER;
                        }
                    } catch (NumberFormatException e) {
                        // skip
                    }
                }

                if (!eventsPrinted) {
                    for (String eventType : EVENTS) {
                        bufferedLine += String.format("%6s ", "");
                    }
                }

                bufferedLine += line;

                linesToPrint--;
                if (linesToPrint >= 0) {
                    pw.println(bufferedLine);
                    if (linesToPrint == 0) {
                        pw.println("--------------------------------------------------");
                        for (String eventType : EVENTS_SHORT) {
                            pw.print(String.format("%6s ", eventType));
                        }
                        pw.println();
                        pw.println();
                    }
                } else {
                    buffer.add(bufferedLine);
                    if (buffer.size() > CONTEXT_BUFFER) {
                        buffer.pollFirst();
                    }
                }
            }
            pw.println();

            String hot = sw.toString();
            if (hot.trim().isEmpty()) {
                hot = "No assembly, make sure your JDK is PrintAssembly-enabled:\n    https://wikis.oracle.com/display/HotSpotInternals/PrintAssembly";
            }

            return new PerfResult(hot);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static class PerfResult extends Result<PerfResult> {
        private final String output;

        public PerfResult(String output) {
            super(ResultRole.SECONDARY, "@asm", of(Double.NaN), "N/A", AggregationPolicy.AVG);
            this.output = output;
        }

        @Override
        public Aggregator<PerfResult> getIterationAggregator() {
            return new PerfResultAggregator();
        }

        @Override
        public Aggregator<PerfResult> getRunAggregator() {
            return new PerfResultAggregator();
        }

        @Override
        public String toString() {
            return "(text only)";
        }

        @Override
        public String extendedInfo(String label) {
            return "Hottest generated code:\n--------------------------------------------------\n" + output;
        }
    }

    static class PerfResultAggregator implements Aggregator<PerfResult> {
        @Override
        public Result aggregate(Collection<PerfResult> results) {
            String output = "";
            for (PerfResult r : results) {
                output += r.output;
            }
            return new PerfResult(output);
        }
    }

}
