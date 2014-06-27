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
import org.openjdk.jmh.util.Multisets;
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
import java.util.HashSet;
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

    /** Show this number of top residuals in "other" compiled/non-compiled code */
    private static final int SHOW_TOP_RESIDUALS = Integer.getInteger("jmh.perfasm.topResiduals", 5);

    /** Cutoff threshold for large region: the region larger than this would be truncated */
    private static final int THRESHOLD_TOO_BIG = Integer.getInteger("jmh.perfasm.tooBigThreshold", 1000);

    /** Print margin: how many "context" lines without counters to show in each region */
    private static final int PRINT_MARGIN = Integer.getInteger("jmh.perfasm.printMargin", 10);

    /** Merge margin: the regions separated by less than the margin are considered the same */
    private static final int MERGE_MARGIN = Integer.getInteger("jmh.perfasm.mergeMargin", 32);

    /** Delay collection for given time; -1 to detect automatically */
    private static final int DELAY_MSEC = Integer.getInteger("jmh.perfasm.delayMs", -1);

    /** Sampling frequency */
    private static final long SAMPLE_FREQUENCY = Long.getLong("jmh.perfasm.frequency", 1000);

    /** Save perf output to file? */
    private static final Boolean SAVE_PERF_OUTPUT = Boolean.getBoolean("jmh.perfasm.savePerf");

    /** Override the perf output location */
    private static final String SAVE_PERF_OUTPUT_TO = System.getProperty("jmh.perfasm.savePerfTo", ".");

    /** Override the perf output filename */
    private static final String SAVE_PERF_OUTPUT_TO_FILE = System.getProperty("jmh.perfasm.savePerfToFile");

    /** Save annotated assembly to file */
    private static final Boolean SAVE_ASM_OUTPUT = Boolean.getBoolean("jmh.perfasm.saveAsm");

    /** Override the annotated assembly location */
    private static final String SAVE_ASM_OUTPUT_TO = System.getProperty("jmh.perfasm.saveAsmTo", ".");

    /** Override the annotated assembly filename */
    private static final String SAVE_ASM_OUTPUT_TO_FILE = System.getProperty("jmh.perfasm.saveAsmToFile");

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
        return Arrays.asList("perf", "record", "-F " + SAMPLE_FREQUENCY, "-e " + Utils.join(EVENTS, ","), "-o" + perfBinData);
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

        /**
         * 4. Figure out generated code regions
         */

        Collection<Region> regions = makeRegions(assembly, events);

        /**
         * 5. Figure out interesting regions, and print them out
         */

        Map<String, Long> totalCounts = new HashMap<String, Long>();
        for (String event : EVENTS) {
            totalCounts.put(event, events.get(event).size());
        }

        String mainEvent = EVENTS[0];

        SortedSet<Region> interestingRegions = new TreeSet<Region>(Region.BEGIN_COMPARATOR);

        TreeSet<Region> topRegions = new TreeSet<Region>(Region.getSortedEventComparator(events, mainEvent));
        topRegions.addAll(regions);

        long threshold = (long) (THRESHOLD_RATE * totalCounts.get(mainEvent));
        for (Region r : topRegions) {
            if (r.getEventCount(events, mainEvent) > threshold) {
                interestingRegions.add(r);
            }
        }

        Multiset<String> compiled = new HashMultiset<String>();
        if (!interestingRegions.isEmpty()) {
            pw.println();
            pw.printf("Hottest regions in compiled code (>%.2f%% \"%s\" events):%n", THRESHOLD_RATE * 100, mainEvent);

            int cnt = 1;
            for (Region r : interestingRegions) {
                printDottedLine(pw, "Region " + cnt);
                pw.printf(" Starts in \"%s\", spans [0x%x:0x%x]%n%n", r.method, r.begin, r.end);
                if (r.code.size() > THRESHOLD_TOO_BIG) {
                    pw.printf(" <region is too big to display, has %d lines, but threshold is %d>%n", r.code.size(), THRESHOLD_TOO_BIG);
                } else {
                    for (ASMLine line : r.code) {
                        for (String event : EVENTS) {
                            long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                            if (count > 0) {
                                pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                            } else {
                                pw.printf("%9s", "");
                            }
                        }
                        pw.println(line.code);
                    }
                }

                printDottedLine(pw, null);
                for (String event : EVENTS) {
                    long count = r.getEventCount(events, event);
                    if (count > 0) {
                        pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                    } else {
                        pw.printf("%9s", "");
                    }
                    compiled.add(event, count);
                }
                pw.println("<total for region " + cnt + ">");
                pw.println();
                cnt++;
            }
        }

        /**
         * 6. Print out residual compiled code
         */

        topRegions.removeAll(interestingRegions);

        if (!topRegions.isEmpty()) {


            Multiset<String> residualCompiled = new HashMultiset<String>();
            Multiset<String> residualCompiledNonTop = new HashMultiset<String>();

            printDottedLine(pw, "Other compiled code");
            int shown = 0;
            for (Region r : topRegions) {
                if (++shown < SHOW_TOP_RESIDUALS) {
                    for (String event : EVENTS) {
                        long count = r.getEventCount(events, event);
                        if (count > 0) {
                            pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                        } else {
                            pw.printf("%9s", "");
                        }
                    }
                    pw.printf("[0x%x:0x%x] in %s%n", r.begin, r.end, r.method);
                } else {
                    for (String event : EVENTS) {
                        residualCompiledNonTop.add(event, r.getEventCount(events, event));
                    }
                }
                for (String event : EVENTS) {
                    residualCompiled.add(event, r.getEventCount(events, event));
                    compiled.add(event, r.getEventCount(events, event));
                }
            }

            if (topRegions.size() - SHOW_TOP_RESIDUALS > 0) {
                for (String event : EVENTS) {
                    long count = residualCompiledNonTop.count(event);
                    if (count > 0) {
                        pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                    } else {
                        pw.printf("%9s", "");
                    }
                }
                pw.println("<...other " + (topRegions.size() - SHOW_TOP_RESIDUALS) + " warm regions...>");
            }
            printDottedLine(pw, null);

            for (String event : EVENTS) {
                long count = residualCompiled.count(event);
                if (count > 0) {
                    pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                } else {
                    pw.printf("%9s", "");
                }
            }
            pw.println("<totals for other compiled code>");
            pw.println();
        }

        /**
         * Print out the non-compiled code
         */

        printDottedLine(pw, "Other (non-compiled) code");

        {
            Set<Long> nativeAddresses = new HashSet<Long>();
            nativeAddresses.addAll(events.get(mainEvent).keys());
            nativeAddresses.removeAll(assembly.addressMap.keySet());

            Multiset<Long> nativeEvents = new HashMultiset<Long>();
            for (Long addr : nativeAddresses) {
                nativeEvents.add(addr, events.get(mainEvent).count(addr));
            }

            Collection<Long> highest = Multisets.countHighest(nativeEvents, SHOW_TOP_RESIDUALS);

            for (Long addr : highest) {
                for (String event : EVENTS) {
                    long count = nativeEvents.count(addr);
                    if (count > 0) {
                        pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
                    } else {
                        pw.printf("%9s", "");
                    }
                }
                pw.printf("[0x%x]%n", addr);
            }
        }

        printDottedLine(pw, null);

        for (String event : EVENTS) {
            long count = totalCounts.get(event) - compiled.count(event);
            if (count > 0) {
                pw.printf("%6.2f%%  ", 100.0 * count / totalCounts.get(event));
            } else {
                pw.printf("%9s", "");
            }
        }
        pw.println("<totals for non-compiled code>");
        pw.println();

        /**
         * Final checks on assembly:
         */

        {
            Set<Long> addrHistory = new HashSet<Long>();
            for (Long addr : assembly.addressMap.keySet()) {
                if (!addrHistory.add(addr)) {
                    pw.println("WARNING: Duplicate instruction addresses detected. This is probably due to compiler reusing\n " +
                            "the code arena for the new generated code. We can not differentiate between methods sharing\n" +
                            "the same addresses, and therefore the profile might be wrong. Increasing generated code\n" +
                            "storage might help.");
                }
            }
        }

        /**
         * Print perf output, if needed:
         */
        if (SAVE_PERF_OUTPUT) {
            String target = (SAVE_PERF_OUTPUT_TO_FILE == null) ?
                    SAVE_PERF_OUTPUT_TO + "/" + params.id() + ".perf" :
                    SAVE_PERF_OUTPUT_TO_FILE;
            try {
                FileUtils.copy(perfParsedData, target);
                pw.println("Perf output saved to " + target);
            } catch (IOException e) {
                pw.println("Unable to save perf output to " + target);
            }
        }

        /**
         * Print annotated assembly, if needed:
         */
        if (SAVE_ASM_OUTPUT) {
            String target = (SAVE_ASM_OUTPUT_TO_FILE == null) ?
                    SAVE_ASM_OUTPUT_TO + "/" + params.id() + ".asm" :
                    SAVE_ASM_OUTPUT_TO_FILE;
            FileOutputStream asm = null;
            try {
                asm = new FileOutputStream(target);
                PrintWriter pwAsm = new PrintWriter(asm);
                for (ASMLine line : assembly.lines) {
                    for (String event : EVENTS) {
                        long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
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

                pw.println("Perf-annotated assembly saved to " + target);
            } catch (IOException e) {
                pw.println("Unable to save perf-annotated assembly to " + target);
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

    Collection<Region> makeRegions(Assembly asms, Map<String, Multiset<Long>> events) {
        SortedSet<Region> regions = new TreeSet<Region>(Region.BEGIN_COMPARATOR);

        SortedSet<Long> addrs = new TreeSet<Long>();
        for (Map.Entry<String, Multiset<Long>> e : events.entrySet()) {
            addrs.addAll(e.getValue().keys());
        }

        Set<Long> eventfulAddrs = new HashSet<Long>();
        Long lastBegin = null;
        Long lastAddr = null;
        for (Long addr : addrs) {
            if (lastAddr == null) {
                lastAddr = addr;
                lastBegin = addr;
            } else {
                if (addr - lastAddr > MERGE_MARGIN) {
                    List<ASMLine> regionLines = asms.getLines(lastBegin, lastAddr, PRINT_MARGIN);
                    if (!regionLines.isEmpty()) {
                        regions.add(new Region(asms.getMethod(lastBegin), lastBegin, lastAddr, regionLines, eventfulAddrs));
                    } else {
                        // TODO: non-generated-code region
                    }
                    lastBegin = addr;
                    eventfulAddrs = new HashSet<Long>();
                }
                lastAddr = addr;
            }
            eventfulAddrs.add(addr);
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
        SortedMap<Long, String> methodMap;

        public Assembly(List<ASMLine> lines, SortedMap<Long, Integer> addressMap, SortedMap<Long, String> methodMap) {
            this.lines = lines;
            this.addressMap = addressMap;
            this.methodMap = methodMap;
        }

        public Assembly() {
            this(new ArrayList<ASMLine>(), new TreeMap<Long, Integer>(), new TreeMap<Long, String>());
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

        public String getMethod(long addr) {
            SortedMap<Long, String> head = methodMap.headMap(addr);
            if (head.isEmpty()) {
                return "N/A";
            } else {
                return methodMap.get(head.lastKey());
            }
        }
    }

    Assembly readAssembly(File stdOut) {
        try {
            List<ASMLine> lines = new ArrayList<ASMLine>();
            SortedMap<Long, Integer> addressMap = new TreeMap<Long, Integer>();
            SortedMap<Long, String> methodMap = new TreeMap<Long, String>();

            String method = null;
            String line;
            BufferedReader br = new BufferedReader(new FileReader(stdOut));
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] elements = line.trim().split(" ");

                if (line.contains("{method}")) {
                    if (elements.length == 7) {
                        method = elements[6].replace("'", "").replace("/", ".") + "::" + elements[3].replace("'", "");
                    }
                } else if (elements.length >= 1 && elements[0].startsWith("0x")) {
                    // Seems to be assembly line
                    try {
                        Long addr = Long.valueOf(elements[0].replace("0x", "").replace(":", ""), 16);
                        int idx = lines.size();
                        lines.add(new ASMLine(addr, line));
                        addressMap.put(addr, idx);

                        if (method != null) {
                            methodMap.put(addr, method);
                            method = null;
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException("Should not be here", e);
                    }
                } else {
                    lines.add(new ASMLine(line));
                }
            }
            return new Assembly(lines, addressMap, methodMap);
        } catch (IOException e) {
            return new Assembly();
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
                        // TODO: Kernel addresses like "ffffffff810c1b00" overflow signed long
                    }
                }
            }

            return events;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
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

    static class Region {
        static final Comparator<Region> BEGIN_COMPARATOR = new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return Long.valueOf(o1.begin).compareTo(o2.begin);
            }
        };

        static Comparator<Region> getSortedEventComparator(final Map<String, Multiset<Long>> events, final String event) {
            return new Comparator<Region>() {
                @Override
                public int compare(Region o1, Region o2) {
                    return Long.valueOf(o2.getEventCount(events, event)).compareTo(o1.getEventCount(events, event));
                }
            };
        }

        final String method;
        final long begin;
        final long end;
        final Collection<ASMLine> code;
        final Set<Long> eventfulAddrs;
        final Map<String, Long> eventCountCache;

        Region(String method, long begin, long end, Collection<ASMLine> asms, Set<Long> eventfulAddrs) {
            this.method = method;
            this.begin = begin;
            this.end = end;
            this.code = asms;
            this.eventfulAddrs = eventfulAddrs;
            this.eventCountCache = new HashMap<String, Long>();
        }

        long getEventCount(Map<String, Multiset<Long>> events, String event) {
            if (!eventCountCache.containsKey(event)) {
                Multiset<Long> evs = events.get(event);
                long count = 0;
                for (Long addr : eventfulAddrs) {
                    count += evs.count(addr);
                }
                eventCountCache.put(event, count);
            }
            return eventCountCache.get(event);
        }

    }

}

