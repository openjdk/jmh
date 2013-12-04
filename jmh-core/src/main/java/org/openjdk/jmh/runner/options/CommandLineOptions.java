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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.options.handlers.BenchmarkModeTypeOptionHandler;
import org.openjdk.jmh.runner.options.handlers.BooleanOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ForkOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ProfilersOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ThreadCountsOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ThreadsOptionHandler;
import org.openjdk.jmh.runner.options.handlers.TimeUnitOptionHandler;
import org.openjdk.jmh.runner.options.handlers.TimeValueOptionHandler;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Class that handles all the options and arguments specific to the harness JVM.
 *
 * Boolean/boolean options getters name conventions:
 *   - method name is prefixed by "is" or "should" when the Option class gives exact answer
 *   - method name is prefixed by "get" when the method is just a getter and meaning of the option clarified somewhere else
 *
 * @author anders.astrand@oracle.com
 * @author sergey.kuksenko@oracle.com
 */
public class CommandLineOptions implements Options {

        /*
     *  Conventions for options processing (unless otherwise specified):
     *  - int options:
     *              negative value means unset
     *  - Boolean options:
     *              null means unset, TRUE/FALSE means true/false;
     *              default values should be processed explicitly
     *  - boolean options:
     *              may be used only for options with false default value
     *              may be set to true in cmdLine, can't be set to false explicitly
     *
     */

    @Option(name = "-i", aliases = {"--iterations"}, metaVar = "INT", usage = "Number of iterations.")
    protected int iterations = -1;

    @Option(name = "-r", aliases = {"--runtime"}, metaVar = "TIME", usage = "Run time for each iteration. Examples: 100s, 200ms; defaults to " + Defaults.MEASUREMENT_TIME_SECS + "s", handler = TimeValueOptionHandler.class)
    protected TimeValue runTime = null;

    @Option(name = "-wi", aliases = {"--warmupiterations"}, metaVar = "INT", usage = "Number of warmup iterations to run.")
    protected int warmupIterations = -1;

    @Option(name = "-w", aliases = {"--warmup"}, metaVar = "TIME", usage = "Run time for warmup iterations. Result not used when calculating score. Examples 100s, 200ms; defaults to " + Defaults.WARMUP_TIME_SECS + "", handler = TimeValueOptionHandler.class)
    protected TimeValue warmupTime = null;

    @Option(name = "-bm", aliases = {"--mode"}, multiValued = false, metaVar = "MODE", usage = "Benchmark mode", handler = BenchmarkModeTypeOptionHandler.class)
    protected List<Mode> benchMode = null;

    @Option(name = "-t", aliases = {"--threads"}, usage = "Number of threads to run the microbenchmark with. Special value \"max\" will use Runtime.availableProcessors()", handler = ThreadsOptionHandler.class)
    protected int threads = Integer.MIN_VALUE;

    @Option(name = "-tg", aliases = {"--threadGroups"}, usage = "Thread group distribution", handler = ThreadCountsOptionHandler.class)
    protected List<Integer> threadGroups = new ArrayList<Integer>();

    @Option(name = "-si", aliases = {"--synciterations"}, usage = "Should the harness continue to load each thread with work untill all threads are done with their measured work? Default is " + Defaults.SHOULD_SYNCH_ITERATIONS, handler = BooleanOptionHandler.class)
    protected Boolean synchIterations = null; // true

    @Option(name = "-gc", usage = "Should do System.gc() between iterations?", handler = BooleanOptionHandler.class)
    protected boolean gcEachIteration = false;

    @Option(name = "-v", aliases = {"--verbose"}, usage = "Verbose mode, default off", handler = BooleanOptionHandler.class)
    protected boolean verbose = false;

    @Option(name = "-foe", usage = "Fail the harness on benchmark erro?", handler = BooleanOptionHandler.class)
    protected boolean failOnError = false;

    @Option(name = "-prof", aliases = {"--useprofiler"}, multiValued = false, usage = "Use profilers for collecting additional info, use --listProfilers to list available profilers", handler = ProfilersOptionHandler.class)
    protected Set<ProfilerType> profilers = EnumSet.noneOf(ProfilerType.class);

    @Option(name = "-tu", aliases = {"--timeunit"}, usage = "Output time unit. Available values: m, s, ms, us, ns", handler = TimeUnitOptionHandler.class)
    protected TimeUnit timeUnit = null;

    // test selection options
    @Argument(metaVar = "REGEXP", usage = "Microbenchmarks to run. Regexp filtering out classes or methods which are MicroBenchmarks.")
    protected List<String> regexps = new ArrayList<String>();

    // micro options

    @Option(name = "-f", aliases = {"--fork"}, metaVar = "{ INT }", usage = "Start each benchmark in new JVM, forking from the same JDK unless --jvm is set. Optional parameter specifies number of times harness should fork. Zero forks means \"no fork\", also \"false\" is accepted", handler = ForkOptionHandler.class)
    protected int fork = -1;

    @Option(name = "-wf", aliases = {"--warmupfork"}, metaVar = "{ INT }", usage = "Number of warmup fork executions. (warmup fork execution results are ignored).")
    protected int warmupFork = -1;

    @Option(name = "-o", aliases = {"--output"}, metaVar = "FILE", usage = "Redirect output to FILE")
    protected String output = null;

    @Option(name = "-rff", aliases = {"--result"}, metaVar = "FILE", usage = "Redirect results to FILE")
    protected String result = Defaults.RESULT_FILE;

    @Option(name = "-of", aliases = {"--outputformat"}, metaVar = "FORMAT", usage = "Format to use for output, use --listOutputFormats to list available formats")
    protected OutputFormatType outputFormat = OutputFormatType.defaultType();

    @Option(name = "-rf", aliases = {"--resultformat"}, metaVar = "FORMAT", usage = "Format to use for results, use --listResultFormats to list available formats")
    protected ResultFormatType resultFormat = ResultFormatType.defaultType();

    @Option(name = "--jvm", metaVar = "JVM", usage = "Custom JVM to use with fork.")
    protected String jvm = null;

    @Option(name = "--jvmargs", metaVar = "JVMARGS", usage = "Custom JVM arguments for --jvm, default is to use parent process's arguments")
    protected String jvmArgs = null;

    @Option(name = "--jvmclasspath", metaVar = "CLASSPATH", usage = "Custom classpath for --jvm, default is to use parent process's classpath")
    protected String jvmClassPath = null;

    @Option(name = "-e", aliases = {"--exclude"}, multiValued = true, metaVar = "REGEXP", usage = "Microbenchmarks to exclude. Regexp filtering out classes or methods which are MicroBenchmarks.")
    protected List<String> excludes = new ArrayList<String>();

    @Option(name = "-wm", aliases = {"--warmupmode"}, usage = "Warmup mode for warming up selected micro benchmarks. Warmup modes are: BULK (before all benchmarks), INDI (before each benchmark), BULK_INDI (both)")
    protected WarmupMode warmupMode = WarmupMode.defaultMode();

    @Option(name = "-wmb", aliases = {"--warmupmicrobenchmarks"}, multiValued = true, metaVar = "REGEXP", usage = "Microbenchmarks to run for warmup before running any other benchmarks. These micros may be different from the target micros to warm up the harness or other parts of the JVM prior to running the target micro benchmarks. Regexp filtering out classes or methods which are MicroBenchmarks.")
    protected List<String> warmupMicros = new ArrayList<String>();

    // show something options
    @Option(name = "-l", aliases = {"--list"}, usage = "List available microbenchmarks and exit. Filter using available regexps.")
    protected boolean list = false;

    @Option(name = "--listOutputFormats", usage = "List available output formats")
    protected boolean listOutputFormats = false;

    @Option(name = "--listResultFormats", usage = "List available result formats")
    protected boolean listResultFormats = false;

    @Option(name = "-h", aliases = {"--help"}, usage = "Display help")
    protected boolean help = false;

    @Option(name = "--listProfilers", usage = "List available profilers")
    protected boolean listProfilers = false;

    /**
     * Kawaguchi's parser
     */
    private transient CmdLineParser parser;

    public static CommandLineOptions newInstance() {
        CommandLineOptions opts = new CommandLineOptions();
        opts.parser = new CmdLineParser(opts);
        return opts;
    }

    protected CommandLineOptions() {
    }

    /**
     * Print Usage
     *
     * @param message Message to print at the top
     */
    public void printUsage(String message) {
        System.err.println(message);
        System.err.println("Usage: [options] [benchmark regexp]*");
        parser.printUsage(System.err);
    }

    /**
     * parse arguments and set fields in the Options instance
     *
     * @throws CmdLineException
     */
    public void parseArguments(String[] argv) throws CmdLineException {
        parser.parseArgument(argv);
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public WarmupMode getWarmupMode() {
        return warmupMode;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public List<String> getIncludes() {
        if (!regexps.isEmpty()) {
            return regexps;
        } else {
            // assume user requested all benchmarks
            return Collections.singletonList(".*");
        }
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public List<String> getWarmupIncludes() {
        if (warmupMicros == null) {
            return Collections.emptyList();
        } else {
            return warmupMicros;
        }
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldList() {
        return list;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public String getJvm() {
        return jvm;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public String getJvmArgs() {
        return jvmArgs;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public String getJvmClassPath() {
        return jvmClassPath;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public int getForkCount() {
        return fork;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public int getWarmupForkCount() {
        return warmupFork;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public String getOutput() {
        return output;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public OutputFormatType getOutputFormat() {
        return outputFormat;
    }

    @Override
    public ResultFormatType getResultFormat() {
        return resultFormat;
    }

    @Override
    public String getResult() {
        return result;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldListOutputFormats() {
        return listOutputFormats;
    }

    public boolean shouldListResultFormats() {
        return listResultFormats;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldHelp() {
        return help;
    }


    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldListProfilers() {
        return listProfilers;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public int getMeasurementIterations() {
        return iterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public TimeValue getMeasurementTime() {
        return runTime;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public TimeValue getWarmupTime() {
        return warmupTime;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public int getWarmupIterations() {
        return warmupIterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public int getThreads() {
        return threads;
    }

    @Override
    public int[] getThreadGroups() {
        if (threadGroups.isEmpty()) {
            return new int[] { 1 };
        } else {
            int[] r = new int[threadGroups.size()];
            for (int c = 0; c < r.length; c++) {
                r[c] = threadGroups.get(c);
            }
            return r;
        }
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public boolean shouldDoGC() {
        return gcEachIteration;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public Boolean shouldSyncIterations() {
        return synchIterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Getter
     *
     * @return the value
     */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Should fail the harness on test error?
     * @return the value
     */
    @Override
    public boolean shouldFailOnError() {
        return failOnError;
    }

    /**
     * Getter
     * @return the value
     */
    @Override
    public Set<ProfilerType> getProfilers() {
        return profilers;
    }

    @Override
    public Collection<Mode> getBenchModes() {
        return (benchMode == null) ? null : new HashSet<Mode>(benchMode);
    }


}
