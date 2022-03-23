/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


/**
 * A profiler based on <a href="https://github.com/jvm-profiling-tools/async-profiler/">async-profiler</a>.
 *
 * @author Jason Zaugg
 */
public final class AsyncProfiler implements ExternalProfiler, InternalProfiler {

    private final JavaApi instance;

    private final boolean verbose;
    private final Direction direction;
    private final String profilerConfig;
    private final List<OutputType> output;
    private final String outputFilePrefix;
    private final File outDir;
    private File trialOutDir;
    private final int traces;
    private final int flat;

    private boolean isVersion1x;

    private boolean warmupStarted;
    private boolean measurementStarted;
    private int measurementIterationCount;
    private final LinkedHashSet<File> generated = new LinkedHashSet<>();

    public AsyncProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();

        parser.formatHelpWith(new ProfilerOptionFormatter("async"));

        OptionSpec<OutputType> optOutput = parser.accepts("output",
                "Output format(s). Supported: " + EnumSet.allOf(OutputType.class) + ".")
                .withRequiredArg().ofType(OutputType.class).withValuesSeparatedBy(",").describedAs("format+").defaultsTo(OutputType.text);

        OptionSpec<Direction> optDirection = parser.accepts("direction",
                "Direction(s) of flame graph. Supported: " + EnumSet.allOf(Direction.class) + ".")
                .withRequiredArg().ofType(Direction.class).describedAs("direction").defaultsTo(Direction.both);

        OptionSpec<String> optLibPath = parser.accepts("libPath",
                "Location of asyncProfiler library. If not specified, System.loadLibrary will be used " +
                "and the library must be made available to the forked JVM in an entry of -Djava.library.path, " +
                "LD_LIBRARY_PATH (Linux), or DYLD_LIBRARY_PATH (Mac OS).")
                .withRequiredArg().ofType(String.class).describedAs("path");

        OptionSpec<String> optEvent = parser.accepts("event",
                "Event to sample: cpu, alloc, lock, wall, itimer; com.foo.Bar.methodName; any event from `perf list` e.g. cache-misses")
                .withRequiredArg().ofType(String.class).describedAs("event").defaultsTo("cpu");

        String secondaryEventOk = "May be captured as a secondary event under output=jfr.";
        OptionSpec<String> optAlloc = parser.accepts("alloc",
                "Enable allocation profiling. Optional argument (e.g. =512k) reduces sampling from the default of one-sample-per-TLAB. " + secondaryEventOk)
                .withOptionalArg().ofType(String.class).describedAs("sample bytes");

        OptionSpec<String> optLock = parser.accepts("lock",
                "Enable lock profiling. Optional argument (e.g. =1ms) limits capture based on lock duration. " + secondaryEventOk)
                .withOptionalArg().ofType(String.class).describedAs("duration");

        OptionSpec<String> optDir = parser.accepts("dir",
                "Output directory.")
                .withRequiredArg().ofType(String.class).describedAs("dir");

        OptionSpec<Long> optInterval = parser.accepts("interval",
                "Profiling interval.")
                .withRequiredArg().ofType(Long.class).describedAs("ns");

        OptionSpec<Integer> optJstackDepth = parser.accepts("jstackdepth",
                "Maximum Java stack depth.")
                .withRequiredArg().ofType(Integer.class).describedAs("frames");

        OptionSpec<Long> optFrameBuf = parser.accepts("framebuf",
                "Size of profiler framebuffer.")
                .withRequiredArg().ofType(Long.class).describedAs("bytes");

        OptionSpec<Boolean> optFilter = parser.accepts("filter",
                "Enable thread filtering during collection. Useful for wall clock profiling, " +
                "but only if the workload registers the relevant threads programatically " +
                "via `AsyncProfiler.JavaApi.getInstance().filterThread(thread, enabled)`.")
                .withRequiredArg().ofType(Boolean.class).defaultsTo(false).describedAs("boolean");

        OptionSpec<Boolean> optThreads = parser.accepts("threads",
                "Profile threads separately.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optSimple = parser.accepts("simple",
                "Simple class names instead of FQN.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optSig = parser.accepts("sig",
                "Print method signatures.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optAnn = parser.accepts("ann",
                "Annotate Java method names.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<String> optInclude = parser.accepts("include",
                "Output only stack traces containing the specified pattern.")
                .withRequiredArg().withValuesSeparatedBy(",").ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optExclude = parser.accepts("exclude",
                "Exclude stack traces with the specified pattern.")
                .withRequiredArg().withValuesSeparatedBy(",").ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optRawCommand = parser.accepts("rawCommand",
                "Command to pass directly to async-profiler. Use to access new features of JMH " +
                "profiler that are not yet supported in this option parser.")
                .withRequiredArg().ofType(String.class).describedAs("command");

        OptionSpec<String> optTitle = parser.accepts("title",
                "SVG title.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<Long> optWidth = parser.accepts("width",
                "SVG width.")
                .withRequiredArg().ofType(Long.class).describedAs("pixels");

        OptionSpec<Long> optMinWidth = parser.accepts("minwidth", "Skip frames smaller than px")
                .withRequiredArg().ofType(Long.class).describedAs("pixels");

        OptionSpec<Boolean> optAllKernel = parser.accepts("allkernel",
                "Only include kernel-mode events.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optAllUser = parser.accepts("alluser",
                "Only include user-mode events.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<CStackMode> optCStack = parser.accepts("cstack",
                "How to traverse C stack: Supported: " + EnumSet.allOf(CStackMode.class) + ".")
                .withRequiredArg().ofType(CStackMode.class).describedAs("mode");

        OptionSpec<Boolean> optVerbose = parser.accepts("verbose",
                "Output the sequence of commands.")
                .withRequiredArg().ofType(Boolean.class).defaultsTo(false).describedAs("bool");

        OptionSpec<Integer> optTraces = parser.accepts("traces",
                "Number of top traces to include in the default output.")
                .withRequiredArg().ofType(Integer.class).defaultsTo(200).describedAs("int");

        OptionSpec<Integer> optFlat = parser.accepts("flat",
                "Number of top flat profiles to include in the default output.")
                .withRequiredArg().ofType(Integer.class).defaultsTo(200).describedAs("int");

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            ProfilerOptionsBuilder builder = new ProfilerOptionsBuilder(set);

            if (!set.has(optDir)) {
                outDir = new File(System.getProperty("user.dir"));
            } else {
                outDir = new File(set.valueOf(optDir));
            }

            builder.appendIfExists(optInterval);
            builder.appendIfExists(optJstackDepth);
            builder.appendIfTrue(optThreads);
            builder.appendIfTrue(optSimple);
            builder.appendIfTrue(optSig);
            builder.appendIfTrue(optAnn);
            builder.appendIfExists(optFrameBuf);
            if (optFilter.value(set)) {
                builder.appendRaw("filter");
            }
            builder.appendMulti(optInclude);
            builder.appendMulti(optExclude);

            builder.appendIfExists(optTitle);
            builder.appendIfExists(optWidth);
            builder.appendIfExists(optMinWidth);

            builder.appendIfTrue(optAllKernel);
            builder.appendIfTrue(optAllUser);
            builder.appendIfExists(optCStack);

            if (set.has(optRawCommand)) {
                builder.appendRaw(optRawCommand.value(set));
            }

            traces = optTraces.value(set);
            flat = optFlat.value(set);

            try {
                if (set.has(optLibPath)) {
                    instance = JavaApi.getInstance(optLibPath.value(set));
                } else {
                    instance = JavaApi.getInstance();
                }
            } catch (UnsatisfiedLinkError e) {
                throw new ProfilerException("Unable to load async-profiler. Ensure asyncProfiler library " +
                        "is on LD_LIBRARY_PATH (Linux), DYLD_LIBRARY_PATH (Mac OS), or -Djava.library.path. " +
                        "Alternatively, point to explicit library location with -prof async:libPath=<path>.", e);
            }
            verbose = optVerbose.value(set);
            try {
                String version = instance.execute("version");
                if (verbose) {
                    System.out.println("[async-profiler] version=" + version);
                }
                isVersion1x = version.startsWith("1.");
            } catch (IOException e) {
                throw new ProfilerException(e);
            }
            direction = optDirection.value(set);

            output = optOutput.values(set);

            // Secondary events are those that may be collected simultaneously with a primary event in a JFR profile.
            // To be used as such, we require they are specifed with the lock and alloc option, rather than event=lock,
            // event=alloc.
            Set<String> secondaryEvents = new HashSet<>();

            if (set.has(optAlloc)) {
                secondaryEvents.add("alloc");
                builder.append(optAlloc);
            }

            if (set.has(optLock)) {
                secondaryEvents.add("lock");
                builder.append(optLock);
            }

            if (set.has(optEvent)) {
                String evName = set.valueOf(optEvent);
                if (evName.contains(",")) {
                    throw new ProfilerException("Event name should not contain commas: " + evName);
                }
                outputFilePrefix = evName;
                builder.append(optEvent);
            } else {
                if (secondaryEvents.isEmpty()) {
                    // Default to the cpu event if no events at all are selected.
                    builder.appendRaw("event=cpu");
                    outputFilePrefix = "cpu";
                } else if (secondaryEvents.size() == 1) {
                    // No primary event, one secondary -- promote it to the primary event. This means any output
                    // format is allowed and the event name will be included in the output file name.
                    outputFilePrefix = secondaryEvents.iterator().next();
                    secondaryEvents.clear();
                } else {
                    outputFilePrefix = "profile";
                }
            }

            if (!secondaryEvents.isEmpty()) {
                if (isVersion1x) {
                    throw new ProfilerException("Secondary event capture not supported on async-profiler 1.x");
                }
                if (output.size() > 1 || output.get(0) != OutputType.jfr) {
                    throw new ProfilerException("Secondary event capture is only supported with output=" + OutputType.jfr.name());
                }
            }

            profilerConfig = builder.profilerOptions();
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        if (trialOutDir == null) {
            createTrialOutDir(benchmarkParams);
        }
        if (iterationParams.getType() == IterationType.WARMUP) {
            if (!warmupStarted) {
                // Collect profiles during warmup to warmup the profiler itself.
                start();
                warmupStarted = true;
            }
        }
        if (iterationParams.getType() == IterationType.MEASUREMENT) {
            if (!measurementStarted) {
                if (warmupStarted) {
                    // Discard samples collected during warmup...
                    execute("stop");
                }
                // ...and start collecting again.
                start();
                measurementStarted = true;
            }
        }
    }

    private void start() {
        if (output.contains(OutputType.jfr)) {
            execute("start," + profilerConfig + ",file=" + outputFile("jfr-%s.jfr").getAbsolutePath());
        } else {
            execute("start," + profilerConfig);
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams,
                                                       IterationResult iterationResult) {
        if (iterationParams.getType() == IterationType.MEASUREMENT) {
            measurementIterationCount += 1;
            if (measurementIterationCount == iterationParams.getCount()) {
                return Collections.singletonList(stopAndDump());
            }
        }

        return Collections.emptyList();
    }

    private void createTrialOutDir(BenchmarkParams benchmarkParams) {
        if (trialOutDir == null) {
            // async-profiler expands %p to PID and %t to timestamp, make sure we don't
            // include % in the file name.
            String fileName = benchmarkParams.id().replace("%", "_");
            trialOutDir = new File(outDir, fileName);
            trialOutDir.mkdirs();
        }
    }

    private TextResult stopAndDump() {
        execute("stop");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (OutputType outputType : output) {
            switch (outputType) {
                case text:
                    File out = outputFile("summary-%s.txt");
                    if (isVersion1x) {
                        dump(out, "summary,flat=" + flat + ",traces=" + traces);
                    } else {
                        dump(out, "flat=" + flat + ",traces=" + traces);
                    }
                    try {
                        for (String line : FileUtils.readAllLines(out)) {
                            pw.println(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case collapsed:
                    dump(outputFile("collapsed-%s.csv"), "collapsed");
                    break;
                case flamegraph:
                    // The last SVG-enabled version is 1.x
                    String ext = isVersion1x ? "svg" : "html";
                    if (direction == Direction.both || direction == Direction.forward) {
                        dump(outputFile("flame-%s-forward." + ext), "flamegraph");
                    }
                    if (direction == Direction.both || direction == Direction.reverse) {
                        dump(outputFile("flame-%s-reverse." + ext), "flamegraph,reverse");
                    }
                    break;
                case tree:
                    dump(outputFile("tree-%s.html"), "tree");
                    break;
                case jfr:
                    // JFR is already dumped into file by async-profiler.
                    break;
            }
        }

        pw.println("Async profiler results:");
        for (File file : generated) {
            pw.print("  ");
            pw.println(file.getPath());
        }
        pw.flush();
        pw.close();

        return new TextResult(sw.toString(), "async");
    }

    private void dump(File target, String command) {
        execute(command + "," + profilerConfig + ",file=" + target.getAbsolutePath());
    }

    private File outputFile(String fileNameFormat) {
        File output = new File(trialOutDir, String.format(fileNameFormat, outputFilePrefix));
        generated.add(output);
        return output;
    }

    private String execute(String command) {
        if (verbose) {
            System.out.println("[async-profiler] " + command);
        }
        try {
            return instance.execute(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum CStackMode {
        fp,
        lbr,
        no
    }

    public enum OutputType {
        text,
        collapsed,
        flamegraph,
        tree,
        jfr
    }

    public enum Direction {
        forward,
        reverse,
        both,
    }

    private static class ProfilerOptionsBuilder {
        private final OptionSet optionSet;
        private final StringBuilder profilerOptions;

        ProfilerOptionsBuilder(OptionSet optionSet) {
            this.optionSet = optionSet;
            this.profilerOptions = new StringBuilder();
        }

        <T> void appendIfExists(OptionSpec<T> option) {
            if (optionSet.has(option)) {
                append(option);
            }
        }

        <T> void append(OptionSpec<T> option) {
            assert (option.options().size() == 1);
            String optionName = option.options().iterator().next();
            separate();
            profilerOptions.append(optionName);
            T arg = optionSet.valueOf(option);
            if (arg != null) {
                profilerOptions.append('=').append(arg);
            }
        }

        void appendRaw(String command) {
            separate();
            profilerOptions.append(command);
        }

        private void separate() {
            if (profilerOptions.length() > 0) {
                profilerOptions.append(',');
            }
        }

        void appendIfTrue(OptionSpec<Boolean> option) {
            if (optionSet.has(option) && optionSet.valueOf(option)) {
                append(option);
            }
        }

        <T> void appendMulti(OptionSpec<T> option) {
            if (optionSet.has(option)) {
                assert (option.options().size() == 1);
                String optionName = option.options().iterator().next();
                for (T value : optionSet.valuesOf(option)) {
                    separate();
                    profilerOptions.append(optionName).append('=').append(value.toString());
                }
            }
        }

        public String profilerOptions() {
            return profilerOptions.toString();
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        List<String> args = new ArrayList<>();
        args.add("-XX:+UnlockDiagnosticVMOptions");
        // Recommended option for async-profiler, enable automatically.
        args.add("-XX:+DebugNonSafepoints");
        return args;
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        return Collections.emptyList();
    }

    @Override
    public boolean allowPrintOut() {
        return true;
    }

    @Override
    public boolean allowPrintErr() {
        return true;
    }

    @Override
    public String getDescription() {
        return "async-profiler profiler provider.";
    }

    // Made public so that power-users could can call filterThread from within the workload
    // to limit collection to a set of threads. This is useful for wall-clock profiling.
    // Adding support in JMH to pass the threads to profilers seems like an invasive change for
    // this niche use case.
    public static final class JavaApi {
        private static EnumSet<Thread.State> ignoredThreadStates = EnumSet.of(Thread.State.NEW, Thread.State.TERMINATED);
        private static JavaApi INSTANCE;

        public static JavaApi getInstance(String libraryFileName) {
            if (INSTANCE == null) {
                synchronized (AsyncProfiler.class) {
                    INSTANCE = new JavaApi(libraryFileName);
                }
            }
            return INSTANCE;
        }

        public static JavaApi getInstance() {
            if (INSTANCE == null) {
                synchronized (AsyncProfiler.class) {
                    INSTANCE = new JavaApi();
                }
            }
            return INSTANCE;
        }

        private JavaApi(String libraryFileName) {
            System.load(libraryFileName);
        }

        private JavaApi() {
            System.loadLibrary("asyncProfiler");
        }

        public String execute(String command) throws IOException {
            return execute0(command);
        }

        /**
         * Enable or disable profile collection for threads.
         *
         * @param thread The thread to enable or disable.
         *               <code>null</code> indicates the current thread.
         * @param enable Whether to enable or disable.
         */
        public void filterThread(Thread thread, boolean enable) {
            if (thread == null) {
                filterThread0(null, enable);
            } else {
                synchronized (thread) {
                    Thread.State state = thread.getState();
                    if (!ignoredThreadStates.contains(state)) {
                        filterThread0(thread, enable);
                    }
                }
            }
        }

        // Loading async-profiler will automatically bind these native methods to the profiler implementation.
        private native void start0(String event, long interval, boolean reset) throws IllegalStateException;

        private native void stop0() throws IllegalStateException;

        private native String execute0(String command) throws IllegalArgumentException, IOException;

        private native long getSamples();

        private native void filterThread0(Thread thread, boolean enable);
    }
}
