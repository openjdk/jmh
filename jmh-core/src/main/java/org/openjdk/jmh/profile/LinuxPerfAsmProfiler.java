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
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.Multiset;
import org.openjdk.jmh.util.TreeMultiset;
import org.openjdk.jmh.util.Utils;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class LinuxPerfAsmProfiler implements ExternalProfiler {

    /** Events to gather **/
    private static final String[] EVENTS = System.getProperty("jmh.perfasm.events", "cycles,instructions").split(",");

    /** Cutoff threshold for hot region: the regions with event count over threshold would be shown */
    private static final double THRESHOLD_RATE = Double.valueOf(System.getProperty("jmh.perfasm.hotThreshold", "0.10"));

    /** Print margin: how many "context" lines without counters to show in each region */
    private static final int PRINT_MARGIN = Integer.getInteger("jmh.perfasm.printMargin", 10);

    /** Merge margin: the regions separated by less than the margin are considered the same */
    private static final int MERGE_MARGIN = Integer.getInteger("jmh.perfasm.mergeMargin", 32);

    /** Delay collection for given time; -1 to detect automatically */
    private static final int DELAY_MSEC = Integer.getInteger("jmh.perfasm.delayMs", -1);

    /** Save perf output to file */
    private static final String SAVE_PERF_OUTPUT = System.getProperty("jmh.perfasm.savePerfTo");

    /** Save perf output to file */
    private static final String SAVE_ASM_OUTPUT = System.getProperty("jmh.perfasm.saveAsmTo");

    private static final boolean IS_SUPPORTED;
    private static final Collection<String> INIT_MSGS;

    static {
        INIT_MSGS = tryWith("perf", "stat", "echo", "1");
        IS_SUPPORTED = INIT_MSGS.isEmpty();
    }

    private String perfBinData;
    private String perfParsedData;

    public LinuxPerfAsmProfiler() throws IOException {
        perfBinData = FileUtils.tempFile("perfbin").getAbsolutePath();
        perfParsedData = FileUtils.tempFile("perfparsed").getAbsolutePath();
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Arrays.asList("perf", "record", "-c 100000", "-e " + Utils.join(EVENTS, ","), "-o" + perfBinData);
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Arrays.asList(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+PrintAssembly");
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkParams params, File stdOut, File stdErr) {
        PerfResult result = processAssembly(params, stdOut, stdErr);
        return Collections.singleton(result);
    }

    @Override
    public Collection<String> checkSupport() {
        return IS_SUPPORTED ? INIT_MSGS : Collections.<String>emptyList();
    }

    public static Collection<String> tryWith(String... cmd) {
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

    private PerfResult processAssembly(BenchmarkParams params, File stdOut, File stdErr) {
        /*
         * 1. Call perf to produce the machine-readable data.
         */

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

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        /**
         * 2. Read out PrintAssembly output
         */

        Assembly assembly = readAssembly(stdOut);
        if (!assembly.isEmpty()) {
            pw.printf("PrintAssembly processed: %d total lines%n", assembly.size());
        } else {
            pw.println();
            pw.println("No assembly, make sure your JDK is PrintAssembly-enabled:\n    https://wikis.oracle.com/display/HotSpotInternals/PrintAssembly");
            pw.println();
        }

        /**
         * 3. Read out perf output
         */

        long delayNs;
        if (DELAY_MSEC == -1) { // not set
            delayNs = params.getWarmup().getCount() *
                            params.getWarmup().getTime().convertTo(TimeUnit.NANOSECONDS)
                            + TimeUnit.SECONDS.toNanos(1); // loosely account for the JVM lag
        } else {
            delayNs = TimeUnit.MILLISECONDS.toNanos(DELAY_MSEC);
        }

        double skipSec = 1.0 * delayNs / TimeUnit.SECONDS.toNanos(1);

        Map<String, Multiset<Long>> events = readEvents(skipSec);

        if (!events.isEmpty()) {
            pw.printf("Perf output processed (skipped %.3f seconds):%n", skipSec);
            int cnt = 1;
            for (String event : EVENTS) {
                pw.printf(" Column %d: %s (%d events)%n", cnt, event, events.get(event).size());
                cnt++;
            }
            pw.println();
        } else {
            pw.println();
            pw.println("No perf data, make sure \"perf stat echo\" is indeed working;\n " +
                    "or the collection delay is not running past the benchmark time.");
            pw.println();
        }

        if (SAVE_PERF_OUTPUT != null) {
            try {
                FileUtils.copy(perfParsedData, SAVE_PERF_OUTPUT);
                pw.println("Perf output saved to " + SAVE_PERF_OUTPUT);
            } catch (IOException e) {
                pw.println("Unable to save perf output to " + SAVE_PERF_OUTPUT);
            }
        }

        /**
         * 4. Figure out generated code regions
         */

        Collection<Region> regions = makeRegions(combine(events), assembly, events);

        /**
         * 5. Figure out interesting regions, and print them out
         */

        Map<String, Long> totalCounts = new HashMap<String, Long>();
        for (String event : EVENTS) {
            totalCounts.put(event, (long) events.get(event).size());
        }

        SortedSet<Region> interestingRegions = new TreeSet<Region>(Region.BEGIN_COMPARATOR);
        for (String event : EVENTS) {
            TreeSet<Region> topRegions = new TreeSet<Region>(Region.getSortedEventComparator(event));
            topRegions.addAll(regions);

            long threshold = (long) (THRESHOLD_RATE * totalCounts.get(event));
            for (Region r : topRegions) {
                if (r.getEventCount(event) > threshold) {
                    interestingRegions.add(r);
                }
            }
        }

        Multiset<String> accounted = new HashMultiset<String>();
        if (!interestingRegions.isEmpty()) {
            pw.println();
            pw.printf("Hottest regions in compiled code (>%.2f%% events):%n", THRESHOLD_RATE * 100);

            int cnt = 1;
            for (Region r : interestingRegions) {
                printDottedLine(pw, "Region " + cnt);
                for (ASMLine line : r.code) {
                    for (String event : EVENTS) {
                        int count = (line.addr != null) ? r.events.get(event).count(line.addr) : 0;
                        if (count > 0) {
                            pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                        } else {
                            pw.printf("%9s", "");
                        }
                    }
                    pw.println(line.code);
                }

                printDottedLine(pw, null);
                for (String event : EVENTS) {
                    int count = r.events.get(event).size();
                    if (count > 0) {
                        pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                    } else {
                        pw.printf("%9s", "");
                    }
                    accounted.add(event, count);
                }
                pw.println("<total for region " + cnt + ">");
                pw.println();
                cnt++;
            }
        }

        /**
         * 6. Print out all residuals
         */

        printDottedLine(pw, "Other compiled code");
        Multiset<String> allRegions = new HashMultiset<String>();
        for (Region r : regions) {
            for (String event : EVENTS) {
                int count = r.events.get(event).size();
                allRegions.add(event, count);
            }
        }

        for (String event : EVENTS) {
            long count = allRegions.count(event) - accounted.count(event);
            if (count > 0) {
                pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
            } else {
                pw.printf("%9s", "");
            }
        }
        pw.println("<unknown>");
        pw.println();


        printDottedLine(pw, "Other (non-compiled) code");
        for (String event : EVENTS) {
            long count = totalCounts.get(event) - allRegions.count(event);
            if (count > 0) {
                pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
            } else {
                pw.printf("%9s", "");
            }
        }
        pw.println("<unknown>");
        pw.println();

        /**
         * Print annotated assembly, if needed:
         */
        if (SAVE_ASM_OUTPUT != null) {
            FileOutputStream asm = null;
            try {
                asm = new FileOutputStream(SAVE_ASM_OUTPUT);
                PrintWriter pwAsm = new PrintWriter(asm);
                for (ASMLine line : assembly.lines) {
                    for (String event : EVENTS) {
                        int count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                        if (count > 0) {
                            pwAsm.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                        } else {
                            pwAsm.printf("%9s", "");
                        }
                    }
                    pwAsm.println(line.code);
                }
                pwAsm.flush();
                asm.close();

                pw.println("Annotated assembly saved to " + SAVE_ASM_OUTPUT);
            } catch (IOException e) {
                pw.println("Unable to save annotated assembly to " + SAVE_ASM_OUTPUT);
            }
        }

        pw.flush();
        pw.close();

        return new PerfResult(sw.toString());
    }

    void printDottedLine(PrintWriter pw, String header) {
        final int HEADER_WIDTH = 80;

        pw.print("....");
        if (header != null) {
            header = "[" + header + "]";
            pw.print(header);
        } else {
            header = "";
        }

        for (int c = 0; c < HEADER_WIDTH - 4 - header.length(); c++) {
            pw.print(".");
        }
        pw.println();
    }

    Collection<Region> makeRegions(Set<AddrInterval> intervals, Assembly asms, Map<String, Multiset<Long>> events) {
        SortedSet<Region> regions = new TreeSet<Region>(Region.BEGIN_COMPARATOR);

        for (AddrInterval interval : intervals) {
            List<ASMLine> regionLines = asms.getLines(interval.begin, interval.end, PRINT_MARGIN);

            Map<String, Multiset<Long>> eventsByAddr = new LinkedHashMap<String, Multiset<Long>>();
            for (String event : EVENTS) {
                HashMultiset<Long> r = new HashMultiset<Long>();
                for (ASMLine line : regionLines) {
                    if (line.addr == null) continue;
                    int count = events.get(event).count(line.addr);
                    r.add(line.addr, count);
                }
                eventsByAddr.put(event, r);
            }

            regions.add(new Region(interval.begin, interval.end, regionLines, eventsByAddr));
        }

        return regions;
    }

    static class PerfResult extends Result<PerfResult> {
        private final String output;

        public PerfResult(String output) {
            super(ResultRole.SECONDARY, "@asm", of(Double.NaN), "N/A", AggregationPolicy.AVG);
            this.output = output;
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
            return "(text only)";
        }

        @Override
        public String extendedInfo(String label) {
            return output;
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

    static class Assembly {
        List<ASMLine> lines;
        SortedMap<Long, Integer> addressMap;

        public Assembly(List<ASMLine> lines, SortedMap<Long, Integer> addressMap) {
            this.lines = lines;
            this.addressMap = addressMap;
        }

        public boolean isEmpty() {
            return lines.isEmpty();
        }

        public int size() {
            return lines.size();
        }

        public List<ASMLine> getLines(long begin, long end, int window) {
            SortedMap<Long, Integer> tailMap = addressMap.tailMap(begin);

            Long beginAddr;
            Integer beginIdx;
            if (!tailMap.isEmpty()) {
                beginAddr = tailMap.firstKey();
                beginIdx = addressMap.get(beginAddr);
            } else {
                return Collections.emptyList();
            }

            SortedMap<Long, Integer> headMap = addressMap.headMap(end);

            Long endAddr;
            Integer endIdx;
            if (!headMap.isEmpty()) {
                endAddr = headMap.lastKey();
                endIdx = addressMap.get(endAddr);
            } else {
                return Collections.emptyList();
            }

            beginIdx = Math.max(0, beginIdx - window);
            endIdx = Math.min(lines.size(), endIdx + 2 + window);

            // Compensate for minute discrepancies
            if (beginIdx < endIdx) {
                return lines.subList(beginIdx, endIdx);
            } else {
                return Collections.emptyList();
            }
        }
    }

    Assembly readAssembly(File stdOut) {
        try {
            List<ASMLine> lines = new ArrayList<ASMLine>();
            SortedMap<Long, Integer> addressMap = new TreeMap<Long, Integer>();
            String line;
            BufferedReader br = new BufferedReader(new FileReader(stdOut));
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] elements = line.trim().split(" ");

                if (elements.length >= 1) {
                    // Seems to be assembly line
                    try {
                        Long addr = Long.valueOf(elements[0].replace("0x", "").replace(":", ""), 16);
                        int idx = lines.size();
                        lines.add(new ASMLine(addr, line));
                        addressMap.put(addr, idx);
                    } catch (NumberFormatException e) {
                        lines.add(new ASMLine(line));
                    }
                } else {
                    lines.add(new ASMLine(line));
                }
            }
            return new Assembly(lines, addressMap);
        } catch (IOException e) {
            return new Assembly(new ArrayList<ASMLine>(), new TreeMap<Long, Integer>());
        }
    }

    Map<String, Multiset<Long>> readEvents(double skipSec) {
        try {
            FileInputStream fis = new FileInputStream(perfParsedData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            Map<String, Multiset<Long>> events = new LinkedHashMap<String, Multiset<Long>>();
            for (String evName : EVENTS) {
                events.put(evName, new TreeMultiset<Long>());
            }

            Double startTime = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                line = line.trim();
                String[] elements = line.split("[ ]+");

                if (elements[0].equalsIgnoreCase("java")) {
                    try {
                        Double time = Double.valueOf(elements[2].replace(":", ""));
                        if (startTime == null) {
                            startTime = time;
                        } else {
                            if (time - startTime < skipSec) {
                                continue;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // misformatted line, no timestamp
                        continue;
                    }

                    String evName = elements[3].replace(":", "");
                    Multiset<Long> evs = events.get(evName);
                    try {
                        Long element = Long.valueOf(elements[4], 16);
                        evs.add(element);
                    } catch (NumberFormatException e) {
                        // should it be kernel code?
                    }
                }
            }

            return events;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    Set<AddrInterval> combine(Map<String, Multiset<Long>> events) {
        SortedSet<AddrInterval> intervals = new TreeSet<AddrInterval>(new Comparator<AddrInterval>() {
            @Override
            public int compare(AddrInterval o1, AddrInterval o2) {
                return Long.valueOf(o1.begin).compareTo(o2.begin);
            }
        });

        SortedSet<Long> addrs = new TreeSet<Long>();
        for (Map.Entry<String, Multiset<Long>> e : events.entrySet()) {
            addrs.addAll(e.getValue().keys());
        }

        Long lastBegin = null;
        Long lastAddr = null;
        for (Long addr : addrs) {
            if (lastAddr == null) {
                lastAddr = addr;
                lastBegin = addr;
            } else {
                if (addr - lastAddr > MERGE_MARGIN) {
                    intervals.add(new AddrInterval(lastBegin, lastAddr));
                    lastBegin = addr;
                }
                lastAddr = addr;
            }
        }

        return intervals;
    }

    static class ASMLine {
        Long addr;
        String code;

        ASMLine(String code) {
            this.code = code;
        }

        ASMLine(long addr, String code) {
            this.addr = addr;
            this.code = code;
        }
    }

    static class AddrInterval {
        long begin;
        long end;

        AddrInterval(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }
    }

    static class Region {
        static final Comparator<Region> BEGIN_COMPARATOR = new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return Long.valueOf(o1.begin).compareTo(o2.begin);
            }
        };

        static Comparator<Region> getSortedEventComparator(final String event) {
            return new Comparator<Region>() {
                @Override
                public int compare(Region o1, Region o2) {
                    return Integer.valueOf(o2.getEventCount(event)).compareTo(o1.getEventCount(event));
                }
            };
        }

        long begin;
        long end;
        Collection<ASMLine> code;
        Map<String, Multiset<Long>> events;

        Region(long begin, long end, Collection<ASMLine> asms, Map<String, Multiset<Long>> events) {
            this.begin = begin;
            this.end = end;
            this.code = asms;
            this.events = events;
        }

        int getEventCount(String event) {
            return events.get(event).size();
        }

    }

}

