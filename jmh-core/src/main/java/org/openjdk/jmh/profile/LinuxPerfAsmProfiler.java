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

    /** Show this number of top hottest code regions */
    private static final int SHOW_TOP = Integer.getInteger("jmh.perfasm.top", 20);

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

    /** Do -XX:+PrintAssembly instrumentation? */
    private static final Boolean SKIP_ASSEMBLY = Boolean.getBoolean("jmh.perfasm.skipAsm");

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
        if (!SKIP_ASSEMBLY) {
            return Arrays.asList(
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+PrintAssembly");
        } else {
            return Collections.emptyList();
        }
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
        } else if (SKIP_ASSEMBLY) {
            pw.println();
            pw.println("PrintAssembly skipped, Java methods are not resolved.");
            pw.println();
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

        final PerfEvents events = readEvents(skipSec);

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
            pw.println("No perf data, make sure \"perf stat echo 1\" is indeed working;\n " +
                    "or the collection delay is not running past the benchmark time.");
            pw.println();
        }

        /**
         * 4. Figure out code regions
         */

        List<Region> regions = makeRegions(assembly, events);

        /**
         * 5. Figure out interesting regions, and print them out
         */

        final String mainEvent = EVENTS[0];

        Collections.sort(regions, new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return Long.valueOf(o2.getEventCount(events, mainEvent)).
                        compareTo(o1.getEventCount(events, mainEvent));
            }
        });

        long threshold = (long) (THRESHOLD_RATE * events.getTotalEvents(mainEvent));

        boolean headerPrinted = false;

        int cnt = 1;
        for (Region r : regions) {
            if (r.getEventCount(events, mainEvent) > threshold) {
                if (!headerPrinted) {
                    pw.printf("Hottest code regions (>%.2f%% \"%s\" events):%n", THRESHOLD_RATE * 100, mainEvent);
                    headerPrinted = true;
                }

                printDottedLine(pw, "Hottest Region " + cnt);
                pw.printf(" [0x%x:0x%x] in %s%n%n", r.begin, r.end, r.method);
                if (r.code.size() > THRESHOLD_TOO_BIG) {
                    pw.printf(" <region is too big to display, has %d lines, but threshold is %d>%n", r.code.size(), THRESHOLD_TOO_BIG);
                } else {
                    if (r.code.isEmpty()) {
                        pw.println(" <no assembly is recorded, native region?>");
                    }
                    for (ASMLine line : r.code) {
                        for (String event : EVENTS) {
                            long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                            printLine(pw, events, event, count);
                        }
                        pw.println(line.code);
                    }
                }

                printDottedLine(pw, null);
                for (String event : EVENTS) {
                    printLine(pw, events, event, r.getEventCount(events, event));
                }
                pw.println("<total for region " + cnt + ">");
                pw.println();
                cnt++;
            }
        }

        /**
         * 6. Print out the hottest regions
         */

        Multiset<String> accounted = new HashMultiset<String>();
        Multiset<String> other = new HashMultiset<String>();

        printDottedLine(pw, "Hottest Regions");
        int shown = 0;
        for (Region r : regions) {
            if (shown++ < SHOW_TOP) {
                for (String event : EVENTS) {
                    printLine(pw, events, event, r.getEventCount(events, event));
                }
                pw.printf("[0x%x:0x%x] in %s%n", r.begin, r.end, r.method);
            } else {
                for (String event : EVENTS) {
                    other.add(event, r.getEventCount(events, event));
                }
            }
            for (String event : EVENTS) {
                accounted.add(event, r.getEventCount(events, event));
            }
        }

        if (regions.size() - SHOW_TOP > 0) {
            for (String event : EVENTS) {
                printLine(pw, events, event, other.count(event));
            }
            pw.println("<...other " + (regions.size() - SHOW_TOP) + " warm regions...>");
        }
        printDottedLine(pw, null);

        for (String event : EVENTS) {
            printLine(pw, events, event, accounted.count(event));
        }
        pw.println("<totals>");
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
            FileOutputStream asm;
            try {
                asm = new FileOutputStream(target);
                PrintWriter pwAsm = new PrintWriter(asm);
                for (ASMLine line : assembly.lines) {
                    for (String event : EVENTS) {
                        long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                        printLine(pwAsm, events, event, count);
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

    static void printLine(PrintWriter pw, PerfEvents events, String event, long count) {
        if (count > 0) {
            pw.printf("%6.2f%%  ", 100.0 * count / events.getTotalEvents(event));
        } else {
            pw.printf("%9s", "");
        }
    }

    void printDottedLine(PrintWriter pw, String header) {
        final int HEADER_WIDTH = 100;

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

    List<Region> makeRegions(Assembly asms, PerfEvents events) {
        List<Region> regions = new ArrayList<Region>();

        SortedSet<Long> addrs = events.getAllAddresses();

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

                    String name;
                    if (!regionLines.isEmpty()) {
                        // Compiled code
                        name = asms.getMethod(lastBegin);
                    } else {
                        // Non-compiled code
                        Set<String> methods = new HashSet<String>();
                        for (Long ea : eventfulAddrs) {
                            methods.add(events.methods.get(ea));
                        }
                        name = methods.toString();
                    }
                    regions.add(new Region(name, lastBegin, lastAddr, regionLines, eventfulAddrs));
                    lastBegin = addr;
                    eventfulAddrs = new HashSet<Long>();
                }
                lastAddr = addr;
            }
            eventfulAddrs.add(addr);
        }

        return regions;
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

    PerfEvents readEvents(double skipSec) {
        try {
            FileInputStream fis = new FileInputStream(perfParsedData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            Map<Long, String> methods = new HashMap<Long, String>();
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
                        methods.put(element, elements[5]); // TODO: Deduplicate method names
                    } catch (NumberFormatException e) {
                        // TODO: Kernel addresses like "ffffffff810c1b00" overflow signed long
                    }
                }
            }

            return new PerfEvents(events, methods);
        } catch (IOException e) {
            return new PerfEvents();
        }
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

    static class PerfEvents {
        final Map<String, Multiset<Long>> events;
        final Map<Long, String> methods;
        final Map<String, Long> totalCounts;

        PerfEvents(Map<String, Multiset<Long>> events, Map<Long, String> methods) {
            this.events = events;
            this.methods = methods;
            this.totalCounts = new HashMap<String, Long>();
            for (String event : EVENTS) {
                totalCounts.put(event, events.get(event).size());
            }
        }

        public PerfEvents() {
            this(Collections.<String, Multiset<Long>>emptyMap(), Collections.<Long, String>emptyMap());
        }

        public boolean isEmpty() {
            return events.isEmpty();
        }

        public Multiset<Long> get(String event) {
            return events.get(event);
        }

        public SortedSet<Long> getAllAddresses() {
            SortedSet<Long> addrs = new TreeSet<Long>();
            for (Multiset<Long> e : events.values()) {
                addrs.addAll(e.keys());
            }
            return addrs;
        }

        public Long getTotalEvents(String event) {
            return totalCounts.get(event);
        }
    }

    static class Assembly {
        final List<ASMLine> lines;
        final SortedMap<Long, Integer> addressMap;
        final SortedMap<Long, String> methodMap;

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

    static class ASMLine {
        final Long addr;
        final String code;

        ASMLine(String code) {
            this(null, code);
        }

        ASMLine(Long addr, String code) {
            this.addr = addr;
            this.code = code;
        }
    }

    static class Region {
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

        long getEventCount(PerfEvents events, String event) {
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

