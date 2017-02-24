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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPerfAsmProfiler implements ExternalProfiler {

    protected final List<String> events;

    private final double regionRateThreshold;
    private final int regionShowTop;
    private final int regionTooBigThreshold;
    private final int printMargin;
    private final int mergeMargin;
    private final int delayMsec;
    private final int lengthMsec;

    private final boolean skipAssembly;
    private final boolean skipInterpreter;
    private final boolean skipVMStubs;

    private final boolean savePerfOutput;
    private final String savePerfOutputTo;
    private final String savePerfOutputToFile;

    private final boolean savePerfBin;
    private final String savePerfBinTo;
    private final String savePerfBinFile;

    private final boolean saveLog;
    private final String saveLogTo;
    private final String saveLogToFile;

    private final boolean printCompilationInfo;
    private final boolean intelSyntax;

    protected final TempFile hsLog;
    protected final TempFile perfBinData;
    protected final TempFile perfParsedData;
    protected final OptionSet set;
    private final boolean drawIntraJumps;
    private final boolean drawInterJumps;

    protected AbstractPerfAsmProfiler(String initLine, String... events) throws ProfilerException {
        try {
            hsLog = FileUtils.weakTempFile("hslog");
            perfBinData = FileUtils.weakTempFile("perfbin");
            perfParsedData = FileUtils.weakTempFile("perfparsed");
        } catch (IOException e) {
            throw new ProfilerException(e);
        }

        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("perfasm"));

        OptionSpec<String> optEvents = parser.accepts("events",
                        "Events to gather.")
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(",").describedAs("event").defaultsTo(events);

        OptionSpec<Double> optThresholdRate = parser.accepts("hotThreshold",
                "Cutoff threshold for hot regions. The regions with event count over threshold would be expanded " +
                        "with detailed disassembly.")
                .withRequiredArg().ofType(Double.class).describedAs("rate").defaultsTo(0.10);

        OptionSpec<Integer> optShowTop = parser.accepts("top",
                        "Show this number of top hottest code regions.")
                .withRequiredArg().ofType(Integer.class).describedAs("#").defaultsTo(20);

        OptionSpec<Integer> optThreshold = parser.accepts("tooBigThreshold",
                        "Cutoff threshold for large region. The region containing more than this number of lines " +
                        "would be truncated.")
                .withRequiredArg().ofType(Integer.class).describedAs("lines").defaultsTo(1000);

        OptionSpec<Integer> optPrintMargin = parser.accepts("printMargin",
                        "Print margin. How many \"context\" lines without counters to show in each region.")
                .withRequiredArg().ofType(Integer.class).describedAs("lines").defaultsTo(10);

        OptionSpec<Integer> optMergeMargin = parser.accepts("mergeMargin",
                        "Merge margin. The regions separated by less than the margin are merged.")
                .withRequiredArg().ofType(Integer.class).describedAs("lines").defaultsTo(32);

        OptionSpec<Integer> optDelay = parser.accepts("delay",
                        "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<Integer> optLength = parser.accepts("length",
                        "Do the collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);

        OptionSpec<Boolean> optSkipAsm = parser.accepts("skipAsm",
                        "Skip -XX:+PrintAssembly instrumentation.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<Boolean> optSkipInterpreter = parser.accepts("skipInterpreter",
                        "Skip printing out interpreter stubs. This may improve the parser performance at the expense " +
                        "of missing the resolution and disassembly of interpreter regions.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<Boolean> optSkipVMStubs = parser.accepts("skipVMStubs",
                        "Skip printing out VM stubs. This may improve the parser performance at the expense " +
                        "of missing the resolution and disassembly of VM stub regions.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<Boolean> optPerfOut = parser.accepts("savePerf",
                        "Save parsed perf output to file. Use this for debugging.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<String> optPerfOutTo = parser.accepts("savePerfTo",
                        "Override the parsed perf output log location. This will use the unique file name per test. Use this for debugging.")
                .withRequiredArg().ofType(String.class).describedAs("dir").defaultsTo(".");

        OptionSpec<String> optPerfOutToFile = parser.accepts("savePerfToFile",
                "Override the perf output log filename. Use this for debugging.")
                .withRequiredArg().ofType(String.class).describedAs("file");

        OptionSpec<Boolean> optPerfBin = parser.accepts("savePerfBin",
                        "Save binary perf data to file. Use this for debugging.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<String> optPerfBinTo = parser.accepts("savePerfBinTo",
                        "Override the binary perf data location. This will use the unique file name per test. Use this for debugging.")
                .withRequiredArg().ofType(String.class).describedAs("dir").defaultsTo(".");

        OptionSpec<String> optPerfBinToFile = parser.accepts("savePerfBinToFile",
                "Override the perf binary data filename. Use this for debugging.")
                .withRequiredArg().ofType(String.class).describedAs("file");

        OptionSpec<Boolean> optSaveLog = parser.accepts("saveLog",
                        "Save annotated Hotspot log to file.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<String> optSaveLogTo = parser.accepts("saveLogTo",
                        "Override the annotated Hotspot log location. This will use the unique file name per test.")
                .withRequiredArg().ofType(String.class).describedAs("dir").defaultsTo(".");

        OptionSpec<String> optSaveLogToFile = parser.accepts("saveLogToFile",
                "Override the annotated Hotspot log filename.")
                .withRequiredArg().ofType(String.class).describedAs("file");

        OptionSpec<Boolean> optPrintCompilationInfo = parser.accepts("printCompilationInfo",
                        "Print the collateral compilation information. Enabling this might corrupt the " +
                        "assembly output, see https://bugs.openjdk.java.net/browse/CODETOOLS-7901102.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<Boolean> optIntelSyntax = parser.accepts("intelSyntax",
                        "Should perfasm use intel syntax?")
                .withRequiredArg().ofType(Boolean.class).describedAs("boolean").defaultsTo(false);

        OptionSpec<Boolean> optDrawIntraJumps = parser.accepts("drawIntraJumps",
                        "Should perfasm draw jump arrows with the region?")
                .withRequiredArg().ofType(Boolean.class).describedAs("boolean").defaultsTo(true);

        OptionSpec<Boolean> optDrawInterJumps = parser.accepts("drawInterJumps",
                        "Should perfasm draw jump arrows out of the region?")
                .withRequiredArg().ofType(Boolean.class).describedAs("boolean").defaultsTo(false);

        addMyOptions(parser);

        set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            this.events = set.valuesOf(optEvents);
            regionRateThreshold = set.valueOf(optThresholdRate);
            regionShowTop = set.valueOf(optShowTop);
            regionTooBigThreshold = set.valueOf(optThreshold);
            printMargin = set.valueOf(optPrintMargin);
            mergeMargin = set.valueOf(optMergeMargin);
            delayMsec = set.valueOf(optDelay);
            lengthMsec = set.valueOf(optLength);

            skipAssembly = set.valueOf(optSkipAsm);
            skipInterpreter = set.valueOf(optSkipInterpreter);
            skipVMStubs = set.valueOf(optSkipVMStubs);

            savePerfOutput = set.valueOf(optPerfOut);
            savePerfOutputTo = set.valueOf(optPerfOutTo);
            savePerfOutputToFile = set.valueOf(optPerfOutToFile);

            savePerfBin = set.valueOf(optPerfBin);
            savePerfBinTo = set.valueOf(optPerfBinTo);
            savePerfBinFile = set.valueOf(optPerfBinToFile);

            saveLog = set.valueOf(optSaveLog);
            saveLogTo = set.valueOf(optSaveLogTo);
            saveLogToFile = set.valueOf(optSaveLogToFile);

            intelSyntax = set.valueOf(optIntelSyntax);
            printCompilationInfo = set.valueOf(optPrintCompilationInfo);
            drawIntraJumps = set.valueOf(optDrawInterJumps);
            drawInterJumps = set.valueOf(optDrawIntraJumps);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    protected abstract void addMyOptions(OptionParser parser);

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        if (!skipAssembly) {
            Collection<String> opts = new ArrayList<>();
            opts.addAll(Arrays.asList(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+LogCompilation",
                "-XX:LogFile=" + hsLog.getAbsolutePath(),
                "-XX:+PrintAssembly"));

            if (!skipInterpreter) {
                opts.add("-XX:+PrintInterpreter");
            }
            if (!skipVMStubs) {
                opts.add("-XX:+PrintNMethods");
                opts.add("-XX:+PrintNativeNMethods");
                opts.add("-XX:+PrintSignatureHandlers");
                opts.add("-XX:+PrintAdapterHandlers");
                opts.add("-XX:+PrintStubCode");
            }
            if (printCompilationInfo) {
                opts.add("-XX:+PrintCompilation");
                opts.add("-XX:+PrintInlining");
                opts.add("-XX:+TraceClassLoading");
            }
            if (intelSyntax) {
                opts.add("-XX:PrintAssemblyOptions=intel");
            }
            return opts;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        PerfResult result = processAssembly(br, stdOut, stdErr);

        // we know these are not needed anymore, proactively delete
        hsLog.delete();
        perfBinData.delete();
        perfParsedData.delete();

        return Collections.singleton(result);
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    /**
     * Parse profiler events from binary to text form.
     */
    protected abstract void parseEvents();

    /**
     * Read parsed events.
     *
     * @param skipMs Milliseconds to skip.
     * @param lenMs Milliseconds to capture after skip
     * @return Events.
     */
    protected abstract PerfEvents readEvents(double skipMs, double lenMs);

    /**
     * Get perf binary data extension (optional).
     *
     * @return Extension.
     */
    protected abstract String perfBinaryExtension();

    private PerfResult processAssembly(BenchmarkResult br, File stdOut, File stdErr) {
        /**
         * 1. Parse binary events.
         */

        parseEvents();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        /**
         * 2. Read out PrintAssembly output
         */

        Assembly assembly = readAssembly(hsLog.file());
        if (assembly.size() > 0) {
            pw.printf("PrintAssembly processed: %d total address lines.%n", assembly.size());
        } else if (skipAssembly) {
            pw.println();
            pw.println("PrintAssembly skipped, Java methods are not resolved.");
            pw.println();
        } else {
            pw.println();
            pw.println("ERROR: No address lines detected in assembly capture, make sure your JDK is PrintAssembly-enabled:\n    https://wiki.openjdk.java.net/display/HotSpot/PrintAssembly");
            pw.println();
        }

        /**
         * 3. Read out perf output
         */

        long skipMs;
        if (delayMsec == -1) { // not set
            skipMs = ProfilerUtils.warmupDelayMs(br);
        } else {
            skipMs = delayMsec;
        }

        double lenMs;
        if (lengthMsec == -1) { // not set
            lenMs = ProfilerUtils.measuredTimeMs(br);
        } else {
            lenMs = lengthMsec;
        }

        final PerfEvents events = readEvents(skipMs, lenMs);

        if (!events.isEmpty()) {
            pw.printf("Perf output processed (skipped %.3f seconds):%n", skipMs / 1000D);
            int cnt = 1;
            for (String event : this.events) {
                pw.printf(" Column %d: %s (%d events)%n", cnt, event, events.get(event).size());
                cnt++;
            }
            pw.println();
        } else {
            pw.println();
            pw.println("ERROR: No perf data, make sure \"perf stat echo 1\" is indeed working;\n " +
                "or the collection delay is not running past the benchmark time.");
            pw.println();
        }

        /**
         * 4. Figure out code regions
         */

        final List<Region> regions = makeRegions(assembly, events);

        /**
         * 5. Figure out interesting regions, and print them out.
         * We would sort the regions by the hotness of the first (main) event type.
         */

        final String mainEvent = this.events.get(0);

        Collections.sort(regions, new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return Long.valueOf(o2.getEventCount(events, mainEvent)).
                    compareTo(o1.getEventCount(events, mainEvent));
            }
        });

        long threshold = (long) (regionRateThreshold * events.getTotalEvents(mainEvent));

        boolean headerPrinted = false;

        int cnt = 1;
        for (Region r : regions) {
            if (r.getEventCount(events, mainEvent) > threshold) {
                if (!headerPrinted) {
                    pw.printf("Hottest code regions (>%.2f%% \"%s\" events):%n%n", regionRateThreshold * 100, mainEvent);
                    headerPrinted = true;
                }

                printDottedLine(pw, "Hottest Region " + cnt);
                pw.printf("%s, %s (%d bytes) %n%n", r.desc().source(), r.desc().name(), r.end - r.begin);
                r.printCode(pw, events);

                printDottedLine(pw);
                for (String event : this.events) {
                    printLine(pw, events, event, r.getEventCount(events, event));
                }
                pw.println("<total for region " + cnt + ">");
                pw.println();
                cnt++;
            }
        }

        if (!headerPrinted) {
            pw.printf("WARNING: No hottest code region above the threshold (%.2f%%) for disassembly.%n", regionRateThreshold * 100);
            pw.println("Use \"hotThreshold\" profiler option to lower the filter threshold.");
            pw.println();
        }

        int lenSource = 0;
        for (Region r : regions) {
            lenSource = Math.max(lenSource, r.desc().source().length());
        }

        /**
         * 6. Print out the hottest regions
         */
        {
            Multiset<String> total = new HashMultiset<>();
            Multiset<String> other = new HashMultiset<>();

            printDottedLine(pw, "Hottest Regions");
            int shown = 0;
            for (Region r : regions) {
                if (shown++ < regionShowTop) {
                    for (String event : this.events) {
                        printLine(pw, events, event, r.getEventCount(events, event));
                    }
                    pw.printf("%" + lenSource + "s  %s (%d bytes) %n", r.desc().source(), r.desc().name(), r.end - r.begin);
                } else {
                    for (String event : this.events) {
                        other.add(event, r.getEventCount(events, event));
                    }
                }
                for (String event : this.events) {
                    total.add(event, r.getEventCount(events, event));
                }
            }

            if (regions.size() - regionShowTop > 0) {
                for (String event : this.events) {
                    printLine(pw, events, event, other.count(event));
                }
                pw.println("<...other " + (regions.size() - regionShowTop) + " warm regions...>");
            }
            printDottedLine(pw);

            for (String event : this.events) {
                printLine(pw, events, event, total.count(event));
            }
            pw.println("<totals>");
            pw.println();
        }

        final Map<String, Multiset<String>> methodsByType = new HashMap<>();
        final Map<String, Multiset<MethodDesc>> methods = new HashMap<>();

        for (String event : this.events) {
            methodsByType.put(event, new HashMultiset<String>());
            methods.put(event, new HashMultiset<MethodDesc>());
        }

        for (Region r : regions) {
            for (String event : this.events) {
                long count = r.getEventCount(events, event);
                methods.get(event).add(r.desc(), count);
                methodsByType.get(event).add(r.desc().source(), count);
            }
        }

        /**
         * Print out hottest methods
         */
        {
            printDottedLine(pw, "Hottest Methods (after inlining)");

            Multiset<String> total = new HashMultiset<>();
            Multiset<String> other = new HashMultiset<>();

            int shownMethods = 0;
            List<MethodDesc> top = Multisets.sortedDesc(methods.get(mainEvent));
            for (MethodDesc m : top) {
                if (shownMethods++ < regionShowTop) {
                    for (String event : this.events) {
                        printLine(pw, events, event, methods.get(event).count(m));
                    }
                    pw.printf("%" + lenSource + "s  %s %n", m.source(), m.name());
                } else {
                    for (String event : this.events) {
                        other.add(event, methods.get(event).count(m));
                    }
                }
                for (String event : this.events) {
                    total.add(event, methods.get(event).count(m));
                }
            }

            if (top.size() - regionShowTop > 0) {
                for (String event : this.events) {
                    printLine(pw, events, event, other.count(event));
                }
                pw.println("<...other " + (top.size() - regionShowTop) + " warm methods...>");
            }
            printDottedLine(pw);

            for (String event : this.events) {
                printLine(pw, events, event, total.count(event));
            }
            pw.println("<totals>");
            pw.println();
        }

        /**
         * Print hot methods distribution
         */
        {
            printDottedLine(pw, "Distribution by Source");

            for (String m : Multisets.sortedDesc(methodsByType.get(mainEvent))) {
                for (String event : this.events) {
                    printLine(pw, events, event, methodsByType.get(event).count(m));
                }
                pw.printf("%" + lenSource + "s%n", m);
            }

            printDottedLine(pw);

            for (String event : this.events) {
                printLine(pw, events, event, methodsByType.get(event).size());
            }

            pw.println("<totals>");
            pw.println();

        }

        /**
         * Final checks on assembly:
         */

        {
            Set<Long> addrHistory = new HashSet<>();
            for (Long addr : assembly.addressMap.keySet()) {
                if (!addrHistory.add(addr)) {
                    pw.println("WARNING: Duplicate instruction addresses detected. This is probably due to compiler reusing\n " +
                        "the code arena for the new generated code. We can not differentiate between methods sharing\n" +
                        "the same addresses, and therefore the profile might be wrong. Increasing generated code\n" +
                        "storage might help.");
                }
            }
        }

        {
            int sum = 0;
            for (Long v : events.totalCounts.values()) {
                sum += v;
            }

            if (sum < 1000) {
                pw.println("WARNING: The perf event count is suspiciously low (" + sum + "). The performance data might be\n" +
                    "inaccurate or misleading. Try to do the profiling again, or tune up the sampling frequency.");
            }
        }

        /**
         * Print perf output, if needed:
         */
        if (savePerfOutput) {
            String target = (savePerfOutputToFile == null) ?
                savePerfOutputTo + "/" + br.getParams().id() + ".perf" :
                savePerfOutputToFile;
            try {
                FileUtils.copy(perfParsedData.getAbsolutePath(), target);
                pw.println("Perf output saved to " + target);
            } catch (IOException e) {
                pw.println("Unable to save perf output to " + target);
            }
        }

        /**
         * Print binary perf output, if needed:
         */
        if (savePerfBin) {
            String target = (savePerfBinFile == null) ?
                savePerfBinTo + "/" + br.getParams().id() + perfBinaryExtension() :
                savePerfBinFile;
            try {
                FileUtils.copy(perfBinData.getAbsolutePath(), target);
                pw.println("Perf binary output saved to " + target);
            } catch (IOException e) {
                pw.println("Unable to save perf binary output to " + target);
            }
        }

        /**
         * Print annotated assembly, if needed:
         */
        if (saveLog) {
            String target = (saveLogToFile == null) ?
                saveLogTo + "/" + br.getParams().id() + ".log" :
                saveLogToFile;
            try (FileOutputStream asm = new FileOutputStream(target);
                 PrintWriter pwAsm = new PrintWriter(asm)) {
                for (ASMLine line : assembly.lines) {
                    for (String event : this.events) {
                        long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                        printLine(pwAsm, events, event, count);
                    }
                    pwAsm.println(line.code);
                }
                pw.println("Perf-annotated Hotspot log is saved to " + target);
            } catch (IOException e) {
                pw.println("Unable to save Hotspot log to " + target);
            }
        }

        pw.flush();
        pw.close();

        return new PerfResult(sw.toString());
    }

    private static void printLine(PrintWriter pw, PerfEvents events, String event, long count) {
        if (count > 0) {
            pw.printf("%6.2f%%  ", 100.0 * count / events.getTotalEvents(event));
        } else {
            pw.printf("%9s", "");
        }
    }

    void printDottedLine(PrintWriter pw) {
        printDottedLine(pw, null);
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
        List<Region> regions = new ArrayList<>();

        SortedSet<Long> allAddrs = events.getAllAddresses();
        for (Interval intv : figureHotIntervals(allAddrs)) {
            SortedSet<Long> eventfulAddrs = allAddrs.subSet(intv.src, intv.dst + 1);

            List<ASMLine> regionLines = asms.getLines(intv.src, intv.dst, printMargin);

            if (!regionLines.isEmpty()) {
                // has some associated assembly

                // TODO: Should scan and split regions for multiple descs?
                MethodDesc desc = asms.getMethod(intv.src);
                if (desc == null) {
                    desc = MethodDesc.unknown();
                }

                regions.add(new GeneratedRegion(this.events, asms, desc, intv.src, intv.dst,
                        regionLines, eventfulAddrs, regionTooBigThreshold, drawIntraJumps, drawInterJumps));
            } else {
                // has no assembly, should be a native region

                // TODO: Should scan and split regions for multiple descs?
                MethodDesc desc = events.getMethod(intv.src);
                if (desc == null) {
                    desc = MethodDesc.unknown();
                }

                regions.add(new NativeRegion(desc, intv.src, intv.dst, eventfulAddrs));
            }
        }

        return regions;
    }


    private List<Interval> figureHotIntervals(SortedSet<Long> addrs) {
        if (addrs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Interval> intervals = new ArrayList<>();
        long begAddr = addrs.first();
        long lastAddr = addrs.first();
        for (long addr : addrs) {
            if (addr - lastAddr > mergeMargin) {
                intervals.add(new Interval(begAddr, lastAddr));
                begAddr = addr;
            }
            lastAddr = addr;
        }

        if (begAddr != lastAddr) {
            intervals.add(new Interval(begAddr, lastAddr));
        }

        return intervals;
    }

    Collection<Collection<String>> splitAssembly(File stdOut) {
        try (FileReader in = new FileReader(stdOut);
             BufferedReader br = new BufferedReader(in)) {
            Multimap<Long, String> writerToLines = new HashMultimap<>();
            Long writerId = -1L;

            Pattern pWriterThread = Pattern.compile("(.*)<writer thread='(.*)'>(.*)");
            String line;

            while ((line = br.readLine()) != null) {
                // Parse the writer threads IDs:
                //    <writer thread='140703710570240'/>
                if (line.contains("<writer thread=")) {
                    Matcher m = pWriterThread.matcher(line);
                    if (m.matches()) {
                        try {
                            writerId = Long.valueOf(m.group(2));
                        } catch (NumberFormatException e) {
                            // something is wrong, try to recover
                        }
                    }
                    continue;
                }
                writerToLines.put(writerId, line);
            }

            Collection<Collection<String>> r = new ArrayList<>();
            for (long id : writerToLines.keys()) {
                r.add(writerToLines.get(id));
            }
            return r;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    Assembly readAssembly(File stdOut) {
        List<ASMLine> lines = new ArrayList<>();
        SortedMap<Long, Integer> addressMap = new TreeMap<>();

        IntervalMap<MethodDesc> stubs       = new IntervalMap<>();
        IntervalMap<MethodDesc> javaMethods = new IntervalMap<>();

        Set<Interval> intervals = new HashSet<>();

        for (Collection<String> cs : splitAssembly(stdOut)) {
            String prevLine = "";
            for (String line : cs) {
                String trim = line.trim();

                if (trim.isEmpty()) continue;
                String[] elements = trim.split(" ");

                ASMLine asmLine = new ASMLine(line);

                // Handle the most frequent case first.
                if (trim.startsWith("0x")) {
                    // Seems to be line with address.
                    try {
                        Long addr = parseAddress(elements[0]);
                        int idx = lines.size();
                        addressMap.put(addr, idx);

                        asmLine = new ASMLine(addr, line);

                        if (elements.length > 1 && (drawInterJumps || drawIntraJumps)) {
                            for (int c = 1; c < elements.length; c++) {
                                if (elements[c].startsWith("0x")) {
                                    try {
                                        Long target = parseAddress(elements[c]);
                                        intervals.add(new Interval(addr, target));
                                    } catch (NumberFormatException e) {
                                        // nope
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Nope, not the address line.
                    }
                }

                if (prevLine.contains("--------") || line.contains("StubRoutines::")) {
                    // Try parsing the interpreter/runtime stub:
                    // ----------------------------------------------------------------------
                    // invokehandle  233 invokehandle  [0x00007f631d023100, 0x00007f631d0233c0]  704 bytes
                    // StubRoutines::catch_exception [0x00007feb43fa7b27, 0x00007feb43fa7b46[ (31 bytes)

                    Pattern pattern = Pattern.compile("(.+)( +)\\[(.+), (.+)[\\]\\[](.*)");
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.matches()) {
                        Long startAddr = parseAddress(matcher.group(3));
                        Long endAddr = parseAddress(matcher.group(4));

                        if (line.contains("StubRoutines::")) {
                            stubs.add(MethodDesc.runtimeStub(matcher.group(1)), startAddr, endAddr);
                        } else {
                            stubs.add(MethodDesc.interpreter(matcher.group(1)), startAddr, endAddr);
                        }
                    }
                }

                if (line.contains("<nmethod")) {
                    // <nmethod compile_id='481' compiler='C1' level='3' entry='0x00007f26f51fb640' size='1392'
                    //   address='0x00007f26f51fb4d0' relocation_offset='296' insts_offset='368' stub_offset='976'
                    //   scopes_data_offset='1152' scopes_pcs_offset='1208' dependencies_offset='1368' nul_chk_table_offset='1376'
                    //   method='java/lang/reflect/Constructor getParameterTypes ()[Ljava/lang/Class;' bytes='11'
                    //   count='258' iicount='258' stamp='8.590'/>

                    Matcher matcher = Pattern.compile("(.*?)<nmethod (.*?)/>(.*?)").matcher(line);
                    if (matcher.matches()) {
                        String body = matcher.group(2);
                        body = body.replaceAll("='", "=");
                        String[] kvs = body.split("' ");

                        HashMap<String, String> map = new HashMap<>();
                        for (String kv : kvs) {
                            String[] pair = kv.split("=");

                            // Guard against "key=''"
                            if (pair.length == 2) {
                                map.put(pair[0], pair[1]);
                            } else {
                                map.put(pair[0], null);
                            }
                        }

                        // Record the starting address for the method
                        Long addr = parseAddress(map.get("entry"));

                        javaMethods.add(
                                MethodDesc.javaMethod(map.get("method"), map.get("compiler"), map.get("level"), map.get("compile_id")),
                                addr,
                                addr + Long.valueOf(map.get("size"))
                        );
                    }
                }

                lines.add(asmLine);

                prevLine = line;
            }
        }

        // Important to get the order right: all Java methods take precedence over interpreter/runtime stubs.
        IntervalMap<MethodDesc> methodMap = new IntervalMap<>();
        methodMap.merge(stubs);
        methodMap.merge(javaMethods);

        return new Assembly(lines, addressMap, methodMap, intervals);
    }

    private Long parseAddress(String address) {
        return Long.valueOf(address.replace("0x", "").replace(":", ""), 16);
    }

    static class PerfResult extends Result<PerfResult> {
        private static final long serialVersionUID = 6871141606856800453L;

        private final String output;

        public PerfResult(String output) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "asm", of(Double.NaN), "---", AggregationPolicy.AVG);
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
        public String extendedInfo() {
            return output;
        }
    }

    static class PerfResultAggregator implements Aggregator<PerfResult> {
        @Override
        public PerfResult aggregate(Collection<PerfResult> results) {
            String output = "";
            for (PerfResult r : results) {
                output += r.output;
            }
            return new PerfResult(output);
        }
    }

    protected static class PerfEvents {
        final Map<String, Multiset<Long>> events;
        final IntervalMap<MethodDesc> methods;
        final Map<String, Long> totalCounts;

        PerfEvents(Collection<String> tracedEvents, Map<String, Multiset<Long>> events, IntervalMap<MethodDesc> methods) {
            this.events = events;
            this.methods = methods;
            this.totalCounts = new HashMap<>();
            for (String event : tracedEvents) {
                totalCounts.put(event, events.get(event).size());
            }
        }

        public PerfEvents(Collection<String> tracedEvents) {
            this(tracedEvents, Collections.<String, Multiset<Long>>emptyMap(), new IntervalMap<MethodDesc>());
        }

        public boolean isEmpty() {
            return events.isEmpty();
        }

        public Multiset<Long> get(String event) {
            return events.get(event);
        }

        public SortedSet<Long> getAllAddresses() {
            SortedSet<Long> addrs = new TreeSet<>();
            for (Multiset<Long> e : events.values()) {
                addrs.addAll(e.keys());
            }
            return addrs;
        }

        public Long getTotalEvents(String event) {
            return totalCounts.get(event);
        }

        public MethodDesc getMethod(long addr) {
            return methods.get(addr);
        }
    }

    static class Assembly {
        final List<ASMLine> lines;
        final SortedMap<Long, Integer> addressMap;
        final IntervalMap<MethodDesc> methodMap;
        final Set<Interval> intervals;

        public Assembly(List<ASMLine> lines, SortedMap<Long, Integer> addressMap, IntervalMap<MethodDesc> methodMap, Set<Interval> intervals) {
            this.lines = lines;
            this.addressMap = addressMap;
            this.methodMap = methodMap;
            this.intervals = intervals;
        }

        public int size() {
            // We only care about the address lines.
            return addressMap.size();
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

        public MethodDesc getMethod(long addr) {
            return methodMap.get(addr);
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
        final MethodDesc method;
        final long begin;
        final long end;
        final Set<Long> eventfulAddrs;
        final Map<String, Long> eventCountCache;

        Region(MethodDesc method, long begin, long end, Set<Long> eventfulAddrs) {
            this.method = method;
            this.begin = begin;
            this.end = end;
            this.eventfulAddrs = eventfulAddrs;
            this.eventCountCache = new HashMap<>();
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

        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println("<no code>");
        }

        public MethodDesc desc() {
            return method;
        }
    }

    static class GeneratedRegion extends Region {
        final Collection<String> tracedEvents;
        final Assembly asms;
        final Collection<ASMLine> code;
        final int threshold;
        final boolean drawIntraJumps;
        final boolean drawInterJumps;

        GeneratedRegion(Collection<String> tracedEvents, Assembly asms, MethodDesc desc, long begin, long end,
                        Collection<ASMLine> code, Set<Long> eventfulAddrs,
                        int threshold, boolean drawIntraJumps, boolean drawInterJumps) {
            super(desc, begin, end, eventfulAddrs);
            this.tracedEvents = tracedEvents;
            this.asms = asms;
            this.code = code;
            this.threshold = threshold;
            this.drawIntraJumps = drawIntraJumps;
            this.drawInterJumps = drawInterJumps;
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            if (code.size() > threshold) {
                pw.printf(" <region is too big to display, has %d lines, but threshold is %d>%n", code.size(), threshold);
            } else {
                long beginLine = begin;
                long endLine = end;
                for (ASMLine line : code) {
                    Long addr = line.addr;
                    if (addr != null) {
                        beginLine = Math.min(beginLine, addr);
                        endLine = Math.max(endLine, addr);
                    }
                }

                Set<Interval> interIvs = new TreeSet<>();
                Set<Interval> intraIvs = new TreeSet<>();

                for (Interval it : asms.intervals) {
                    boolean srcInline = (beginLine < it.src && it.src < endLine);
                    boolean dstInline = (beginLine < it.dst && it.dst < endLine);
                    if (srcInline && dstInline) {
                        if (drawInterJumps) {
                            interIvs.add(it);
                        }
                    } else if (srcInline || dstInline) {
                        if (drawIntraJumps) {
                            intraIvs.add(it);
                        }
                    }
                }

                long prevAddr = 0;
                for (ASMLine line : code) {
                    for (String event : tracedEvents) {
                        long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                        printLine(pw, events, event, count);
                    }

                    long addr;
                    long evAddr;

                    if (line.addr == null) {
                        addr = prevAddr;
                        evAddr = -1;
                    } else {
                        addr = line.addr;
                        evAddr = addr;
                        prevAddr = addr;
                    }

                    for (Interval it : intraIvs) {
                        printInterval(pw, it, addr, evAddr, false);
                    }

                    for (Interval it : interIvs) {
                        printInterval(pw, it, addr, evAddr, true);
                    }

                    pw.println(line.code);
                }
            }
        }

        private void printInterval(PrintWriter pw, Interval it, long addr, long evAddr, boolean inline) {
            if (it.src < it.dst) {
                // flows downwards
                if (it.src == evAddr) {
                    pw.print("\u256d");
                } else if (it.dst == evAddr) {
                    pw.print("\u2198");
                } else if ((it.src <= addr) && (addr < it.dst)) {
                    if (inline) {
                        pw.print("\u2502");
                    } else {
                        pw.print("\u2575");
                    }
                } else {
                    pw.print(" ");
                }
            } else {
                // flows upwards
                if (it.src == evAddr) {
                    pw.print("\u2570");
                } else if (it.dst == evAddr) {
                    pw.print("\u2197");
                } else if ((it.dst <= addr) && (addr < it.src)) {
                    if (inline) {
                        pw.print("\u2502");
                    } else {
                        pw.print("\u2575");
                    }
                } else {
                    pw.print(" ");
                }
            }
        }
    }

    static class NativeRegion extends Region {

        NativeRegion(MethodDesc desc, long begin, long end, Set<Long> eventfulAddrs) {
            super(desc, begin, end, eventfulAddrs);
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println(" <no assembly is recorded, native region>");
        }
    }

    static class UnknownRegion extends Region {
        UnknownRegion() {
            super(MethodDesc.unknown(), 0L, 0L, Collections.singleton(0L));
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println(" <no assembly is recorded, unknown region>");
        }
    }

    static class MethodDesc {
        private final String name;
        private final String source;

        protected MethodDesc(String name, String source) {
            this.name = name;
            this.source = source;
        }

        public static MethodDesc unresolved() {
            return new MethodDesc("<unresolved>", "");
        }

        public static MethodDesc unknown() {
            return new MethodDesc("<unknown>", "");
        }

        public static MethodDesc kernel() {
            return new MethodDesc("<kernel>", "kernel");
        }

        public static MethodDesc interpreter(String name) {
            return new MethodDesc(name, "interpreter");
        }

        public static MethodDesc runtimeStub(String name) {
            return new MethodDesc(name, "runtime stub");
        }

        public static MethodDesc javaMethod(String name, String compiler, String level, String ver) {
            String methodName = name.replace("/", ".").replaceFirst(" ", "::").split(" ")[0];
            return new MethodDesc(
                    methodName + ", version " + ver,
                    (compiler != null ? compiler : "Unknown") + (level != null ? ", level " + level : "")
            );
        }

        public static MethodDesc nativeMethod(String symbol, String lib) {
            return new MethodDesc(symbol, lib);
        }

        public String name() {
            return name;
        }

        public String source() {
            return source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodDesc that = (MethodDesc) o;

            if (!name.equals(that.name)) return false;
            return source.equals(that.source);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + source.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodDesc{" +
                    "name='" + name + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }

}
