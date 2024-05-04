/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * macOS permnorm profiler based on xctrace utility shipped with Xcode Instruments.
 * <p>
 * The profiling process consists of several steps:
 * 1) launching a program that needs to be profiled using `xctrace record` command; in case of success,
 * the output of this step is a "trace-file", which is in fact a directory containing multiple files
 * representing the recorded trace, the trace may contain multiple resulting tables, depending on the template;
 * 2) inspecting a recorded trace to check if it contains a table supported by the profiler; this information
 * could be obtained from the trace's table of contents (`xctrace export --toc`);
 * 3) extracting the table with profiling results from the trace file using xctrace export and parsing it
 * to extract individual samples.
 * <p>
 * `xctrace export` command extracts data only in XML format, thus both the table of contents and the table
 * with profiling results need to be parsed as XML documents.
 * <p>
 * This profiler currently supports only one table type: counters-profile.
 * Such tables generated by the CPU Counters instrument performing sampling either by
 * timer interrupts, or interrupts generated by a PMU counter, depending on particular configuration.
 * <p>
 * Tracing configuration, or template, is required to perform a recording.
 * It is a file that could be configured and saved using Instruments application.
 * <p>
 * CPU Counters template has no default parameters and could only be configured in the Instruments app UI.
 * With a recent Xcode versions, this issue could be overcome by building a so-called instrument package that
 * imports CPU Counters template, initializes its parameters with some values that could be statically set in a
 * package description, and defines an instrument, that xctrace can use.
 * <p>
 * One of CPU Counters template's parameters is a list of PMU events to sample. Events supported by the current CPU
 * could be extracted from KPEP database files located in {@code /usr/share/kpep} folder. Each KPEP file is a binary
 * property list file, that could be converted to XML and then parsed. File's content provides information about
 * event names, their description and various restrictions on their use.
 * <p>
 * To provide a default behavior (that does not require a user-configured template) and make profilers use a bit more
 * convinient, XCTraceNormProfiler parses KPEP database files and uses extracted info to build Instruments packages.
 * <p>
 *
 * @see XCTraceSupport#parseKpepFile(File)
 * @see XCTraceSupport#buildInstrumentsPMCSamplingPackage(File, long, Collection)
 */
public class XCTraceNormProfiler implements ExternalProfiler {
    // https://developer.apple.com/documentation/xcode-release-notes/xcode-13-release-notes#Instruments
    // Older versions support CPU Counters in some way, but are lacking handy "counters-profile" table.
    private static final int XCTRACE_VERSION_WITH_COUNTERS_PROFILE_TABLE = 13;
    // It's missing from release notes, but "--instrument" option seems to appear in Xcode 14
    private static final int XCTRACE_VERSION_WITH_INSTRUMENT_OPTIONS = 14;
    // Currently, only counters-profile table is supported
    private static final XCTraceTableHandler.ProfilingTableType SUPPORTED_TABLE_TYPE =
            XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE;

    // Set of events available for all supported CPUs. This set is not exhaustive and contains as many events as
    // it could be configured on a CPU with the least counters available (it's 4).
    // On Apple Silicon CPUs, CORE_ACTIVE_CYCLE and INST_ALL are configurable events that have their fixed analogues -
    // FIXED_CYCLES and FIXED_INSTRUCTIONS. In general, it would be better to use fixed events, but it's fine for
    // default settings consisting of only 4 events in total.
    private static final String[] DEFAULT_EVENTS = new String[]{
            "CORE_ACTIVE_CYCLE", "INST_ALL", "INST_BRANCH", "BRANCH_MISPRED_NONSPEC"
    };
    private static final String[] CYCLES_EVENT_NAMES = new String[]{
            "CORE_ACTIVE_CYCLE", "Cycles", "FIXED_CYCLES", "CPU_CLK_UNHALTED.THREAD", "CPU_CLK_UNHALTED.THREAD_P"
    };
    private static final String[] INSTRUCTIONS_EVENT_NAMES = new String[]{
            "INST_ALL", "Instructions", "FIXED_INSTRUCTIONS", "INST_RETIRED.ANY", "INST_RETIRED.ANY_P"
    };
    private static final String[] BRANCH_EVENT_NAMES = new String[]{
            "INST_BRANCH", "BR_INST_RETIRED.ALL_BRANCHES", "BR_INST_RETIRED.ALL_BRANCHES_PEBS"
    };
    private static final String[] BRANCH_MISS_EVENT_NAMES = new String[]{
            "BRANCH_MISPRED_NONSPEC", "BR_MISP_RETIRED.ALL_BRANCHES",  "BR_MISP_RETIRED.ALL_BRANCHES_PS"
    };
    private static final String CUSTOM_INSTRUMENT_NAME = "XCTraceNormProfiler";

    private final String xctracePath;
    private final String tracingTemplate;
    private final Path temporaryDirectory;
    private final TempFile outputFile;

    private final long delayMs;
    private final long lengthMs;
    private final boolean shouldFixStartTime;
    private final List<String> pmuEvents;
    private final int samplingRateMsec;
    private final File samplingPackage;
    private final XCTraceSupport.PerfEvents perfEvents;

    public XCTraceNormProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter(XCTraceNormProfiler.class.getName()));

        OptionSpec<String> templateOpt = parser.accepts("template", "Name of or path to Instruments template. " +
                        "Use `xctrace list templates` to view available templates. " +
                        "Only templates with \"CPU Counters\" instrument are supported at the moment.")
                .withOptionalArg().ofType(String.class);
        OptionSpec<Boolean> printAvailableEventsOpt = parser.accepts("listEvents",
                        "Print list of available PMU events.")
                .withOptionalArg().ofType(Boolean.class).defaultsTo(false);
        OptionSpec<String> eventsOpt = parser.accepts("events", "Comma-separated lists of PMU events to sample. " +
                        "To get a list of available events, please use \"listEvents=true\" option, " +
                        "Instruments app UI, or inspect a KPEP database file specific to your CPU.")
                .withOptionalArg().ofType(String.class).withValuesSeparatedBy(",")
                .defaultsTo(DEFAULT_EVENTS);
        OptionSpec<Integer> samplingRateOpt = parser.accepts("samplingRate", "Sampling rate, in milliseconds. " +
                        "Applied only for \"events\" sampling, when \"template\" is not specified")
                .withOptionalArg().ofType(Integer.class).defaultsTo(10);
        OptionSpec<Integer> optDelay = parser.accepts("delay",
                        "Delay collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);
        OptionSpec<Integer> optLength = parser.accepts("length",
                        "Do the collection for a given time, in milliseconds; -1 to detect automatically.")
                .withRequiredArg().ofType(Integer.class).describedAs("ms").defaultsTo(-1);
        OptionSpec<Boolean> correctOpt = parser.accepts("fixStartTime",
                        "Fix the start time by the time it took to launch.")
                .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
        OptionSpec<Boolean> validateEventsOpt = parser.accepts("validateEvents",
                        "Check if specified PMU events could be used simultaneously.")
                .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

        OptionSet options = ProfilerUtils.parseInitLine(initLine, parser);

        File kpepFile = XCTraceSupport.getKpepFilePath();
        if (!kpepFile.exists()) {
            throw new ProfilerException("KPEP database file does not exist: " + kpepFile.getAbsolutePath() +
                    ". Most likely, it means current CPU is either virtualized (we're running inside a VM), or " +
                    "not supported yet by the macOs (less likely).");
        }
        perfEvents = XCTraceSupport.parseKpepFile(kpepFile);

        if (options.valueOf(printAvailableEventsOpt)) {
            dumpSupportedEvents(perfEvents, kpepFile);
        }

        delayMs = options.valueOf(optDelay);
        lengthMs = options.valueOf(optLength);
        shouldFixStartTime = options.valueOf(correctOpt);
        samplingRateMsec = options.valueOf(samplingRateOpt);

        if (options.hasArgument(templateOpt) && options.hasArgument(eventsOpt)) {
            throw new ProfilerException(
                    "Please use either \"template\", or \"events\" option, but not both simultaneously.");
        }

        if (options.hasArgument(templateOpt)) {
            xctracePath = XCTraceSupport.getXCTracePath(XCTRACE_VERSION_WITH_COUNTERS_PROFILE_TABLE);
            tracingTemplate = options.valueOf(templateOpt);
            pmuEvents = Collections.emptyList();
            samplingPackage = null;
        } else {
            xctracePath = XCTraceSupport.getXCTracePath(XCTRACE_VERSION_WITH_INSTRUMENT_OPTIONS);
            tracingTemplate = null;
            List<String> userEvents = extractSupportedEvents(options.valuesOf(eventsOpt), perfEvents);
            if (options.valueOf(validateEventsOpt)) {
                checkEventsCouldBeScheduled(userEvents, perfEvents);
            }
            if (userEvents.isEmpty()) {
                throw new ProfilerException("No supported events.");
            }
            pmuEvents = userEvents;
            samplingPackage = buildSamplingPackage();
        }

        temporaryDirectory = XCTraceSupport.createTemporaryDirectoryName();
        try {
            outputFile = FileUtils.weakTempFile("xctrace-out.xml");
        } catch (IOException e) {
            throw new ProfilerException(e);
        }
    }

    private static void dumpSupportedEvents(
            XCTraceSupport.PerfEvents perfEvents, File kpepFile) throws ProfilerException {
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Supported PMU events (parsed from ")
                .append(kpepFile.getAbsolutePath())
                .append("):\n");
        perfEvents.getAliases().stream().sorted().forEachOrdered(alias ->
                helpMessage.append(alias).append("\tAlias to ")
                        .append(perfEvents.getEvent(alias).getName()).append('\n'));
        perfEvents.getAllEvents().stream()
                .sorted(Comparator.comparing(XCTraceSupport.PerfEventInfo::getName))
                .forEachOrdered(event -> helpMessage.append(event.getName()).append('\t')
                        .append(event.getDescription()).append('\n'));
        throw new ProfilerException(helpMessage.toString());
    }

    /**
     * Filters out all unsupported events {@code userEvents} and maps names of what remains, if needed.
     * <p>
     * With unsupported events, everything is straightforward: such events should be filtered out.
     * However, things are a bit convoluted when it comes to supported events.
     * An event might be a regular event, or an alias to a regular event.
     * A regular event may also have an associated fallback event.
     * <p>
     * Aliases provide a name consistent across all supported CPUs (like {@code INST_ALL}) and simply
     * refer to one of other events.
     * A regular event may not be supported, and in that case it has an associated fallback event.
     * <p>
     * The mapping is performed in the following way:
     * <li>try to use aliases when they are specified;</li>
     * <li>for aliases and unsupported events, find an event that will be actually sampled and make sure it not already
     * selected;</li>
     * <li>if a name (not its alias) of unsupported event was specified, map it to a fallback event's name.</li>
     */
    private static List<String> extractSupportedEvents(List<String> userEvents, XCTraceSupport.PerfEvents perfEvents) {
        List<String> supportedEvents = new ArrayList<>();
        Set<String> underlyingEvents = new HashSet<>();

        for (String eventName : userEvents) {
            XCTraceSupport.PerfEventInfo event = perfEvents.getEvent(eventName);
            if (event == null) {
                // Event not found in the database
                continue;
            }

            boolean isAlias = !event.getName().equals(eventName);
            // For some reason, fixed counters sampling does not work on Intel-based devices.
            // Instead, Instruments app uses a fallback event.
            if (event.isFixed() && perfEvents.getArchitecture() == XCTraceSupport.CpuArch.X86_64) {
                if (event.getFallbackEvent() == null) {
                    // There's no fallback event, looks like an error, let's skip it.
                    continue;
                }
                event = perfEvents.getEvent(event.getFallbackEvent());
            }
            if (underlyingEvents.add(event.getName())) {
                // Prefer aliases over resolved event names as xctrace/Instruments
                // can handle them correctly on its own.
                // If the eventName is not an alias, pick up the name from the event object
                // as it might be a fallback event chosen in the previous step.
                supportedEvents.add(isAlias ? eventName : event.getName());
            }
        }

        return supportedEvents;
    }

    /**
     * Checks if selected events could be simultaneously scheduled for sampling.
     * <p>
     * Unlike, Linux perf_events, Instruments and xctrace do not support events multiplexing.
     * Xctrace will crash if a user specified more events than counters available on CPU,
     * or some events are constrained with respect to counters they could be scheduled to
     * and corresponding counters are unavailable (or will be occupied to count other events).
     * <p>
     * This function attempts to check if all selected events could be scheduled simultaneously.
     */
    private static void checkEventsCouldBeScheduled(List<String> events, XCTraceSupport.PerfEvents perfEvents)
            throws ProfilerException {
        // In general, testing if all events could be scheduled on CPU counter w.r.t. counter constrains
        // is equivalent to finding a maximum matching in a bipartite graph (where one set of nodes are events
        // and another set of nodes are counters; and there's an edge
        // if an event could be scheduled to one of the counters).
        // It's unclear how xctrace/Instruments check these constraints,
        // but it seems like instead of finding a maximum bipartite matching, we can greedily assign events to
        // counters starting from more constrained events (events that could be scheduled on a smaller counters subset)
        // until either all events assigned, or an event that could not be assigned is found.
        // Such an approach should work as long as there are no events having overlapping counters constraints of the
        // same size (like, event A could be scheduled on counter {1, 2, 3}, event B on {2, 3, 4}
        // and event C on {3, 4, 5}). Luckily, there are no such events in /usr/share/kpep.
        // And, well, perf_events is doing something similar when scheduling events within a group.
        long availableCountersMask = perfEvents.getConfigurableCountersMask() | perfEvents.getFixedCountersMask();
        List<Map.Entry<String, XCTraceSupport.PerfEventInfo>> eventsInfo = events.stream()
                // Map a name to an event (additionally, choosing a fallback event when needed)
                .collect(Collectors.toMap(Function.identity(), event -> {
                    XCTraceSupport.PerfEventInfo eventInfo = perfEvents.getEvent(event);
                    if (eventInfo.isFixed() && perfEvents.getArchitecture() == XCTraceSupport.CpuArch.X86_64) {
                        // Fixed counters don't work on Intel-based hosts, use a fallback event instead.
                        return perfEvents.getEvent(eventInfo.getFallbackEvent());
                    }
                    return eventInfo;
                }))
                .entrySet()
                .stream()
                // Sort by the number of set bits in the mask to process more constrained events first
                .sorted(Comparator.comparingInt(evt -> Long.bitCount(evt.getValue().getCounterMask())))
                .collect(Collectors.toList());

        for (Map.Entry<String, XCTraceSupport.PerfEventInfo> eventInfo : eventsInfo) {
            String requestedName = eventInfo.getKey();
            XCTraceSupport.PerfEventInfo event = eventInfo.getValue();

            long eventMask = event.getCounterMask();
            if ((availableCountersMask & eventMask) == 0) {
                throw new ProfilerException("Event " + requestedName + " (" + event.getName() +
                        ") could not be used with other selected events due to performance counters constraints. " +
                        "Consider configuring a template using the " +
                        "Instruments app to check which events could be used simultaneously.");
            }
            // Extract a mask with a single PMC ...
            long counterMask = Long.lowestOneBit(availableCountersMask & eventMask);
            // ... and claim that counter by clearing the corresponding bit in the mask
            availableCountersMask = availableCountersMask & ~counterMask;
        }
    }

    /**
     * Builds an Instruments package that uses {@code CPU Counters} template to sample PMU counters.
     * The package defines a custom instrument with a {@link XCTraceNormProfiler#CUSTOM_INSTRUMENT_NAME} name.
     * The package uses {@link XCTraceNormProfiler#samplingRateMsec} and {@link XCTraceNormProfiler#pmuEvents}
     * as parameters.
     * <p>
     * Generated packages are cached in {@code ~/Libraries/Caches/org.openjdk.jmh/} directory to avoid building
     * same packages over and over again.
     * If {@code ~/Libraries/Caches} does not exist, the package will be saved in {@code user.dir} folder.
     *
     * @see <a href="https://help.apple.com/instruments/developer/mac/current/#/devcd5016d31">Instruments Developer Help</a>
     */
    private File buildSamplingPackage() throws ProfilerException {
        // ~/Library/Caches is a place where an app should cache whatever it needs,
        // according to the File System Programming Guide
        // (File System Basics / About macOS File System / The Library Directory Stores App-Specific Files):
        // https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/FileSystemOverview/FileSystemOverview.html#//apple_ref/doc/uid/TP40010672-CH2-SW1
        File cacheDirRoot = new File(System.getProperty("user.home"), "Library/Caches");
        File cacheDir;
        if (!cacheDirRoot.exists()) {
            // Let's place it in the CWD if Caches dir does not exist.
            cacheDir = new File(System.getProperty("user.dir"));
        } else {
            cacheDir = new File(cacheDirRoot, "org.openjdk.jmh");
            cacheDir.mkdirs();
        }

        String digest = XCTraceSupport.generateInstrumentsPackageDigest(samplingRateMsec, pmuEvents);
        File pkg = new File(cacheDir, "xctracenorm-" + digest + ".pkg");
        if (pkg.exists()) {
            return pkg;
        }
        // Write to a temp file first, to ensure there will be no broken file in case of an error ...
        File tempPkgFile = new File(cacheDir, "xctracenorm-temp-" + System.currentTimeMillis() + ".pkg");
        XCTraceSupport.buildInstrumentsPMCSamplingPackage(tempPkgFile, samplingRateMsec, pmuEvents);
        // ... and if it was generated successfully, let's rename it.
        if (!tempPkgFile.renameTo(pkg)) {
            throw new ProfilerException("Failed to rename a package file from " + tempPkgFile.getAbsolutePath() +
                    " to " + pkg.getAbsolutePath());
        }
        return pkg;
    }

    private static XCTraceTableHandler.XCTraceTableDesc findTableDescription(XCTraceTableOfContentsHandler tocHandler) {
        XCTraceTableHandler.XCTraceTableDesc tableDesc = tocHandler.getSupportedTables()
                .stream()
                .filter(t -> t.getTableType() == SUPPORTED_TABLE_TYPE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Table \"" + SUPPORTED_TABLE_TYPE.tableName +
                        "\" was not found in the trace results."));
        if (tableDesc.getPmcEvents().isEmpty() && tableDesc.getTriggerType() == XCTraceTableHandler.TriggerType.TIME) {
            throw new IllegalStateException("Results does not contain any events.");
        }
        return tableDesc;
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        if (samplingPackage != null) {
            return XCTraceSupport.recordWithPackageCommandPrefix(
                    xctracePath, temporaryDirectory.toAbsolutePath().toString(), samplingPackage, CUSTOM_INSTRUMENT_NAME);
        } else {
            return XCTraceSupport.recordCommandPrefix(xctracePath, temporaryDirectory.toAbsolutePath().toString(),
                    tracingTemplate);
        }
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        if (!temporaryDirectory.toFile().isDirectory() && !temporaryDirectory.toFile().mkdirs()) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        try {
            return processResults(br);
        } finally {
            XCTraceSupport.removeDirectory(temporaryDirectory);
        }
    }

    private Collection<? extends Result> processResults(BenchmarkResult br) {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md == null) {
            return Collections.emptyList();
        }
        long measurementsDurationMs = md.getStopTime() - md.getMeasurementTime();
        if (measurementsDurationMs == 0L) {
            return Collections.emptyList();
        }
        double opsThroughput = md.getMeasurementOps() / (double) measurementsDurationMs;

        // Find the resulting file and extract metadata from it
        Path traceFile = XCTraceSupport.findTraceFile(temporaryDirectory);
        XCTraceSupport.exportTableOfContents(xctracePath, traceFile.toAbsolutePath().toString(),
                outputFile.getAbsolutePath());

        XCTraceTableOfContentsHandler tocHandler = new XCTraceTableOfContentsHandler();
        tocHandler.parse(outputFile.file());
        // Get info about a table with profiling results
        XCTraceTableHandler.XCTraceTableDesc tableDesc = findTableDescription(tocHandler);
        // Extract profiling results
        XCTraceSupport.exportTable(xctracePath, traceFile.toAbsolutePath().toString(), outputFile.getAbsolutePath(),
                SUPPORTED_TABLE_TYPE);

        // Please refer to XCTraceAsmProfiler::readEvents for detailed explanation,
        // but briefly, ProfilerUtils::measurementDelayMs uses the time when a fork was started,
        // and it's not the actual start time.
        // The actual start time is the time xctrace was launched (tocHandler.getRecordStartMs),
        // and we're correcting measurement delay by the difference between these two timestamps.
        long timeCorrectionMs = 0;
        if (shouldFixStartTime) {
            timeCorrectionMs = tocHandler.getRecordStartMs() - md.getStartTime();
        }
        long skipMs = delayMs;
        if (skipMs == -1L) {
            skipMs = ProfilerUtils.measurementDelayMs(br);
        }
        skipMs -= timeCorrectionMs;
        long durationMs = lengthMs;
        if (durationMs == -1L) {
            durationMs = ProfilerUtils.measuredTimeMs(br);
        }

        long skipNs = skipMs * 1_000_000;
        long durationNs = durationMs * 1_000_000;

        AggregatedEvents aggregator = new AggregatedEvents(tableDesc);
        new XCTraceTableProfileHandler(SUPPORTED_TABLE_TYPE, sample -> {
            if (sample.getTimeFromStartNs() <= skipNs || sample.getTimeFromStartNs() > skipNs + durationNs) {
                return;
            }

            aggregator.add(sample);
        }).parse(outputFile.file());

        if (aggregator.getEventsCount() == 0) {
            return Collections.emptyList();
        }

        Collection<Result<?>> results = new ArrayList<>();
        computeAggregates(results, aggregator);
        aggregator.normalizeByThroughput(opsThroughput);

        for (int i = 0; i < tableDesc.getPmcEvents().size(); i++) {
            String event = tableDesc.getPmcEvents().get(i);
            results.add(new ScalarResult(event, aggregator.getEventValues()[i],
                    "#/op", AggregationPolicy.AVG));
        }
        if (tableDesc.getTriggerType() == XCTraceTableHandler.TriggerType.PMI) {
            results.add(new ScalarResult(tableDesc.triggerEvent(),
                    aggregator.getEventValues()[aggregator.getEventValues().length - 1],
                    "#/op", AggregationPolicy.AVG));
        }
        return results;
    }

    private void computeAggregates(Collection<Result<?>> results, AggregatedEvents aggregator) {
        computeCommonMetrics(results, aggregator);

        if (perfEvents.getArchitecture() == XCTraceSupport.CpuArch.AARCH64) {
            computeAppleSiliconArm64InstDensityMetrics(results, aggregator);
        }
    }

    private static void computeCommonMetrics(Collection<Result<?>> results, AggregatedEvents aggregator) {
        CounterValue cycles = aggregator.getAnyOfOrNull(CYCLES_EVENT_NAMES);
        CounterValue insts = aggregator.getAnyOfOrNull(INSTRUCTIONS_EVENT_NAMES);

        if (cycles != null && cycles.getValue() != 0D && insts != null && insts.getValue() != 0D) {
            results.add(new ScalarResult("CPI", cycles.getValue() / insts.getValue(),
                    cycles.getName() + "/" + insts.getName(), AggregationPolicy.AVG));
            results.add(new ScalarResult("IPC", insts.getValue() / cycles.getValue(),
                    insts.getName() + "/" + cycles.getName(), AggregationPolicy.AVG));
        }

        CounterValue branches = aggregator.getAnyOfOrNull(BRANCH_EVENT_NAMES);
        CounterValue missedBranches = aggregator.getAnyOfOrNull(BRANCH_MISS_EVENT_NAMES);
        if (branches != null && branches.getValue() != 0D && missedBranches != null) {
            results.add(new ScalarResult("Branch miss ratio", missedBranches.getValue() / branches.getValue(),
                    missedBranches.getName() + "/" + branches.getName(), AggregationPolicy.AVG));
        }
    }

    // Compute instructions density metrics (defined in Apple Silicon CPU Optimization Guide,
    // https://developer.apple.com/documentation/apple-silicon/cpu-optimization-guide).
    private static void computeAppleSiliconArm64InstDensityMetrics(Collection<Result<?>> results, AggregatedEvents aggregator) {
        CounterValue insts = aggregator.getAnyOfOrNull(INSTRUCTIONS_EVENT_NAMES);
        if (insts == null) {
            return;
        }
        for (String eventName : aggregator.getEventNames()) {
            if (!eventName.startsWith("INST_") || eventName.equals("INST_ALL")) {
                continue;
            }
            Double value = aggregator.getCountOrNull(eventName);
            if (value == null || value == 0D) {
                continue;
            }

            results.add(new ScalarResult(eventName + " density (of instructions)", value / insts.getValue(),
                    eventName + "/" + insts.getName(), AggregationPolicy.AVG));
        }
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
        return "macOS xctrace (Instruments) PMU counter statistics, normalized by operation count";
    }

    private static class AggregatedEvents {
        private final List<String> eventNames;
        private final double[] eventValues;
        private long eventsCount = 0;

        private long minTimestampMs = Long.MAX_VALUE;
        private long maxTimestampMs = Long.MIN_VALUE;

        AggregatedEvents(XCTraceTableHandler.XCTraceTableDesc tableDesc) {
            List<String> names = new ArrayList<>(tableDesc.getPmcEvents());
            names.add(tableDesc.triggerEvent());
            eventNames = Collections.unmodifiableList(names);
            eventValues = new double[getEventNames().size()];
        }

        void add(XCTraceTableProfileHandler.XCTraceSample sample) {
            long[] counters = sample.getPmcValues();
            for (int i = 0; i < counters.length; i++) {
                eventValues[i] += counters[i];
            }
            eventValues[eventValues.length - 1] = sample.getWeight();
            minTimestampMs = Math.min(minTimestampMs, sample.getTimeFromStartNs());
            maxTimestampMs = Math.max(maxTimestampMs, sample.getTimeFromStartNs());
            eventsCount += 1;
        }

        void normalizeByThroughput(double throughput) {
            if (maxTimestampMs == minTimestampMs) {
                throw new IllegalStateException("Min and max timestamps are the same.");
            }
            double timeSpanMs = (maxTimestampMs - minTimestampMs) / 1e6;
            for (int i = 0; i < getEventValues().length; i++) {
                eventValues[i] = eventValues[i] / timeSpanMs / throughput;
            }
        }

        CounterValue getAnyOfOrNull(String... keys) {
            for (String key : keys) {
                Double value = getCountOrNull(key);
                if (value != null) {
                    return new CounterValue(key, value);
                }
            }
            return null;
        }

        Double getCountOrNull(String event) {
            int idx = eventNames.indexOf(event);
            if (idx == -1) return null;
            return eventValues[idx];
        }

        List<String> getEventNames() {
            return eventNames;
        }

        double[] getEventValues() {
            return eventValues;
        }

        long getEventsCount() {
            return eventsCount;
        }
    }

    private static class CounterValue {
        private final String name;
        private final double value;

        CounterValue(String name, double value) {
            this.name = name;
            this.value = value;
        }

        double getValue() {
            return value;
        }

        String getName() {
            return name;
        }
    }
}