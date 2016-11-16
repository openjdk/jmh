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

import joptsimple.*;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.util.*;
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
    private final List<Mode> benchMode = new ArrayList<>();
    private final Optional<Integer> threads;
    private final List<Integer> threadGroups = new ArrayList<>();
    private final Optional<Boolean> synchIterations;
    private final Optional<Boolean> gcEachIteration;
    private final Optional<VerboseMode> verbose;
    private final Optional<Boolean> failOnError;
    private final List<ProfilerConfig> profilers = new ArrayList<>();
    private final Optional<TimeUnit> timeUnit;
    private final Optional<Integer> opsPerInvocation;
    private final List<String> regexps = new ArrayList<>();
    private final Optional<Integer> fork;
    private final Optional<Integer> warmupFork;
    private final Optional<String> output;
    private final Optional<String> result;
    private final Optional<ResultFormatType> resultFormat;
    private final Optional<String> jvm;
    private final Optional<Collection<String>> jvmArgs;
    private final Optional<Collection<String>> jvmArgsAppend;
    private final Optional<Collection<String>> jvmArgsPrepend;
    private final List<String> excludes = new ArrayList<>();
    private final Optional<WarmupMode> warmupMode;
    private final List<String> warmupMicros = new ArrayList<>();
    private final Multimap<String, String> params = new HashMultimap<>();
    private final boolean list;
    private final boolean listWithParams;
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

        OptionSpec<Integer> optMeasureCount = parser.accepts("i", "Number of measurement iterations to do. " +
                "Measurement iterations are counted towards the benchmark score. " +
                "(default: " + Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT + " for " + Mode.SingleShotTime + ", and " +
                Defaults.MEASUREMENT_ITERATIONS + " for all other modes)")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int");

        OptionSpec<Integer> optMeasureBatchSize = parser.accepts("bs", "Batch size: number of benchmark method " +
                "calls per operation. Some benchmark modes may ignore this setting, please check this separately. " +
                "(default: " + Defaults.MEASUREMENT_BATCHSIZE + ")")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int");

        OptionSpec<TimeValue> optMeasureTime = parser.accepts("r", "Minimum time to spend at each measurement " +
                "iteration. Benchmarks may generally run longer than iteration duration. " +
                "(default: " + Defaults.MEASUREMENT_TIME + ")")
                .withRequiredArg().ofType(TimeValue.class).describedAs("time");

        OptionSpec<Integer> optWarmupCount = parser.accepts("wi", "Number of warmup iterations to do. Warmup " +
                "iterations are not counted towards the benchmark score. " +
                "(default: " + Defaults.WARMUP_ITERATIONS_SINGLESHOT + " for " + Mode.SingleShotTime + ", and " +
                Defaults.WARMUP_ITERATIONS + " for all other modes)")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.NON_NEGATIVE).describedAs("int");

        OptionSpec<Integer> optWarmupBatchSize = parser.accepts("wbs", "Warmup batch size: number of benchmark " +
                "method calls per operation. Some benchmark modes may ignore this setting. " +
                "(default: " + Defaults.WARMUP_BATCHSIZE + ")")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int");

        OptionSpec<TimeValue> optWarmupTime = parser.accepts("w", "Minimum time to spend at each warmup iteration. " +
                "Benchmarks may generally run longer than iteration duration. " +
                "(default: " + Defaults.WARMUP_TIME + ")")
                .withRequiredArg().ofType(TimeValue.class).describedAs("time");

        OptionSpec<TimeValue> optTimeoutTime = parser.accepts("to", "Timeout for benchmark iteration. After reaching " +
                "this timeout, JMH will try to interrupt the running tasks. Non-cooperating benchmarks may ignore " +
                "this timeout. " +
                "(default: " + Defaults.TIMEOUT + ")")
                .withRequiredArg().ofType(TimeValue.class).describedAs("time");

        OptionSpec<Integer> optThreads = parser.accepts("t", "Number of worker threads to run with. 'max' means the " +
                "maximum number of hardware threads available on the machine, figured out by JMH itself. " +
                "(default: " + Defaults.THREADS + ")")
                .withRequiredArg().withValuesConvertedBy(ThreadsValueConverter.INSTANCE).describedAs("int");

        OptionSpec<String> optBenchmarkMode = parser.accepts("bm", "Benchmark mode. Available modes are: " + Mode.getKnown() + ". " +
                "(default: " + Defaults.BENCHMARK_MODE + ")")
                .withRequiredArg().ofType(String.class).withValuesSeparatedBy(',').describedAs("mode");

        OptionSpec<Boolean> optSyncIters = parser.accepts("si", "Should JMH synchronize iterations? This would " +
                "significantly lower the noise in multithreaded tests, by making sure the measured part happens only " +
                "when all workers are running. " +
                "(default: " + Defaults.SYNC_ITERATIONS + ")")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optGC = parser.accepts("gc", "Should JMH force GC between iterations? Forcing the GC may " +
                "help to lower the noise in GC-heavy benchmarks, at the expense of jeopardizing GC ergonomics " +
                "decisions. Use with care. " +
                "(default: " + Defaults.DO_GC + ")")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optFOE = parser.accepts("foe", "Should JMH fail immediately if any benchmark had " +
                "experienced an unrecoverable error? This helps to make quick sanity tests for benchmark suites, as " +
                "well as make the automated runs with checking error codes. " +
                "(default: " + Defaults.FAIL_ON_ERROR + ")")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<String> optVerboseMode = parser.accepts("v", "Verbosity mode. Available modes are: " + Arrays.toString(VerboseMode.values()) + ". " +
                "(default: " + Defaults.VERBOSITY + ")")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> optArgs = parser.nonOptions("Benchmarks to run (regexp+). " +
                "(default: " + Defaults.INCLUDE_BENCHMARKS + ")")
                .describedAs("regexp+");

        OptionSpec<Integer> optForks = parser.accepts("f", "How many times to fork a single benchmark. Use 0 to " +
                "disable forking altogether. Warning: disabling forking may have detrimental impact on benchmark and " +
                "infrastructure reliability, you might want to use different warmup mode instead. " +
                "(default: " + Defaults.MEASUREMENT_FORKS + ")")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.NON_NEGATIVE).describedAs("int");

        OptionSpec<Integer> optWarmupForks = parser.accepts("wf", "How many warmup forks to make for a single benchmark. " +
                "All iterations within the warmup fork are not counted towards the benchmark score. Use 0 to disable " +
                "warmup forks. " +
                "(default: " + Defaults.WARMUP_FORKS + ")")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.NON_NEGATIVE).describedAs("int");

        OptionSpec<String> optOutput = parser.accepts("o", "Redirect human-readable output to a given file.")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<String> optOutputResults = parser.accepts("rff", "Write machine-readable results to a given file. " +
                "The file format is controlled by -rf option. Please see the list of result formats for available " +
                "formats. " +
                "(default: " + Defaults.RESULT_FILE_PREFIX + ".<result-format>)")
                .withRequiredArg().ofType(String.class).describedAs("filename");

        OptionSpec<String> optProfilers = parser.accepts("prof", "Use profilers to collect additional benchmark data. " +
                "Some profilers are not available on all JVMs and/or all OSes. Please see the list of available " +
                "profilers with -lprof.")
                .withRequiredArg().ofType(String.class).describedAs("profiler");

        OptionSpec<Integer> optThreadGroups = parser.accepts("tg", "Override thread group distribution for asymmetric " +
                "benchmarks. This option expects a comma-separated list of thread counts within the group. See " +
                "@Group/@GroupThreads Javadoc for more information.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(Integer.class)
                .withValuesConvertedBy(IntegerValueConverter.NON_NEGATIVE).describedAs("int+");

        OptionSpec<String> optJvm = parser.accepts("jvm", "Use given JVM for runs. This option only affects forked runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgs = parser.accepts("jvmArgs", "Use given JVM arguments. Most options are inherited " +
                "from the host VM options, but in some cases you want to pass the options only to a forked VM. Either " +
                "single space-separated option line, or multiple options are accepted. This option only affects forked " +
                "runs.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsAppend = parser.accepts("jvmArgsAppend", "Same as jvmArgs, but append these " +
                "options before the already given JVM args.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optJvmArgsPrepend = parser.accepts("jvmArgsPrepend", "Same as jvmArgs, but prepend these " +
                "options before the already given JVM arg.")
                .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<String> optTU = parser.accepts("tu", "Override time unit in benchmark results. Available time units " +
                "are: [m, s, ms, us, ns]. " +
                "(default: " + Defaults.OUTPUT_TIMEUNIT + ")")
                .withRequiredArg().ofType(String.class).describedAs("TU");

        OptionSpec<Integer> optOPI = parser.accepts("opi", "Override operations per invocation, see " +
                "@OperationsPerInvocation Javadoc for details. " +
                "(default: " + Defaults.OPS_PER_INVOCATION + ")")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int");

        OptionSpec<String> optResultFormat = parser.accepts("rf", "Format type for machine-readable results. These " +
                "results are written to a separate file (see -rff). See the list of available result formats with -lrf. " +
                "(default: " + Defaults.RESULT_FORMAT +")")
                .withRequiredArg().ofType(String.class).describedAs("type");

        OptionSpec<String> optWarmupMode = parser.accepts("wm", "Warmup mode for warming up selected benchmarks. " +
                "Warmup modes are: " + warmupModesDesc() +
                "(default: " + Defaults.WARMUP_MODE + ")")
                .withRequiredArg().ofType(String.class).describedAs("mode");

        OptionSpec<String> optExcludes = parser.accepts("e", "Benchmarks to exclude from the run.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optParams = parser.accepts("p", "Benchmark parameters. This option is expected to be used " +
                "once per parameter. Parameter name and parameter values should be separated with equals sign. " +
                "Parameter values should be separated with commas.")
                .withRequiredArg().ofType(String.class).describedAs("param={v,}*");

        OptionSpec<String> optWarmupBenchmarks = parser.accepts("wmb", "Warmup benchmarks to include in the run in " +
                "addition to already selected by the primary filters. Harness will not measure these benchmarks, but " +
                "only use them for the warmup.")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class).describedAs("regexp+");

        parser.accepts("l", "List the benchmarks that match a filter, and exit.");
        parser.accepts("lp", "List the benchmarks that match a filter, along with parameters, and exit.");
        parser.accepts("lrf", "List machine-readable result formats, and exit.");
        parser.accepts("lprof", "List profilers, and exit.");
        parser.accepts("h", "Display help, and exit.");

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

            opsPerInvocation = toOptional(optOPI, set);

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
            listWithParams = set.has("lp");
            listResultFormats = set.has("lrf");
            listProfilers = set.has("lprof");

            iterations = toOptional(optMeasureCount, set);
            batchSize = toOptional(optMeasureBatchSize, set);
            runTime = toOptional(optMeasureTime, set);
            warmupIterations = toOptional(optWarmupCount, set);
            warmupBatchSize = toOptional(optWarmupBatchSize, set);
            warmupTime = toOptional(optWarmupTime, set);
            timeout = toOptional(optTimeoutTime, set);
            threads = toOptional(optThreads, set);
            synchIterations = toOptional(optSyncIters, set);
            gcEachIteration = toOptional(optGC, set);
            failOnError = toOptional(optFOE, set);
            fork = toOptional(optForks, set);
            warmupFork = toOptional(optWarmupForks, set);
            output = toOptional(optOutput, set);
            result = toOptional(optOutputResults, set);

            if (set.has(optBenchmarkMode)) {
                try {
                    List<Mode> modes = new ArrayList<>();
                    for (String m : optBenchmarkMode.values(set)) {
                        modes.add(Mode.deepValueOf(m));
                    }
                    benchMode.addAll(modes);
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
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

            if (set.has(optProfilers)) {
                try {
                    for (String m : optProfilers.values(set)) {
                        int idx = m.indexOf(":");
                        String profName = (idx == -1) ?  m : m.substring(0, idx);
                        String params   = (idx == -1) ? "" : m.substring(idx + 1);
                        profilers.add(new ProfilerConfig(profName, params));
                    }
                } catch (IllegalArgumentException iae) {
                    throw new CommandLineOptionException(iae.getMessage(), iae);
                }
            }

            if (set.has(optThreadGroups)) {
                threadGroups.addAll(set.valuesOf(optThreadGroups));
                int total = 0;
                for (int group : threadGroups) {
                    total += group;
                }
                if (total <= 0) {
                    throw new CommandLineOptionException("Group thread count should be positive, but it is " + total);
                }
            }

            jvm = toOptional(optJvm, set);

            jvmArgs = treatQuoted(set, optJvmArgs);
            jvmArgsAppend = treatQuoted(set, optJvmArgsAppend);
            jvmArgsPrepend = treatQuoted(set, optJvmArgsPrepend);

            if (set.hasArgument(optParams)) {
                for (String p : optParams.values(set)) {
                    String[] keys = p.split("=", 2);
                    if (keys.length != 2) {
                        throw new CommandLineOptionException("Unable to parse parameter string \"" + p + "\"");
                    }
                    params.putAll(keys[0], Arrays.asList(keys[1].split(",")));
                }
            }

        } catch (OptionException e) {
            String message = e.getMessage();
            Throwable cause = e.getCause();
            if (cause instanceof ValueConversionException) {
                message += ". " + cause.getMessage();
            }
            throw new CommandLineOptionException(message, e);
        }
    }

    private String warmupModesDesc() {
        StringBuilder sb = new StringBuilder();
        for (WarmupMode mode : WarmupMode.values()) {
            sb.append(mode);
            sb.append(" = ");
            switch (mode) {
                case BULK:
                    sb.append("Warmup all benchmarks first, then do all the measurements. ");
                    break;
                case INDI:
                    sb.append("Warmup each benchmark individually, then measure it. ");
                    break;
                case BULK_INDI:
                    sb.append("Warmup all benchmarks first, then re-warmup each benchmark individually, then measure it. ");
                    break;
            }
        }
        return sb.toString();
    }

    private static <T> Optional<T> toOptional(OptionSpec<T> option, OptionSet set) {
        if (set.has(option)) {
            return Optional.eitherOf(option.value(set));
        }
        return Optional.none();
    }

    public Optional<Collection<String>> treatQuoted(OptionSet set, OptionSpec<String> spec) {
        if (set.hasArgument(spec)) {
            try {
                List<String> vals = spec.values(set);
                if (vals.size() != 1) {
                    return Optional.<Collection<String>>of(vals);
                } else {
                    // Windows launcher somehow ends up here, fall-through to single value treatment
                }
            } catch (OptionException e) {
                // only a single value, fall through
            }
            return Optional.of(Utils.splitQuotedEscape(spec.value(set)));
        }
        return Optional.none();
    }

    public void showHelp() throws IOException {
        parser.printHelpOn(System.err);
    }

    public void listProfilers() {
        ProfilerFactory.listProfilers(System.out);
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

    public boolean shouldListWithParams() {
        return listWithParams;
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
    public List<ProfilerConfig> getProfilers() {
        return profilers;
    }

    @Override
    public Collection<Mode> getBenchModes() {
        return new HashSet<>(benchMode);
    }

    @Override
    public Optional<TimeValue> getTimeout() {
        return timeout;
    }
}
