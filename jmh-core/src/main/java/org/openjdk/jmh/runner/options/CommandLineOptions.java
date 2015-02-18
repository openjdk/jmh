/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.runner.options;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class that handles all the command line options.
 */
public class CommandLineOptions implements Options {
    private static final long serialVersionUID = 5565183446360224399L;

    private final Optional<Integer> iterations;
    private final Optional<TimeValue> timeout;
    private final Optional<TimeValue> runTime;
    private final Optional<Integer> batchSize;
    private final Optional<Integer> warmupIterations;
    private final Optional<TimeValue> warmupTime;
    private final Optional<Integer> warmupBatchSize;
    private final List<Mode> benchMode = new ArrayList<Mode>();
    private final Optional<Integer> threads;
    private final List<Integer> threadGroups = new ArrayList<Integer>();
    private final Optional<Boolean> synchIterations;
    private final Optional<Boolean> gcEachIteration;
    private final Optional<VerboseMode> verbose;
    private final Optional<Boolean> failOnError;
    private final List<Class<? extends Profiler>> profilers = new ArrayList<Class<? extends Profiler>>();
    private final Optional<TimeUnit> timeUnit;
    private final Optional<Integer> opsPerInvocation;
    private final List<String> regexps = new ArrayList<String>();
    private final Optional<Integer> fork;
    private final Optional<Integer> warmupFork;
    private final Optional<String> output;
    private final Optional<String> result;
    private final Optional<ResultFormatType> resultFormat;
    private final Optional<String> jvm;
    private final Optional<Collection<String>> jvmArgs;
    private final Optional<Collection<String>> jvmArgsAppend;
    private final Optional<Collection<String>> jvmArgsPrepend;
    private final List<String> excludes = new ArrayList<String>();
    private final Optional<WarmupMode> warmupMode;
    private final List<String> warmupMicros = new ArrayList<String>();
    private final Multimap<String, String> params = new HashMultimap<String, String>();
    private final boolean list;
    private final boolean listResultFormats;
    private final boolean help;
    private final boolean listProfilers;

    private final transient OptionParser parser;

    /**
     * Parses the given command line.
     * @param argv argument list
     * @throws CommandLineOptionException if some options are misspelled
     */
    public CommandLineOptions(String... argv) throws CommandLineOptionException {
        parser = new OptionParser();
        parser.formatHelpWith(new OptionFormatter());

        OptionSpec<Integer> optMeasureCount = parser.accepts("i", "Number of measurement iterations to do.")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<Integer> optMeasureBatchSize = parser.accepts("bs", "Batch size: number of benchmark method calls per operation. " +
                "(some benchmark modes can ignore this setting)")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<String> optMeasureTime = parser.accepts("r", "Time to spend at each measurement iteration.")
                .withRequiredArg().ofType(String.class).describedAs("time");

        OptionSpec<Integer> optWarmupCount = parser.accepts("wi", "Number of warmup iterations to do.")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<Integer> optWarmupBatchSize = parser.accepts("wbs", "Warmup batch size: number of benchmark method calls per operation. " +
                "(some benchmark modes can ignore this setting)")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<String> optWarmupTime = parser.accepts("w", "Time to spend at each warmup iteration.")
                .withRequiredArg().ofType(String.class).describedAs("time");

        OptionSpec<String> optTimeoutTime = parser.accepts("to", "Timeout for benchmark iteration.")
                .withRequiredArg().ofType(String.class).describedAs("time");

        OptionSpec<String> optThreads = parser.accepts("t", "Number of worker threads to run with.")
                .withRequiredArg().ofType(String.class).describedAs("int");

        OptionSpec<String> optBenchmarkMode = parser.accepts("bm", "Benchmark mode. Available modes are: " + Mode.getKnown())
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(',').describedAs("mode");

        OptionSpec<Boolean> optSyncIters = parser.accepts("si", "Synchronize iterations?")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optGC = parser.accepts("gc", "Should JMH force GC between iterations?")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optFOE = parser.accepts("foe", "Should JMH fail immediately if any benchmark had" +
                " experienced the unrecoverable error?")
                .withOptionalArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<String> optVerboseMode = parser.accepts("v", "Verbosity mode. Available modes are: " + Arrays.toString(VerboseMode.values()))
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> optArgs = parser.nonOptions("Benchmarks to run (regexp+).")
                .describedAs("regexp+");

        OptionSpec<Integer> optForks = parser.accepts("f", "How many times to forks a single benchmark." +
                " Use 0 to disable forking altogether (WARNING: disabling forking may have detrimental" +
                " impact on benchmark and infrastructure reliability, you might want to use different" +
                " warmup mode instead).")
                .withOptionalArg().ofType(Integer.class).describedAs("int");

        OptionSpec<Integer> optWarmupForks = parser.accepts("wf", "How many warmup forks to make " +
                "for a single benchmark. 0 to disable warmup forks.")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<String> optOutput = parser.accepts("o", "Redirect human-readable output to file.")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<String> optOutputResults = parser.accepts("rff", "Write results to given file.")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<String> optProfilers = parser.accepts("prof", "Use profilers to collect additional data." +
                " See the list of available profilers first.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class).describedAs("profiler+");

        OptionSpec<Integer> optThreadGroups = parser.accepts("tg", "Override thread group distribution for asymmetric benchmarks.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(Integer.class).describedAs("int+");

        OptionSpec<String> optJvm = parser.accepts("jvm", "Custom JVM to use when forking (path to JVM executable).")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgs = parser.accepts("jvmArgs", "Custom JVM args to use when forking.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsAppend = parser.accepts("jvmArgsAppend", "Custom JVM args to use when forking (append these)")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsPrepend = parser.accepts("jvmArgsPrepend", "Custom JVM args to use when forking (prepend these)")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optTU = parser.accepts("tu", "Output time unit. Available time units are: [m, s, ms, us, ns].")
                .withRequiredArg().ofType(String.class).describedAs("TU");

        OptionSpec<Integer> optOPI = parser.accepts("opi", "Operations per invocation.")
                .withRequiredArg().ofType(Integer.class).describedAs("int");

        OptionSpec<String> optResultFormat = parser.accepts("rf", "Result format type. See the list of available result formats first.")
                .withRequiredArg().ofType(String.class).describedAs("type");

        OptionSpec<String> optWarmupMode = parser.accepts("wm", "Warmup mode for warming up selected benchmarks. Warmup modes are: " + Arrays.toString(WarmupMode.values()) + ".")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> optExcludes = parser.accepts("e", "Benchmarks to exclude from the run.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optParams = parser.accepts("p", "Benchmark parameters. This option is expected to be used once" +
                " per parameter. Parameter name and parameter values should be separated with equals sign." +
                " Parameter values should be separated with commas.")
                .withRequiredArg().ofType(String.class).describedAs("param={v,}*");

        OptionSpec<String> optWarmupBenchmarks = parser.accepts("wmb", "Warmup benchmarks to include in the run " +
                "in addition to already selected. JMH will not measure these benchmarks, but only use them" +
                " for the warmup.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class).describedAs("regexp+");

        parser.accepts("l", "List matching benchmarks and exit.");
        parser.accepts("lrf", "List result formats.");
        parser.accepts("lprof", "List profilers.");
        parser.accepts("h", "Display help.");

        try {
            OptionSet set = parser.parse(argv);

            if (set.has(optExcludes)) {
                excludes.addAll(optExcludes.values(set));
            }

            if (set.has(optWarmupBenchmarks)) {
                warmupMicros.addAll(optWarmupBenchmarks.values(set));
            }

            if (set.has(optTU)) {
                String va = optTU.value(set);
                TimeUnit tu;
                if (va.equalsIgnoreCase("ns")) {
                    tu = TimeUnit.NANOSECONDS;
                } else if (va.equalsIgnoreCase("us")) {
                    tu = TimeUnit.MICROSECONDS;
                } else if (va.equalsIgnoreCase("ms")) {
                    tu = TimeUnit.MILLISECONDS;
                } else if (va.equalsIgnoreCase("s")) {
                    tu = TimeUnit.SECONDS;
                } else if (va.equalsIgnoreCase("m")) {
                    tu = TimeUnit.MINUTES;
                } else if (va.equalsIgnoreCase("h")) {
                    tu = TimeUnit.HOURS;
                } else {
                    throw new CommandLineOptionException("Unknown time unit: " + va);
                }
                timeUnit = Optional.of(tu);
            } else {
                timeUnit = Optional.none();
            }

            opsPerInvocation = Optional.eitherOf(optOPI.value(set));

            if (set.has(optWarmupMode)) {
                try {
                    warmupMode = Optional.of(WarmupMode.valueOf(optWarmupMode.value(set)));
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                warmupMode = Optional.none();
            }

            if (set.has(optResultFormat)) {
                try {
                    resultFormat = Optional.of(ResultFormatType.valueOf(optResultFormat.value(set).toUpperCase()));
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                resultFormat = Optional.none();
            }

            help = set.has("h");
            list = set.has("l");
            listResultFormats = set.has("lrf");
            listProfilers = set.has("lprof");

            iterations = Optional.eitherOf(optMeasureCount.value(set));

            batchSize = Optional.eitherOf(optMeasureBatchSize.value(set));

            if (set.has(optMeasureTime)) {
                String value = optMeasureTime.value(set);
                try {
                    runTime = Optional.of(TimeValue.fromString(value));
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                runTime = Optional.none();
            }

            warmupIterations = Optional.eitherOf(optWarmupCount.value(set));

            warmupBatchSize = Optional.eitherOf(optWarmupBatchSize.value(set));

            if (set.has(optWarmupTime)) {
                String value = optWarmupTime.value(set);
                try {
                    warmupTime = Optional.of(TimeValue.fromString(value));
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                warmupTime = Optional.none();
            }

            if (set.has(optTimeoutTime)) {
                String value = optTimeoutTime.value(set);
                try {
                    timeout = Optional.of(TimeValue.fromString(value));
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                timeout = Optional.none();
            }

            if (set.has(optThreads)) {
                String v = optThreads.value(set);
                if (v.equalsIgnoreCase("max")) {
                    threads = Optional.of(Threads.MAX);
                } else {
                    try {
                        threads = Optional.of(Integer.valueOf(v));
                    } catch (IllegalArgumentException iae) {
                        throw new CommandLineOptionException(iae.getMessage(), iae);
                    }
                }
            } else {
                threads = Optional.none();
            }

            if (set.has(optBenchmarkMode)) {
                try {
                    List<Mode> modes = new ArrayList<Mode>();
                    for (String m : optBenchmarkMode.values(set)) {
                        modes.add(Mode.deepValueOf(m));
                    }
                    benchMode.addAll(modes);
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            }

            if (set.has(optSyncIters)) {
                if (set.hasArgument(optSyncIters)) {
                    synchIterations = Optional.of(optSyncIters.value(set));
                } else {
                    synchIterations = Optional.of(true);
                }
            } else {
                synchIterations = Optional.none();
            }

            if (set.has(optGC)) {
                if (set.hasArgument(optGC)) {
                    gcEachIteration = Optional.of(optGC.value(set));
                } else {
                    gcEachIteration = Optional.of(true);
                }
            } else {
                gcEachIteration = Optional.none();
            }

            if (set.has(optFOE)) {
                if (set.hasArgument(optFOE)) {
                    failOnError = Optional.of(optFOE.value(set));
                } else {
                    failOnError = Optional.of(true);
                }
            } else {
                failOnError = Optional.none();
            }

            if (set.has(optVerboseMode)) {
                try {
                    if (set.hasArgument(optVerboseMode)) {
                        verbose = Optional.of(VerboseMode.valueOf(set.valueOf(optVerboseMode).toUpperCase()));
                    } else {
                        verbose = Optional.of(VerboseMode.EXTRA);
                    }
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            } else {
                verbose = Optional.none();
            }

            regexps.addAll(set.valuesOf(optArgs));

            if (set.has(optForks)) {
                if (set.hasArgument(optForks)) {
                    fork = Optional.of(optForks.value(set));
                } else {
                    fork = Optional.of(1);
                }
            } else {
                fork = Optional.none();
            }

            warmupFork = Optional.eitherOf(optWarmupForks.value(set));
            output = Optional.eitherOf(optOutput.value(set));
            result = Optional.eitherOf(optOutputResults.value(set));

            if (set.has(optProfilers)) {
                try {
                    for (String m : optProfilers.values(set)) {
                        Class<? extends Profiler> pClass = ProfilerFactory.getProfilerByName(m);
                        if (pClass == null) {
                            throw new CommandLineOptionException("Unable to find profiler: " + m);
                        }
                        profilers.add(pClass);
                    }
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            }

            if (set.has(optThreadGroups)) {
                threadGroups.addAll(set.valuesOf(optThreadGroups));
            }

            jvm = Optional.eitherOf(optJvm.value(set));

            if (set.hasArgument(optJvmArgs)) {
                jvmArgs = Optional.<Collection<String>>of(Arrays.asList(optJvmArgs.value(set).trim().split("[ ]+")));
            } else {
                jvmArgs = Optional.none();
            }

            if (set.hasArgument(optJvmArgsAppend)) {
                jvmArgsAppend = Optional.<Collection<String>>of(Arrays.asList(optJvmArgsAppend.value(set).trim().split("[ ]+")));
            } else {
                jvmArgsAppend = Optional.none();
            }

            if (set.hasArgument(optJvmArgsPrepend)) {
                jvmArgsPrepend = Optional.<Collection<String>>of(Arrays.asList(optJvmArgsPrepend.value(set).trim().split("[ ]+")));
            } else {
                jvmArgsPrepend = Optional.none();
            }

            if (set.hasArgument(optParams)) {
                for (String p : optParams.values(set)) {
                    String[] keys = p.split("=");
                    if (keys.length != 2) {
                        throw new CommandLineOptionException("Unable to parse parameter string \"" + p + "\"");
                    }
                    params.putAll(keys[0], Arrays.asList(keys[1].split(",")));
                }
            }

        } catch (OptionException e) {
            throw new CommandLineOptionException(e.getMessage(), e);
        }
    }

    public void showHelp() throws IOException {
        parser.printHelpOn(System.err);
    }

    public void listProfilers() {
        StringBuilder supported = new StringBuilder();
        StringBuilder unsupported = new StringBuilder();

        List<Class<? extends Profiler>> discoveredProfilers = ProfilerFactory.getDiscoveredProfilers();

        for (Class<? extends Profiler> s : ProfilerFactory.getAvailableProfilers()) {
            List<String> initMessages = new ArrayList<String>();
            if (ProfilerFactory.checkSupport(s, initMessages)) {
                supported.append(String.format("%20s: %s %s\n",
                        ProfilerFactory.getLabel(s),
                        ProfilerFactory.getDescription(s),
                        discoveredProfilers.contains(s) ? "(discovered)" : ""));
            } else {
                unsupported.append(String.format("%20s: %s %s\n",
                        ProfilerFactory.getLabel(s),
                        ProfilerFactory.getDescription(s),
                        discoveredProfilers.contains(s) ? "(discovered)" : ""));
                for (String im : initMessages) {
                    unsupported.append(String.format("%20s  %s\n", "", im));
                }
                unsupported.append("\n");
            }
        }

        if (!supported.toString().isEmpty()) {
            System.out.println("Supported profilers:\n" + supported.toString());
        }

        if (!unsupported.toString().isEmpty()) {
            System.out.println("Unsupported profilers:\n" + unsupported.toString());
        }
    }

    public void listResultFormats() {
        StringBuilder sb = new StringBuilder();

        for (ResultFormatType f : ResultFormatType.values()) {
            sb.append(f.toString().toLowerCase());
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);

        System.out.println("Available formats: " + sb.toString());
    }

    public boolean shouldList() {
        return list;
    }

    public boolean shouldListResultFormats() {
        return listResultFormats;
    }

    public boolean shouldHelp() {
        return help;
    }

    public boolean shouldListProfilers() {
        return listProfilers;
    }

    @Override
    public Optional<WarmupMode> getWarmupMode() {
        return warmupMode;
    }

    @Override
    public List<String> getIncludes() {
        return regexps;
    }

    @Override
    public List<String> getExcludes() {
        return excludes;
    }

    @Override
    public List<String> getWarmupIncludes() {
        return warmupMicros;
    }

    @Override
    public Optional<String> getJvm() {
        return jvm;
    }

    @Override
    public Optional<Collection<String>> getJvmArgs() {
        return jvmArgs;
    }

    @Override
    public Optional<Collection<String>> getJvmArgsAppend() {
        return jvmArgsAppend;
    }

    @Override
    public Optional<Collection<String>> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    @Override
    public Optional<Collection<String>> getParameter(String name) {
        Collection<String> list = params.get(name);
        if (list == null || list.isEmpty()){
            return Optional.none();
        } else {
            return Optional.of(list);
        }
    }

    @Override
    public Optional<Integer> getForkCount() {
        return fork;
    }

    @Override
    public Optional<Integer> getWarmupForkCount() {
        return warmupFork;
    }

    @Override
    public Optional<String> getOutput() {
        return output;
    }

    @Override
    public Optional<ResultFormatType> getResultFormat() {
        return resultFormat;
    }

    @Override
    public Optional<String> getResult() {
        return result;
    }

    @Override
    public Optional<Integer> getMeasurementIterations() {
        return iterations;
    }

    @Override
    public Optional<Integer> getMeasurementBatchSize() {
        return batchSize;
    }

    @Override
    public Optional<TimeValue> getMeasurementTime() {
        return runTime;
    }

    @Override
    public Optional<TimeValue> getWarmupTime() {
        return warmupTime;
    }

    @Override
    public Optional<Integer> getWarmupIterations() {
        return warmupIterations;
    }

    @Override
    public Optional<Integer> getWarmupBatchSize() {
        return warmupBatchSize;
    }

    @Override
    public Optional<Integer> getThreads() {
        return threads;
    }

    @Override
    public Optional<int[]> getThreadGroups() {
        if (threadGroups.isEmpty()) {
            return Optional.none();
        } else {
            int[] r = new int[threadGroups.size()];
            for (int c = 0; c < r.length; c++) {
                r[c] = threadGroups.get(c);
            }
            return Optional.of(r);
        }
    }

    @Override
    public Optional<Boolean> shouldDoGC() {
        return gcEachIteration;
    }

    @Override
    public Optional<Boolean> shouldSyncIterations() {
        return synchIterations;
    }

    @Override
    public Optional<VerboseMode> verbosity() {
        return verbose;
    }

    @Override
    public Optional<TimeUnit> getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Optional<Integer> getOperationsPerInvocation() {
        return opsPerInvocation;
    }

    @Override
    public Optional<Boolean> shouldFailOnError() {
        return failOnError;
    }

    @Override
    public List<Class<? extends Profiler>> getProfilers() {
        return profilers;
    }

    @Override
    public Collection<Mode> getBenchModes() {
        return new HashSet<Mode>(benchMode);
    }

    @Override
    public Optional<TimeValue> getTimeout() {
        return timeout;
    }
}
