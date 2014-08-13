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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.link.BinaryLinkServer;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Runner executes JMH benchmarks.
 *
 * <p>This is the entry point for JMH Java API.</p>
 */
public class Runner extends BaseRunner {
    private final BenchmarkList list;
    private int cpuCount;

    /**
     * Create runner with the custom OutputFormat.
     *
     * @param options options to use
     * @param format OutputFormat to use
     */
    public Runner(Options options, OutputFormat format) {
        super(options, format);
        this.list = BenchmarkList.defaultList();
    }

    /**
     * Create Runner with the given options.
     * This method sets up the {@link org.openjdk.jmh.runner.format.OutputFormat} as
     * mandated by options.
     * @param options options to use.
     */
    public Runner(Options options) {
        this(options, createOutputFormat(options));
    }

    private static OutputFormat createOutputFormat(Options options) {
        // sadly required here as the check cannot be made before calling this method in constructor
        if (options == null) {
            throw new IllegalArgumentException("Options not allowed to be null.");
        }
        PrintStream out;
        // setup OutputFormat singleton
        if (options.getOutput().hasValue()) {
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(options.getOutput().get()))));
                System.setOut(out); // override to print everything to file
            } catch (FileNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            out = System.out;
        }

        return OutputFormatFactory.createFormatInstance(out, options.verbosity().orElse(Defaults.VERBOSITY));
    }

    /**
     * Print matching benchmarks into output.
     */
    public void list() {
        Set<BenchmarkListEntry> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        out.println("Benchmarks: ");
        for (BenchmarkListEntry benchmark : benchmarks) {
            out.println(benchmark.getUsername());
        }
    }

    /**
     * Shortcut method for the single benchmark execution.
     * This method is handy when Options describe only the single benchmark to run.
     *
     * @return benchmark result
     * @throws RunnerException if more than one benchmark is found, or no results are returned
     */
    public RunResult runSingle() throws RunnerException {
        Set<BenchmarkListEntry> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.size() == 1) {
            Collection<RunResult> values = run();
            if (values.size() == 1) {
                return values.iterator().next();
            } else {
                throw new RunnerException("No results returned");
            }
        } else {
            throw new RunnerException("More than single benchmark is matching the options");
        }
    }

    /**
     * Run benchmarks.
     *
     * @return map of benchmark results
     * @throws org.openjdk.jmh.runner.RunnerException if something goes wrong
     */
    public Collection<RunResult> run() throws RunnerException {
        for (Class<? extends Profiler> p : options.getProfilers()) {
            List<String> initMessages = new ArrayList<String>();
            if (!ProfilerFactory.checkSupport(p, initMessages)) {
                StringBuilder sb = new StringBuilder();
                for (String im : initMessages) {
                    sb.append(String.format("%5s %s\n", "", im));
                }
                throw new RunnerException("The requested profiler (" + p.getName() + ") is not supported: \n" + sb.toString());
            }
        }

        SortedSet<BenchmarkListEntry> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.isEmpty()) {
            out.flush();
            out.close();
            throw new NoBenchmarksException();
        }

        // override the benchmark types;
        // this may yield new benchmark records
        if (!options.getBenchModes().isEmpty()) {
            List<BenchmarkListEntry> newBenchmarks = new ArrayList<BenchmarkListEntry>();
            for (BenchmarkListEntry br : benchmarks) {
                for (Mode m : options.getBenchModes()) {
                    newBenchmarks.add(br.cloneWith(m));
                }

            }

            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }

        // clone with all the modes
        {
            List<BenchmarkListEntry> newBenchmarks = new ArrayList<BenchmarkListEntry>();
            for (BenchmarkListEntry br : benchmarks) {
                if (br.getMode() == Mode.All) {
                    for (Mode mode : Mode.values()) {
                        if (mode == Mode.All) continue;
                        newBenchmarks.add(br.cloneWith(mode));
                    }
                } else {
                    newBenchmarks.add(br);
                }
            }

            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }

        // clone with all parameters
        {
            List<BenchmarkListEntry> newBenchmarks = new ArrayList<BenchmarkListEntry>();
            for (BenchmarkListEntry br : benchmarks) {
                if (br.getParams().hasValue()) {
                    for (WorkloadParams p : explodeAllParams(br)) {
                        newBenchmarks.add(br.cloneWith(p));
                    }
                } else {
                    newBenchmarks.add(br);
                }
            }
            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }

        Collection<RunResult> results = runBenchmarks(benchmarks);

        out.flush();
        out.close();

        // If user requested the result file in one way or the other, write it out.
        if (options.getResult().hasValue() || options.getResultFormat().hasValue()) {
            ResultFormatFactory.getInstance(
                        options.getResultFormat().orElse(Defaults.RESULT_FORMAT),
                        options.getResult().orElse(Defaults.RESULT_FILE)
            ).writeOut(results);
        }

        return results;
    }

    private List<ActionPlan> getActionPlans(Set<BenchmarkListEntry> benchmarks) {
        ActionPlan base = new ActionPlan(ActionType.FORKED);

        LinkedHashSet<BenchmarkListEntry> warmupBenches = new LinkedHashSet<BenchmarkListEntry>();

        List<String> warmupMicrosRegexp = options.getWarmupIncludes();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupBenches.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isBulk()) {
            warmupBenches.addAll(benchmarks);
        }

        for (BenchmarkListEntry wr : warmupBenches) {
            base.add(newAction(wr, ActionMode.WARMUP));
        }

        ActionPlan embeddedPlan = new ActionPlan(ActionType.EMBEDDED);
        embeddedPlan.mixIn(base);

        boolean addEmbedded = false;

        List<ActionPlan> result = new ArrayList<ActionPlan>();
        for (BenchmarkListEntry br : benchmarks) {
            BenchmarkParams params = newBenchmarkParams(br, ActionMode.UNDEF);

            if (params.getForks() <= 0) {
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi()) {
                    embeddedPlan.add(newAction(br, ActionMode.WARMUP_MEASUREMENT));
                } else {
                    embeddedPlan.add(newAction(br, ActionMode.MEASUREMENT));
                }
                addEmbedded = true;
            }

            if (params.getForks() > 0) {
                ActionPlan r = new ActionPlan(ActionType.FORKED);
                r.mixIn(base);
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi()) {
                    r.add(newAction(br, ActionMode.WARMUP_MEASUREMENT));
                } else {
                    r.add(newAction(br, ActionMode.MEASUREMENT));
                }
                result.add(r);
            }
        }

        if (addEmbedded) {
            result.add(embeddedPlan);
        }

        return result;
    }

    private Action newAction(BenchmarkListEntry br, ActionMode mode) {
        return new Action(newBenchmarkParams(br, mode), mode);
    }

    private BenchmarkParams newBenchmarkParams(BenchmarkListEntry benchmark, ActionMode mode) {
        int[] threadGroups = options.getThreadGroups().orElse(benchmark.getThreadGroups());

        int threads = options.getThreads().orElse(
                benchmark.getThreads().orElse(
                        Defaults.THREADS));

        if (threads == Threads.MAX) {
            if (cpuCount == 0) {
                out.print("# Detecting actual CPU count: ");
                cpuCount = Utils.figureOutHotCPUs();
                out.println(cpuCount + " detected");
            }
            threads = cpuCount;
        }

        threads = Utils.roundUp(threads, Utils.sum(threadGroups));

        boolean synchIterations = (benchmark.getMode() != Mode.SingleShotTime) &&
                options.shouldSyncIterations().orElse(Defaults.SYNC_ITERATIONS);

        IterationParams measurement = mode.doMeasurement() ?
                new IterationParams(
                        IterationType.MEASUREMENT,
                        options.getMeasurementIterations().orElse(
                                benchmark.getMeasurementIterations().orElse(
                                        (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT : Defaults.MEASUREMENT_ITERATIONS
                                )),
                        options.getMeasurementTime().orElse(
                                benchmark.getMeasurementTime().orElse(
                                        (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.MEASUREMENT_TIME
                                )),
                        options.getMeasurementBatchSize().orElse(
                                benchmark.getMeasurementBatchSize().orElse(
                                        Defaults.MEASUREMENT_BATCHSIZE
                                )
                        )
                ) :
                new IterationParams(IterationType.MEASUREMENT, 0, TimeValue.NONE, 1);

        IterationParams warmup = mode.doWarmup() ?
                new IterationParams(
                        IterationType.WARMUP,
                        options.getWarmupIterations().orElse(
                                benchmark.getWarmupIterations().orElse(
                                        (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.WARMUP_ITERATIONS_SINGLESHOT : Defaults.WARMUP_ITERATIONS
                                )),
                        options.getWarmupTime().orElse(
                                benchmark.getWarmupTime().orElse(
                                        (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.WARMUP_TIME
                                )),
                        options.getWarmupBatchSize().orElse(
                                benchmark.getWarmupBatchSize().orElse(
                                        Defaults.WARMUP_BATCHSIZE
                                )
                        )
                ) :
                new IterationParams(IterationType.WARMUP, 0, TimeValue.NONE, 1);

        int forks = options.getForkCount().orElse(
                benchmark.getForks().orElse(
                        Defaults.MEASUREMENT_FORKS));

        int warmupForks = options.getWarmupForkCount().orElse(
                benchmark.getWarmupForks().orElse(
                        Defaults.WARMUP_FORKS));

        TimeUnit timeUnit = options.getTimeUnit().orElse(
                benchmark.getTimeUnit().orElse(
                        Defaults.OUTPUT_TIMEUNIT));

        int opsPerInvocation = options.getOperationsPerInvocation().orElse(
                benchmark.getOperationsPerInvocation().orElse(
                        Defaults.OPS_PER_INVOCATION));

        String jvm = options.getJvm().orElse(
                benchmark.getJvm().orElse(Utils.getCurrentJvm()));

        Collection<String> jvmArgs = new ArrayList<String>();

        jvmArgs.addAll(options.getJvmArgsPrepend().orElse(
                benchmark.getJvmArgsPrepend().orElse(Collections.<String>emptyList())));

        jvmArgs.addAll(options.getJvmArgs().orElse(
                benchmark.getJvmArgs().orElse(ManagementFactory.getRuntimeMXBean().getInputArguments())));

        jvmArgs.addAll(options.getJvmArgsAppend().orElse(
                benchmark.getJvmArgsAppend().orElse(Collections.<String>emptyList())));

        return new BenchmarkParams(benchmark.getUsername(), benchmark.generatedTarget(), synchIterations,
                threads, threadGroups, forks, warmupForks,
                warmup, measurement, benchmark.getMode(), benchmark.getWorkloadParams(), timeUnit, opsPerInvocation,
                jvm, jvmArgs);
    }

    private List<WorkloadParams> explodeAllParams(BenchmarkListEntry br) throws RunnerException {
        Map<String, String[]> benchParams = br.getParams().orElse(Collections.<String, String[]>emptyMap());
        List<WorkloadParams> ps = new ArrayList<WorkloadParams>();
        for (String k : benchParams.keySet()) {
            Collection<String> values = options.getParameter(k).orElse(Arrays.asList(benchParams.get(k)));
            if (values.isEmpty()) {
                throw new RunnerException("Benchmark \"" + br.getUsername() +
                        "\" defines the parameter \"" + k + "\", but no default values.\n" +
                        "Define the default values within the annotation, or provide the parameter values at runtime.");
            }
            if (ps.isEmpty()) {
                int idx = 0;
                for (String v : values) {
                    WorkloadParams al = new WorkloadParams();
                    al.put(k, v, idx);
                    ps.add(al);
                    idx++;
                }
            } else {
                List<WorkloadParams> newPs = new ArrayList<WorkloadParams>();
                for (WorkloadParams p : ps) {
                    int idx = 0;
                    for (String v : values) {
                        WorkloadParams al = p.copy();
                        al.put(k, v, idx);
                        newPs.add(al);
                        idx++;
                    }
                }
                ps = newPs;
            }
        }
        return ps;
    }

    private Collection<RunResult> runBenchmarks(SortedSet<BenchmarkListEntry> benchmarks) throws RunnerException {
        out.startRun();

        Multimap<BenchmarkParams, BenchmarkResult> results = new TreeMultimap<BenchmarkParams, BenchmarkResult>();
        List<ActionPlan> plan = getActionPlans(benchmarks);

        etaBeforeBenchmarks(plan);

        try {
            for (ActionPlan r : plan) {
                Multimap<BenchmarkParams, BenchmarkResult> res;
                switch (r.getType()) {
                    case EMBEDDED:
                        res = runBenchmarks(false, r);
                        break;
                    case FORKED:
                        res = runSeparate(r);
                        break;
                    default:
                        throw new IllegalStateException("Unknown action plan type: " + r.getType());
                }

                for (BenchmarkParams br : res.keys()) {
                    results.putAll(br, res.get(br));
                }
            }

            etaAfterBenchmarks();

            SortedSet<RunResult> runResults = mergeRunResults(results);
            out.endRun(runResults);
            return runResults;
        } catch (BenchmarkException be) {
            throw new RunnerException("Benchmark caught the exception", be.getCause());
        }
    }

    private SortedSet<RunResult> mergeRunResults(Multimap<BenchmarkParams, BenchmarkResult> results) {
        SortedSet<RunResult> result = new TreeSet<RunResult>(RunResult.DEFAULT_SORT_COMPARATOR);
        for (BenchmarkParams key : results.keys()) {
            result.add(new RunResult(results.get(key)));
        }
        return result;
    }

    private Multimap<BenchmarkParams, BenchmarkResult> runSeparate(ActionPlan actionPlan) {
        Multimap<BenchmarkParams, BenchmarkResult> results = new HashMultimap<BenchmarkParams, BenchmarkResult>();

        if (actionPlan.getMeasurementActions().size() != 1) {
            throw new IllegalStateException("Expect only single benchmark in the action plan, but was " + actionPlan.getMeasurementActions().size());
        }

        BinaryLinkServer server = null;
        try {
            server = new BinaryLinkServer(options, out);

            server.setPlan(actionPlan);

            BenchmarkParams params = actionPlan.getMeasurementActions().get(0).getParams();

            boolean printOut = true;
            boolean printErr = true;
            List<ExternalProfiler> profilers = new ArrayList<ExternalProfiler>();

            List<String> javaInvokeOptions = new ArrayList<String>();
            List<String> javaOptions = new ArrayList<String>();
            for (Class<? extends Profiler> p : options.getProfilers()) {
                if (!ProfilerFactory.isExternal(p)) continue;
                ExternalProfiler prof = (ExternalProfiler) ProfilerFactory.prepareProfiler(p, null);
                profilers.add(prof);
                javaInvokeOptions.addAll(prof.addJVMInvokeOptions(params));
                javaOptions.addAll(prof.addJVMOptions(params));
                printOut &= prof.allowPrintOut();
                printErr &= prof.allowPrintErr();
            }

            boolean forcePrint = options.verbosity().orElse(Defaults.VERBOSITY).equalsOrHigherThan(VerboseMode.EXTRA);
            printOut = forcePrint || printOut;
            printErr = forcePrint || printErr;

            String[] commandString = getSeparateExecutionCommand(params, server.getHost(), server.getPort(), javaInvokeOptions, javaOptions);
            String opts = Utils.join(params.getJvmArgs(), " ");
            if (opts.trim().isEmpty()) {
                opts = "<none>";
            }

            out.println("# VM invoker: " + params.getJvm());
            out.println("# VM options: " + opts);
            out.startBenchmark(params);
            out.println("");

            int forkCount = params.getForks();
            int warmupForkCount = params.getWarmupForks();
            if (warmupForkCount > 0) {
                out.verbosePrintln("Warmup forking " + warmupForkCount + " times using command: " + Arrays.toString(commandString));
                for (int i = 0; i < warmupForkCount; i++) {
                    etaBeforeBenchmark();
                    out.println("# Warmup Fork: " + (i + 1) + " of " + warmupForkCount);

                    File stdErr = FileUtils.tempFile("stderr");
                    File stdOut = FileUtils.tempFile("stdout");

                    doFork(server, commandString, stdOut, stdErr, printOut, printErr);

                    etaAfterBenchmark(params);
                    out.println("");
                }
            }

            out.verbosePrintln("Forking " + forkCount + " times using command: " + Arrays.toString(commandString));
            for (int i = 0; i < forkCount; i++) {
                etaBeforeBenchmark();
                out.println("# Fork: " + (i + 1) + " of " + forkCount);

                File stdErr = FileUtils.tempFile("stderr");
                File stdOut = FileUtils.tempFile("stdout");

                if (!profilers.isEmpty()) {
                    out.print("# Preparing profilers: ");
                    for (ExternalProfiler profiler : profilers) {
                        out.print(profiler.label() + " ");
                        profiler.beforeTrial(params);
                    }
                    out.println("");

                    if (!printOut) {
                        out.println("# Profilers consume stdout from target VM, use -v " + VerboseMode.EXTRA + " to copy to console");
                    }
                    if (!printErr) {
                        out.println("# Profilers consume stderr from target VM, use -v " + VerboseMode.EXTRA + " to copy to console");
                    }
                }

                Multimap<BenchmarkParams, BenchmarkResult> result = doFork(server, commandString, stdOut, stdErr, printOut, printErr);

                if (!profilers.isEmpty()) {
                    out.print("# Processing profiler results: ");
                    for (ExternalProfiler profiler : profilers) {
                        out.print(profiler.label() + " ");
                        for (Result profR : profiler.afterTrial(params, stdOut, stdErr)) {
                            for (BenchmarkResult r : result.values()) {
                                r.addBenchmarkResult(profR);
                            }
                        }
                    }
                    out.println("");
                }

                results.merge(result);
                etaAfterBenchmark(params);
                out.println("");
            }

            out.endBenchmark(new RunResult(results.get(params)).getAggregatedResult());

        } catch (IOException e) {
            results.clear();
            throw new BenchmarkException(e);
        } catch (BenchmarkException e) {
            results.clear();
            if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR)) {
                out.println("Benchmark had encountered error, and fail on error was requested");
                throw e;
            }
        } finally {
            if (server != null) {
                server.terminate();
            }
        }

        return results;
    }

    private Multimap<BenchmarkParams, BenchmarkResult> doFork(BinaryLinkServer reader, String[] commandString,
                                                              File stdOut, File stdErr, boolean printOut, boolean printErr) {
        FileOutputStream fosErr = null;
        FileOutputStream fosOut = null;
        try {
            Process p = Runtime.getRuntime().exec(commandString);

            fosErr = new FileOutputStream(stdErr);
            fosOut = new FileOutputStream(stdOut);

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fosErr);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fosOut);

            if (printErr) {
                errDrainer.addOutputStream(System.err);
            }

            if (printOut) {
                outDrainer.addOutputStream(System.out);
            }

            errDrainer.start();
            outDrainer.start();

            int ecode = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            // need to wait for all pending messages to be processed
            // before starting the next benchmark
            reader.waitFinish();

            if (ecode != 0) {
                out.println("<forked VM failed with exit code " + ecode + ">");
                out.println("<stdout last='10 lines'>");
                for (String l : FileUtils.tail(stdOut, 10)) {
                    out.println(l);
                }
                out.println("</stdout>");
                out.println("<stderr last='10 lines'>");
                for (String l : FileUtils.tail(stdErr, 10)) {
                    out.println(l);
                }
                out.println("</stderr>");

                out.println("");

                throw new BenchmarkException(new IllegalStateException("Forked VM failed with exit code " + ecode));
            }

        } catch (IOException ex) {
            throw new BenchmarkException(ex);
        } catch (InterruptedException ex) {
            throw new BenchmarkException(ex);
        } finally {
            FileUtils.safelyClose(fosErr);
            FileUtils.safelyClose(fosOut);
        }

        BenchmarkException exception = reader.getException();
        if (exception == null) {
            return reader.getResults();
        } else {
            throw exception;
        }
    }

    /**
     * Helper method for assembling the command to execute the forked JVM with
     *
     * @param benchmark benchmark to execute
     * @param host host VM host
     * @param port host VM port
     * @param javaInvokeOptions prepend these commands before JVM invocation
     * @param javaOptions add these options to JVM command string
     * @return the final command to execute
     */
    String[] getSeparateExecutionCommand(BenchmarkParams benchmark, String host, int port, List<String> javaInvokeOptions, List<String> javaOptions) {

        List<String> command = new ArrayList<String>();

        // prefix java invoke options, if any profiler wants it
        command.addAll(javaInvokeOptions);

        // use supplied jvm, if given
        command.add(benchmark.getJvm());

        // use supplied jvm args, if given
        command.addAll(benchmark.getJvmArgs());

        // add profiler JVM commands, if any profiler wants it
        command.addAll(javaOptions);

        // add any compiler oracle hints
        CompilerHints.addCompilerHints(command);

        // assemble final process command
        command.add("-cp");
        if (Utils.isWindows()) {
            command.add('"' + System.getProperty("java.class.path") + '"');
        } else {
            command.add(System.getProperty("java.class.path"));
        }
        command.add(ForkedMain.class.getName());

        // Forked VM assumes the exact order of arguments:
        //   1) host name to back-connect
        //   2) host port to back-connect
        command.add(host);
        command.add(String.valueOf(port));

        return command.toArray(new String[command.size()]);
    }

}
