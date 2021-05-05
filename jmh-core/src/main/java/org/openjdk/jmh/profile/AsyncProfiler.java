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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;


/**
 * A profiler based on <a href="https://github.com/jvm-profiling-tools/async-profiler/">async-profiler</a>.
 *
 * @author Jason Zaugg
 */
public final class AsyncProfiler implements ExternalProfiler, InternalProfiler {
    private static final String DEFAULT_EVENT = "cpu";

    private final JavaApi instance;

    private final boolean verbose;
    private final Direction direction;
    private final String profilerConfig;
    private final List<OutputType> output;
    private final String event;
    private final File outDir;
    private final int traces;
    private final int flat;

    private boolean warmupStarted = false;
    private boolean measurementStarted = false;
    private int measurementIterationCount;
    private final List<File> generated = new ArrayList<>();

    public AsyncProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();

        parser.formatHelpWith(new ProfilerOptionFormatter("async"));

        OptionSpec<OutputType> optOutput = parser.accepts("output",
                "Output format(s). Supported: " + EnumSet.allOf(OutputType.class).toString() + ".")
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
                "Event to sample: cpu, alloc, wall, lock, cache-misses, etc.")
                .withRequiredArg().ofType(String.class).describedAs("event").defaultsTo(DEFAULT_EVENT);

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

        StringBuilder profilerOptions = new StringBuilder();

        try {
            ProfilerOptionsBuilder builder = new ProfilerOptionsBuilder(set, profilerOptions);
            this.event = optEvent.value(set);
            builder.append(optEvent);
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

            this.traces = optTraces.value(set);
            this.flat = optFlat.value(set);

            this.profilerConfig = profilerOptions.toString();

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
            this.direction = optDirection.value(set);
            this.output = optOutput.values(set);
            this.verbose = optVerbose.value(set);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        if (iterationParams.getType() == IterationType.WARMUP) {
            if (!warmupStarted) {
                // Collect profiles during warmup to warmup the profiler itself.
                execute("start," + profilerConfig);
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
                execute("start," + profilerConfig);
                measurementStarted = true;
            }
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams,
                                                       IterationResult iterationResult) {
        if (iterationParams.getType() == IterationType.MEASUREMENT) {
            measurementIterationCount += 1;
            if (measurementIterationCount == iterationParams.getCount()) {
                File trialOutDir = createTrialOutDir(benchmarkParams);
                return Collections.singletonList(stopAndDump(trialOutDir));
            }
        }

        return Collections.emptyList();
    }

    private File createTrialOutDir(BenchmarkParams benchmarkParams) {
        String fileName = benchmarkParams.id();
        File trialOutDir = new File(this.outDir, fileName);
        trialOutDir.mkdirs();
        return trialOutDir;
    }

    private TextResult stopAndDump(File trialOutDir) {
        execute("stop");

        StringWriter output = new StringWriter();
        PrintWriter pw = new PrintWriter(output);
        for (OutputType outputType : this.output) {
            switch (outputType) {
                case text:
                    String textOutput = dump(trialOutDir, "summary-%s.txt", "summary,flat=" + flat + ",traces=" + traces);
                    pw.println(textOutput);
                    break;
                case collapsed:
                    dump(trialOutDir, "collapsed-%s.csv", "collapsed");
                    break;
                case flamegraph:
                    boolean svg = svgFlamegraphs(execute("version"));
                    String ext = svg ? "svg" : "html";
                    String type = svg ? "svg" : "flamegraph";
                    if (direction == Direction.both || direction == Direction.forward) {
                        dump(trialOutDir, "flame-%s-forward." + ext, type);
                    }
                    if (direction == Direction.both || direction == Direction.reverse) {
                        dump(trialOutDir, "flame-%s-reverse." + ext, type + ",reverse");
                    }
                    break;
                case tree:
                    dump(trialOutDir, "tree-%s.html", "tree");
                    break;
                case jfr:
                    dump(trialOutDir, "%s.jfr", "jfr");
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

        return new TextResult(output.toString(), "async");
    }

    private String dump(File specificOutDir, String fileNameFormatString, String content) {
        File output = new File(specificOutDir, String.format(fileNameFormatString, event));
        generated.add(output);
        String result = execute(content + "," + profilerConfig);
        try {
            FileUtils.writeLines(output, Collections.singletonList(result));
            return result;
        } catch (IOException e) {
            return "N/A";
        }
    }

    static boolean svgFlamegraphs(String ver) {
        if (ver.startsWith("1")) {
            // The last SVG-enabled version is 1.x
            return true;
        } else {
            return false;
        }
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

        ProfilerOptionsBuilder(OptionSet optionSet, StringBuilder profilerOptions) {
            this.optionSet = optionSet;
            this.profilerOptions = profilerOptions;
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
            profilerOptions.append(optionName).append('=').append(optionSet.valueOf(option).toString());
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

        private <T> void appendMulti(OptionSpec<T> option) {
            if (optionSet.has(option)) {
                assert (option.options().size() == 1);
                String optionName = option.options().iterator().next();
                for (T value : optionSet.valuesOf(option)) {
                    profilerOptions.append(',').append(optionName).append('=').append(value.toString());
                }
            }
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
