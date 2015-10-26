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
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    protected final String hsLog;
    protected final String perfBinData;
    protected final String perfParsedData;
    protected final OptionSet set;
    private final boolean drawIntraJumps;
    private final boolean drawInterJumps;

    protected AbstractPerfAsmProfiler(String initLine, String... events) throws ProfilerException {
        try {
            hsLog = FileUtils.tempFile("hslog").getAbsolutePath();
            perfBinData = FileUtils.tempFile("perfbin").getAbsolutePath();
            perfParsedData = FileUtils.tempFile("perfparsed").getAbsolutePath();
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
            Collection<String> opts = new ArrayList<String>();
            opts.addAll(Arrays.asList(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+LogCompilation",
                "-XX:LogFile=" + hsLog,
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
     * @param skipSec Seconds to skip.
     * @return Events.
     */
    protected abstract PerfEvents readEvents(double skipSec);

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

        Assembly assembly = readAssembly(new File(hsLog));
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

        long delayNs;
        if (delayMsec == -1) { // not set
            BenchmarkResultMetaData md = br.getMetadata();
            if (md != null) {
                // try to ask harness itself:
                delayNs = TimeUnit.MILLISECONDS.toNanos(md.getMeasurementTime() - md.getStartTime());
            } else {
                // metadata is not available, let's make a guess:
                IterationParams wp = br.getParams().getWarmup();
                delayNs = wp.getCount() * wp.getTime().convertTo(TimeUnit.NANOSECONDS)
                        + TimeUnit.SECONDS.toNanos(1); // loosely account for the JVM lag
            }
        } else {
            delayNs = TimeUnit.MILLISECONDS.toNanos(delayMsec);
        }

        double skipSec = 1.0 * delayNs / TimeUnit.SECONDS.toNanos(1);

        final PerfEvents events = readEvents(skipSec);

        if (!events.isEmpty()) {
            pw.printf("Perf output processed (skipped %.3f seconds):%n", skipSec);
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
                    pw.printf("Hottest code regions (>%.2f%% \"%s\" events):%n", regionRateThreshold * 100, mainEvent);
                    headerPrinted = true;
                }

                printDottedLine(pw, "Hottest Region " + cnt);
                pw.printf(" [0x%x:0x%x] in %s%n%n", r.begin, r.end, r.getName());
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

        /**
         * 6. Print out the hottest regions
         */
        {
            Multiset<String> total = new HashMultiset<String>();
            Multiset<String> other = new HashMultiset<String>();

            printDottedLine(pw, "Hottest Regions");
            int shown = 0;
            for (Region r : regions) {
                if (shown++ < regionShowTop) {
                    for (String event : this.events) {
                        printLine(pw, events, event, r.getEventCount(events, event));
                    }
                    pw.printf("[0x%x:0x%x] in %s%n", r.begin, r.end, r.getName());
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

        final Map<String, Multiset<String>> methodsByType = new HashMap<String, Multiset<String>>();
        for (String event : this.events) {
            methodsByType.put(event, new HashMultiset<String>());
        }

        /**
         * Print out hottest methods
         */
        {
            printDottedLine(pw, "Hottest Methods (after inlining)");

            Map<String, Multiset<String>> methods = new HashMap<String, Multiset<String>>();
            for (String event : this.events) {
                methods.put(event, new HashMultiset<String>());
            }

            for (Region r : regions) {
                for (String event : this.events) {
                    long count = r.getEventCount(events, event);
                    methods.get(event).add(r.getName(), count);
                    methodsByType.get(event).add(r.getType(), count);
                }
            }

            Multiset<String> total = new HashMultiset<String>();
            Multiset<String> other = new HashMultiset<String>();

            int shownMethods = 0;
            List<String> top = Multisets.sortedDesc(methods.get(mainEvent));
            for (String m : top) {
                if (shownMethods++ < regionShowTop) {
                    for (String event : this.events) {
                        printLine(pw, events, event, methods.get(event).count(m));
                    }
                    pw.printf("%s%n", m);
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
            printDottedLine(pw, "Distribution by Area");

            for (String m : Multisets.sortedDesc(methodsByType.get(mainEvent))) {
                for (String event : this.events) {
                    printLine(pw, events, event, methodsByType.get(event).count(m));
                }
                pw.printf("%s%n", m);
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
                FileUtils.copy(perfParsedData, target);
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
                FileUtils.copy(perfBinData, target);
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
            FileOutputStream asm;
            try {
                asm = new FileOutputStream(target);
                PrintWriter pwAsm = new PrintWriter(asm);
                for (ASMLine line : assembly.lines) {
                    for (String event : this.events) {
                        long count = (line.addr != null) ? events.get(event).count(line.addr) : 0;
                        printLine(pwAsm, events, event, count);
                    }
                    pwAsm.println(line.code);
                }
                pwAsm.flush();
                FileUtils.safelyClose(asm);

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
        List<Region> regions = new ArrayList<Region>();

        SortedSet<Long> addrs = events.getAllAddresses();

        Set<Long> eventfulAddrs = new HashSet<Long>();
        Long lastBegin = null;
        Long lastAddr = null;
        for (Long addr : addrs) {
            if (addr == 0) {
                regions.add(new UnknownRegion());
                continue;
            }

            if (lastAddr == null) {
                lastAddr = addr;
                lastBegin = addr;
            } else {
                if (addr - lastAddr > mergeMargin) {
                    List<ASMLine> regionLines = asms.getLines(lastBegin, lastAddr, printMargin);

                    long minAddr = Long.MAX_VALUE;
                    long maxAddr = Long.MIN_VALUE;
                    for (ASMLine line : regionLines) {
                        if (line.addr != null) {
                            minAddr = Math.min(minAddr, line.addr);
                            maxAddr = Math.max(maxAddr, line.addr);
                        }
                    }

                    if (!regionLines.isEmpty()) {
                        regions.add(new GeneratedRegion(this.events, asms, minAddr, maxAddr, regionLines, eventfulAddrs, regionTooBigThreshold, drawIntraJumps, drawInterJumps));
                    } else {
                        regions.add(new NativeRegion(events, lastBegin, lastAddr, eventfulAddrs));
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

    Collection<Collection<String>> splitAssembly(File stdOut) {
        FileReader in = null;
        try {
            Multimap<Long, String> writerToLines = new HashMultimap<Long, String>();
            Long writerId = -1L;

            Pattern pWriterThread = Pattern.compile("(.*)<writer thread='(.*)'>(.*)");
            String line;

            in = new FileReader(stdOut);
            BufferedReader br = new BufferedReader(in);
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

            Collection<Collection<String>> r = new ArrayList<Collection<String>>();
            for (long id : writerToLines.keys()) {
                r.add(writerToLines.get(id));
            }
            return r;
        } catch (IOException e) {
            return Collections.emptyList();
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    Assembly readAssembly(File stdOut) {
        List<ASMLine> lines = new ArrayList<ASMLine>();
        SortedMap<Long, Integer> addressMap = new TreeMap<Long, Integer>();
        SortedMap<Long, String> methodMap = new TreeMap<Long, String>();
        Set<Interval> intervals = new HashSet<Interval>();

        for (Collection<String> cs : splitAssembly(stdOut)) {
            String method = null;
            String prevLine = "";
            for (String line : cs) {
                String trim = line.trim();

                if (trim.isEmpty()) continue;
                String[] elements = trim.split(" ");

                ASMLine asmLine = new ASMLine(line);

                // Handle the most frequent case first.
                if (elements.length >= 1 && elements[0].startsWith("0x")) {
                    // Seems to be line with address.
                    try {
                        Long addr = Long.valueOf(elements[0].replace("0x", "").replace(":", ""), 16);
                        int idx = lines.size();
                        addressMap.put(addr, idx);

                        // Record the starting address for the method, if any.
                        if (method != null) {
                            methodMap.put(addr, method);
                            method = null;
                        }

                        asmLine = new ASMLine(addr, line);

                        if (drawInterJumps || drawIntraJumps) {
                            for (int c = 1; c < elements.length; c++) {
                                if (elements[c].startsWith("0x")) {
                                    try {
                                        Long target = Long.valueOf(elements[c].replace("0x", "").replace(":", ""), 16);
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
                } else if (line.contains("# {method}")) {
                    // Handle the compiled code line.
                    if (elements.length == 6) {
                        // old JDKs may print the line with 6 fields: # {method} <name> <signature> in <class>
                        method = (elements[5].replace("/", ".") + "::" + elements[2]).replace("'", "");
                    } else if (elements.length == 7) {
                        // newer JDKs always print 7 fields: # {method} <address> <name> <signature> in <class>
                        method = (elements[6].replace("/", ".") + "::" + elements[3]).replace("'", "");
                    } else {
                        // {method} line is corrupted, other writer had possibly interjected;
                        // honestly say we can't figure the method name out instead of lying.
                        method = "<name unparseable>";
                    }
                    method = method.replace("&apos;", "");
                    method = method.replace("&lt;", "<");
                    method = method.replace("&gt;", ">");
                } else if (prevLine.contains("--------")) {
                    if (line.trim().endsWith("bytes")) {
                        // Handle the VM stub/interpreter line.
                        method = "<stub: " + line.substring(0, line.indexOf("[")).trim() + ">";
                    }
                } else if (line.contains("StubRoutines::")) {
                    // Handle the VM stub/interpreter line (another format)
                    method = elements[0];
                }
                lines.add(asmLine);

                prevLine = line;
            }
        }
        return new Assembly(lines, addressMap, methodMap, intervals);
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

    static class Interval implements Comparable<Interval> {
        private final long src;
        private final long dst;

        public Interval(long src, long dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Interval interval = (Interval) o;

            if (dst != interval.dst) return false;
            if (src != interval.src) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (src ^ (src >>> 32));
            result = 31 * result + (int) (dst ^ (dst >>> 32));
            return result;
        }

        @Override
        public int compareTo(Interval o) {
            if (src < o.src) {
                return -1;
            } else if (src > o.src) {
                return 1;
            } else {
                return (dst < o.dst) ? -1 : ((dst == o.dst) ? 0 : 1);
            }
        }
    }


    protected static class PerfEvents {
        final Map<String, Multiset<Long>> events;
        final Map<Long, String> methods;
        final Map<Long, String> libs;
        final Map<String, Long> totalCounts;

        PerfEvents(Collection<String> tracedEvents, Map<String, Multiset<Long>> events, Map<Long, String> methods, Map<Long, String> libs) {
            this.events = events;
            this.methods = methods;
            this.libs = libs;
            this.totalCounts = new HashMap<String, Long>();
            for (String event : tracedEvents) {
                totalCounts.put(event, events.get(event).size());
            }
        }

        public PerfEvents(Collection<String> tracedEvents) {
            this(tracedEvents, Collections.<String, Multiset<Long>>emptyMap(), Collections.<Long, String>emptyMap(), Collections.<Long, String>emptyMap());
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
        final Set<Interval> intervals;

        public Assembly(List<ASMLine> lines, SortedMap<Long, Integer> addressMap, SortedMap<Long, String> methodMap, Set<Interval> intervals) {
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

        public String getMethod(long addr) {
            if (methodMap.containsKey(addr)) {
                return methodMap.get(addr);
            }
            SortedMap<Long, String> head = methodMap.headMap(addr);
            if (head.isEmpty()) {
                return "<unresolved>";
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
        final Set<Long> eventfulAddrs;
        final Map<String, Long> eventCountCache;

        Region(String method, long begin, long end, Set<Long> eventfulAddrs) {
            this.method = method;
            this.begin = begin;
            this.end = end;
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

        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println("<no code>");
        }

        public String getName() {
            return method;
        }

        public String getType() {
            return "<unknown>";
        }
    }

    static class GeneratedRegion extends Region {
        final Collection<String> tracedEvents;
        final Assembly asms;
        final Collection<ASMLine> code;
        final int threshold;
        final boolean drawIntraJumps;
        final boolean drawInterJumps;

        GeneratedRegion(Collection<String> tracedEvents, Assembly asms, long begin, long end,
                        Collection<ASMLine> code, Set<Long> eventfulAddrs,
                        int threshold, boolean drawIntraJumps, boolean drawInterJumps) {
            super(generateName(asms, eventfulAddrs), begin, end, eventfulAddrs);
            this.tracedEvents = tracedEvents;
            this.asms = asms;
            this.code = code;
            this.threshold = threshold;
            this.drawIntraJumps = drawIntraJumps;
            this.drawInterJumps = drawInterJumps;
        }

        static String generateName(Assembly asm, Set<Long> eventfulAddrs) {
            Set<String> methods = new HashSet<String>();
            for (Long ea : eventfulAddrs) {
                String m = asm.getMethod(ea);
                if (m != null) {
                    methods.add(m);
                }
            }
            return Utils.join(methods, "; ");
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            if (code.size() > threshold) {
                pw.printf(" <region is too big to display, has %d lines, but threshold is %d>%n", code.size(), threshold);
            } else {
                Set<Interval> interIvs = new TreeSet<Interval>();
                Set<Interval> intraIvs = new TreeSet<Interval>();

                for (Interval it : asms.intervals) {
                    boolean srcInline = (begin < it.src && it.src < end);
                    boolean dstInline = (begin < it.dst && it.dst < end);
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

        @Override
        public String getType() {
            return "<generated code>";
        }
    }

    static class NativeRegion extends Region {
        private final String lib;

        NativeRegion(PerfEvents events, long begin, long end, Set<Long> eventfulAddrs) {
            super(generateName(events, eventfulAddrs), begin, end, eventfulAddrs);
            lib = resolveLib(events, eventfulAddrs);
        }

        static String generateName(PerfEvents events, Set<Long> eventfulAddrs) {
            Set<String> methods = new HashSet<String>();
            for (Long ea : eventfulAddrs) {
                methods.add(events.methods.get(ea));
            }
            return Utils.join(methods, "; ");
        }

        static String resolveLib(PerfEvents events, Set<Long> eventfulAddrs) {
            Set<String> libs = new HashSet<String>();
            for (Long ea : eventfulAddrs) {
                libs.add(events.libs.get(ea));
            }
            return Utils.join(libs, "; ");
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println(" <no assembly is recorded, native region>");
        }

        @Override
        public String getType() {
            return "<native code in (" + lib + ")>";
        }

        @Override
        public String getName() {
            return method + " (" + lib + ")";
        }
    }

    static class UnknownRegion extends Region {
        UnknownRegion() {
            super("<unknown>", 0L, 0L, Collections.singleton(0L));
        }

        @Override
        public void printCode(PrintWriter pw, PerfEvents events) {
            pw.println(" <no assembly is recorded, unknown region>");
        }

        @Override
        public String getType() {
            return "<unknown>";
        }
    }
}
