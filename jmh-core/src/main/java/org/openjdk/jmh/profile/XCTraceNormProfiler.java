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

import joptsimple.ArgumentAcceptingOptionSpec;
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
import java.util.stream.Collectors;

// TODO: add doc
public class XCTraceNormProfiler implements ExternalProfiler {
    // https://developer.apple.com/documentation/xcode-release-notes/xcode-13-release-notes#Instruments
    // Older versions support CPU Counters in some way, but are lacking handy "counters-profile" table.
    private static final int XCTRACE_VERSION_WITH_COUNTERS_PROFILE_TABLE = 13;
    // It's missing from release notes, but "--instrument" option seems to appear in Xcode 14
    private static final int XCTRACE_VERSION_WITH_INSTRUMENT_OPTIONS = 14;
    private static final XCTraceTableHandler.ProfilingTableType SUPPORTED_TABLE_TYPE =
            XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE;

    private static final String[] CYCLES_EVENT_NAMES_ARM64 = new String[]{
            "Cycles", "FIXED_CYCLES", "CORE_ACTIVE_CYCLE"
    };
    private static final String[] INSTRUCTIONS_EVENT_NAMES_ARM64 = new String[]{
            "Instructions", "FIXED_INSTRUCTIONS", "INST_ALL"
    };
    private static final String[] CYCLES_EVENT_NAMES_X86_64 = new String[]{
            "CORE_ACTIVE_CYCLE", "CPU_CLK_UNHALTED.THREAD", "CPU_CLK_UNHALTED.THREAD_P"
    };
    private static final String[] INSTRUCTIONS_EVENT_NAMES_X86_64 = new String[]{
            "INST_ALL", "INST_RETIRED.ANY", "INST_RETIRED.ANY_P"
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
        OptionSpec<String> eventsOpt = setArchSpecificDefaultValues(
                parser.accepts("events", "Comma-separated lists of PMU events to sample. " +
                                "By default, only cycles and instructions are sampled.")
                        .withOptionalArg().ofType(String.class).withValuesSeparatedBy(","));
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
                        "Check if selected PMU events could be used simultaneously.")
                .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

        OptionSet options = ProfilerUtils.parseInitLine(initLine, parser);

        File kpepFile = XCTraceSupport.getKpepFilePath();
        if (!kpepFile.exists()) {
            throw new ProfilerException("Kpep database file does not exist: " + kpepFile.getAbsolutePath() +
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
            List<String> userEvents = getSupportedEvents(options.valuesOf(eventsOpt), perfEvents);
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

    private static OptionSpec<String> setArchSpecificDefaultValues(ArgumentAcceptingOptionSpec<String> events) {
        // TODO: rename constants
        return events.defaultsTo(XCTraceSupport.PerfEvents.CPU_CYCLES,
                XCTraceSupport.PerfEvents.INSTRUCTIONS);
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

    private static List<String> getSupportedEvents(List<String> userEvents, XCTraceSupport.PerfEvents perfEvents) {
        Set<String> supportedEvents = new LinkedHashSet<>();

        // Filter out unsupported events and map aliases back to regular events
        for (String event : userEvents) {
            if (perfEvents.isSupportedEvent(event)) {
                supportedEvents.add(event);
            }
        }

        return new ArrayList<>(supportedEvents);
    }

    /**
     * Checks if selected events could be simultaneously scheduled for counting.
     * <p/>
     * Unlike, Linux perf_events, Instruments and xctrace do not support events multiplexing.
     * Xctrace will crash if a user specified more events than counters available on CPU,
     * or some events are constrained with respect to counters they could be scheduled to
     * and corresponding counters are unavailable.
     * <p/>
     * This functions attempts to check if all selected events could
     * (at least theoretically) be scheduled simultaneously.
     */
    private static void checkEventsCouldBeScheduled(List<String> events, XCTraceSupport.PerfEvents perfEvents)
            throws ProfilerException {
        if (events.size() > perfEvents.getMaxCounters()) {
            throw new ProfilerException("Profiler supports only " + perfEvents.getMaxCounters() + " counters, " +
                    "but " + events.size() + " PMU events were specified: " + events);
        }
        // In general, testing if all events could be scheduled on CPU counter w.r.t. counters constrains
        // is equivalent to finding a maximum matching in a bipartite graph (where one set of nodes are events
        // and another set of nodes are counters; and there's an edge
        // if an event could be scheduled to one of the counters).
        // It's unclear how xctrace/Instruments (or, apparently, kperfdata framework) check these constraints,
        // but it seems like instead of finding a maximum bipartite matching, we can greatly assign events to
        // counters starting from more constrained events (events that could be scheduled on a smaller counters subset)
        // until either all events assigned, or an event that could not be assigned is found.
        // Such an approach should work as long as there are no events having overlapping counters constants of the
        // same size (like, event A could be scheduled on counter {1, 2, 3}, event B on {2, 3, 4}
        // and event C on {3, 4, 5}). Luckily, there are no such events in /usr/share/kpep.
        // And, well, perf_events is doing something similar when scheduling events.
        long availableCountersMask = perfEvents.getConfigurableCountersMask() | perfEvents.getFixedCountersMask();
        List<XCTraceSupport.PerfEventInfo> eventsInfo = events.stream()
                .map(perfEvents::getEvent)
                // Sort by the number of set bits in the mask to process more constrained events first
                .sorted(Comparator.comparingInt(evt -> Long.bitCount(evt.getCounterMask())))
                .collect(Collectors.toList());
        for (XCTraceSupport.PerfEventInfo event : eventsInfo) {
            long eventMask = event.getCounterMask();
            if ((availableCountersMask & eventMask) == 0) {
                throw new ProfilerException("Event " + event.getName() + " could not be used with other selected " +
                        "events due to performance counters constraints. Consider configuring a template using the " +
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
     * <p/>
     * Generated packages are cached in {@code ~/Libraries/Caches/org.openjdk.jmh/} directory to avoid building
     * same packages over and over again.
     * If {@code ~/Libraries/Caches} does not exist, the package will be places in {@code user.dir} folder.
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

        Path traceFile = XCTraceSupport.findTraceFile(temporaryDirectory);
        XCTraceSupport.exportTableOfContents(xctracePath, traceFile.toAbsolutePath().toString(),
                outputFile.getAbsolutePath());

        XCTraceTableOfContentsHandler tocHandler = new XCTraceTableOfContentsHandler();
        tocHandler.parse(outputFile.file());
        XCTraceTableHandler.XCTraceTableDesc tableDesc = findTableDescription(tocHandler);
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

        if (aggregator.eventsCount == 0) {
            return Collections.emptyList();
        }

        Collection<Result<?>> results = new ArrayList<>();

        computeAggregates(results, aggregator);

        aggregator.normalizeByThroughput(opsThroughput);

        for (int i = 0; i < tableDesc.getPmcEvents().size(); i++) {
            String event = tableDesc.getPmcEvents().get(i);
            results.add(new ScalarResult(event, aggregator.eventValues[i],
                    "#/op", AggregationPolicy.AVG));
        }
        if (tableDesc.getTriggerType() == XCTraceTableHandler.TriggerType.PMI) {
            results.add(new ScalarResult(tableDesc.triggerEvent(),
                    aggregator.eventValues[aggregator.eventValues.length - 1],
                    "#/op", AggregationPolicy.AVG));
        }
        return results;
    }

    private void computeAggregates(Collection<Result<?>> results, AggregatedEvents aggregator) {
        switch (perfEvents.getArchitecture()) {
            case "arm64":
                computeAggregatesAppleSiliconArm64(results, aggregator);
                break;
            case "x86_64":
                computeAggregatesX86_64(results, aggregator);
                break;
            default:
                throw new IllegalStateException("Unsupported CPU architecture: " + perfEvents.getArchitecture());
        }
    }

    private void computeAggregatesX86_64(Collection<Result<?>> results, AggregatedEvents aggregator) {
        computeCpiAndIpc(results, aggregator, INSTRUCTIONS_EVENT_NAMES_X86_64, CYCLES_EVENT_NAMES_X86_64);
    }

    private void computeAggregatesAppleSiliconArm64(Collection<Result<?>> results, AggregatedEvents aggregator) {
        computeCpiAndIpc(results, aggregator, INSTRUCTIONS_EVENT_NAMES_ARM64, CYCLES_EVENT_NAMES_ARM64);
        computeAppleSiliconArm64InstDensityMetrics(results, aggregator);
    }

    private static void computeCpiAndIpc(Collection<Result<?>> results, AggregatedEvents aggregator,
                                         String[] instructionEventNames, String[] cyclesEventNames) {
        CounterValue cycles = aggregator.getAnyOfOrNull(cyclesEventNames);
        if (cycles == null || cycles.getValue() == 0D) {
            return;
        }

        CounterValue insts = aggregator.getAnyOfOrNull(instructionEventNames);
        if (insts == null || cycles.getValue() == 0D) {
            return;
        }

        results.add(new ScalarResult("CPI", cycles.getValue() / insts.getValue(),
                cycles.getName() + "/" + insts.getName(), AggregationPolicy.AVG));
        results.add(new ScalarResult("IPC", insts.getValue() / cycles.getValue(),
                insts.getName() + "/" + cycles.getName(), AggregationPolicy.AVG));
    }

    // Compute instructions density metrics (defined in Apple Silicon CPU Optimization Guide,
    // https://developer.apple.com/documentation/apple-silicon/cpu-optimization-guide).
    private static void computeAppleSiliconArm64InstDensityMetrics(Collection<Result<?>> results, AggregatedEvents aggregator) {
        // Try to use INST_ALL first, as it seems to be a more canonical event name
        CounterValue insts = aggregator.getAnyOfOrNull("INST_ALL");
        if (insts == null && (insts = aggregator.getAnyOfOrNull(INSTRUCTIONS_EVENT_NAMES_ARM64)) == null) {
            return;
        }
        for (String eventName : aggregator.eventNames) {
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
        final List<String> eventNames;
        final double[] eventValues;
        long eventsCount = 0;

        long minTimestampMs = Long.MAX_VALUE;
        long maxTimestampMs = Long.MIN_VALUE;

        public AggregatedEvents(XCTraceTableHandler.XCTraceTableDesc tableDesc) {
            List<String> names = new ArrayList<>(tableDesc.getPmcEvents());
            names.add(tableDesc.triggerEvent());
            eventNames = Collections.unmodifiableList(names);
            eventValues = new double[eventNames.size()];
        }

        void add(XCTraceTableProfileHandler.XCTraceSample sample) {
            long[] counters = sample.getPmcValues();
            for (int i = 0; i < counters.length; i++) {
                eventValues[i] += counters[i];
            }
            eventValues[eventValues.length - 1] = sample.getWeight();
            minTimestampMs = Math.min(minTimestampMs, sample.getTimeFromStartNs());
            maxTimestampMs = Math.max(maxTimestampMs, sample.getTimeFromStartNs());
            eventsCount++;
        }

        void normalizeByThroughput(double throughput) {
            if (maxTimestampMs == minTimestampMs) {
                throw new IllegalStateException("Min and max timestamps are the same.");
            }
            double timeSpanMs = (maxTimestampMs - minTimestampMs) / 1e6;
            for (int i = 0; i < eventValues.length; i++) {
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
    }

    private static class CounterValue {
        private final String name;
        private final double value;

        public CounterValue(String name, double value) {
            this.name = name;
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }
}
