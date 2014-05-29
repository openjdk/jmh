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

import org.openjdk.jmh.ForkedMain;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.link.BinaryLinkServer;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.output.format.OutputFormatFactory;
import org.openjdk.jmh.output.results.ResultFormat;
import org.openjdk.jmh.output.results.ResultFormatFactory;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.Profiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.NullOutputStream;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.TreeMultimap;

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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runner frontend class. Responsible for running micro benchmarks in this JVM.
 */
public class Runner extends BaseRunner {
    private final MicroBenchmarkList list;

    /**
     * THIS IS AN EXPERIMENTAL API.
     *
     * Create runner with the custom OutputFormat
     * @param options options to use
     * @param format OutputFormat to use
     */
    public Runner(Options options, OutputFormat format) {
        super(options, format);
        this.list = MicroBenchmarkList.defaultList();
    }

    /**
     * Create Runner with the given options.
     * @param options options to use.
     */
    public Runner(Options options) {
        this(options, createOutputFormat(options));
    }

    /** Setup helper method, creates OutputFormat according to argv options. */
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
                Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                throw new IllegalStateException(ex);
            }
        } else {
            out = System.out;
        }

        return OutputFormatFactory.createFormatInstance(out, options.verbosity().orElse(Defaults.VERBOSITY));
    }

    public void list() {
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        out.println("Benchmarks: ");
        for (BenchmarkRecord benchmark : benchmarks) {
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
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.size() == 1) {
            Map<BenchmarkRecord, RunResult> rs = run();
            Collection<RunResult> values = rs.values();
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
    public SortedMap<BenchmarkRecord, RunResult> run() throws RunnerException {
        for (Class<? extends Profiler> p : options.getProfilers()) {
            Collection<String> initMessages = ProfilerFactory.checkSupport(p);
            if (!initMessages.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String im : initMessages) {
                    sb.append(String.format("%5s %s\n", "", im));
                }
                throw new RunnerException("The requested profiler (" + p.getName() + ") is not supported: \n" + sb.toString());
            }
        }

        SortedSet<BenchmarkRecord> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.isEmpty()) {
            out.flush();
            out.close();
            throw new NoBenchmarksException();
        }

        // override the benchmark types;
        // this may yield new benchmark records
        if (!options.getBenchModes().isEmpty()) {
            List<BenchmarkRecord> newBenchmarks = new ArrayList<BenchmarkRecord>();
            for (BenchmarkRecord br : benchmarks) {
                for (Mode m : options.getBenchModes()) {
                    newBenchmarks.add(br.cloneWith(m));
                }

            }

            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }

        // clone with all the modes
        {
            List<BenchmarkRecord> newBenchmarks = new ArrayList<BenchmarkRecord>();
            for (BenchmarkRecord br : benchmarks) {
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
            List<BenchmarkRecord> newBenchmarks = new ArrayList<BenchmarkRecord>();
            for (BenchmarkRecord br : benchmarks) {
                if (br.getParams().hasValue()) {
                    for (ActualParams p : explodeAllParams(br)) {
                        newBenchmarks.add(br.cloneWith(p));
                    }
                } else {
                    newBenchmarks.add(br);
                }
            }
            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }

        SortedMap<BenchmarkRecord, RunResult> results = runBenchmarks(benchmarks);

        out.flush();
        out.close();

        ResultFormatType resultFormatType = options.getResultFormat().orElse(Defaults.RESULT_FORMAT);
        String resultFormatFile = options.getResult().orElse(Defaults.RESULT_FILE);
        ResultFormat resultFormat = ResultFormatFactory.getInstance(resultFormatType, resultFormatFile);
        resultFormat.writeOut(results);

        return results;
    }

    private List<ActionPlan> getActionPlans(Set<BenchmarkRecord> benchmarks) {
        ActionPlan base = new ActionPlan(ActionType.FORKED);

        LinkedHashSet<BenchmarkRecord> warmupBenches = new LinkedHashSet<BenchmarkRecord>();

        List<String> warmupMicrosRegexp = options.getWarmupIncludes();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupBenches.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isBulk()) {
            warmupBenches.addAll(benchmarks);
        }

        for (BenchmarkRecord wr : warmupBenches) {
            base.addWarmup(wr);
        }

        ActionPlan embeddedPlan = new ActionPlan(ActionType.EMBEDDED);
        embeddedPlan.mixIn(base);

        boolean addEmbedded = false;

        List<ActionPlan> result = new ArrayList<ActionPlan>();
        for (BenchmarkRecord br : benchmarks) {
            BenchmarkParams params = new BenchmarkParams(out, options, br, ActionMode.UNDEF);

            if (params.getForks() <= 0) {
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi()) {
                    embeddedPlan.addWarmupMeasurement(br);
                } else {
                    embeddedPlan.addMeasurement(br);
                }
                addEmbedded = true;
            }

            if (params.getForks() > 0) {
                ActionPlan r = new ActionPlan(ActionType.FORKED);
                r.mixIn(base);
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi()) {
                    r.addWarmupMeasurement(br);
                } else {
                    r.addMeasurement(br);
                }
                result.add(r);
            }
        }

        if (addEmbedded) {
            result.add(embeddedPlan);
        }

        return result;
    }

    private List<ActualParams> explodeAllParams(BenchmarkRecord br) throws RunnerException {
        Map<String, String[]> benchParams = br.getParams().orElse(Collections.<String, String[]>emptyMap());
        List<ActualParams> ps = new ArrayList<ActualParams>();
        for (String k : benchParams.keySet()) {
            Collection<String> values = options.getParameter(k).orElse(Arrays.asList(benchParams.get(k)));
            if (values.isEmpty()) {
                throw new RunnerException("Benchmark \"" + br.getUsername() +
                        "\" defines the parameter \"" + k + "\", but its values are ambiguous.\n" +
                        "Define the default values within the annotation, or provide the parameter values in runtime.");
            }
            if (ps.isEmpty()) {
                int idx = 0;
                for (String v : values) {
                    ActualParams al = new ActualParams();
                    al.put(k, v, idx);
                    ps.add(al);
                    idx++;
                }
            } else {
                List<ActualParams> newPs = new ArrayList<ActualParams>();
                for (ActualParams p : ps) {
                    int idx = 0;
                    for (String v : values) {
                        ActualParams al = p.copy();
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



    private SortedMap<BenchmarkRecord, RunResult> runBenchmarks(SortedSet<BenchmarkRecord> benchmarks) throws RunnerException {
        out.startRun();

        Multimap<BenchmarkRecord, BenchResult> results = new TreeMultimap<BenchmarkRecord, BenchResult>();
        List<ActionPlan> plan = getActionPlans(benchmarks);

        beforeBenchmarks(plan);

        try {
            for (ActionPlan r : plan) {
                Multimap<BenchmarkRecord, BenchResult> res;
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

                for (BenchmarkRecord br : res.keys()) {
                    results.putAll(br, res.get(br));
                }
            }

            afterBenchmarks();

            SortedMap<BenchmarkRecord, RunResult> runResults = mergeRunResults(results);
            out.endRun(runResults);
            return runResults;
        } catch (BenchmarkException be) {
            throw new RunnerException("Benchmark caught the exception", be.getCause());
        }
    }

    private SortedMap<BenchmarkRecord, RunResult> mergeRunResults(Multimap<BenchmarkRecord, BenchResult> results) {
        SortedMap<BenchmarkRecord, RunResult> result = new TreeMap<BenchmarkRecord, RunResult>();
        for (BenchmarkRecord key : results.keys()) {
            Collection<BenchResult> rs = results.get(key);
            result.put(key, new RunResult(rs));
        }
        return result;
    }

    private Multimap<BenchmarkRecord, BenchResult> runSeparate(ActionPlan actionPlan) {
        Multimap<BenchmarkRecord, BenchResult> results = new HashMultimap<BenchmarkRecord, BenchResult>();

        if (actionPlan.getMeasurementActions().size() != 1) {
            throw new IllegalStateException("Expect only single benchmark in the action plan, but was " + actionPlan.getMeasurementActions().size());
        }

        BinaryLinkServer server = null;
        try {
            server = new BinaryLinkServer(options, out);

            server.setPlan(actionPlan);

            BenchmarkRecord benchmark = actionPlan.getMeasurementActions().get(0).getBenchmark();

            String[] commandString = getSeparateExecutionCommand(benchmark, server.getHost(), server.getPort());
            String opts = merge(getJvmArgs(benchmark));
            if (opts.trim().isEmpty()) {
                opts = "<none>";
            }

            String jvm = options.getJvm().orElse(getDefaultJvm());

            BenchmarkParams params = new BenchmarkParams(out, options, benchmark, ActionMode.UNDEF);

            int forkCount = params.getForks();
            int warmupForkCount = params.getWarmupForks();
            if (warmupForkCount > 0) {
                out.verbosePrintln("Warmup forking " + warmupForkCount + " times using command: " + Arrays.toString(commandString));
                for (int i = 0; i < warmupForkCount; i++) {
                    beforeBenchmark();
                    out.println("# VM invoker: " + jvm);
                    out.println("# VM options: " + opts);
                    out.println("# Warmup Fork: " + (i + 1) + " of " + warmupForkCount);
                    doFork(server, commandString);
                    afterBenchmark(benchmark);
                }
            }

            out.verbosePrintln("Forking " + forkCount + " times using command: " + Arrays.toString(commandString));
            for (int i = 0; i < forkCount; i++) {
                beforeBenchmark();
                out.println("# VM invoker: " + jvm);
                out.println("# VM options: " + opts);
                out.println("# Fork: " + (i + 1) + " of " + forkCount);
                Multimap<BenchmarkRecord, BenchResult> result = doFork(server, commandString);
                results.merge(result);
                afterBenchmark(benchmark);
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (server != null) {
                server.terminate();
            }
        }

        return results;
    }

    private String merge(Collection<String> ss) {
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }

    private Multimap<BenchmarkRecord, BenchResult> doFork(BinaryLinkServer reader, String[] commandString) {
        try {
            Process p = Runtime.getRuntime().exec(commandString);

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), new NullOutputStream());
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), new NullOutputStream());

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
                out.println("");
                if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR)) {
                    throw new BenchmarkException(
                        new IllegalStateException("Forked VM failed with exit code " + ecode)
                    );
                }
            }

        } catch (IOException ex) {
            throw new BenchmarkException(ex);
        } catch (InterruptedException ex) {
            throw new BenchmarkException(ex);
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
     *
     * @param benchmark benchmark to execute
     * @param host host VM host
     * @param port host VM port
     * @return the final command to execute
     */
    public String[] getSeparateExecutionCommand(BenchmarkRecord benchmark, String host, int port) {

        List<String> command = new ArrayList<String>();

        // use supplied jvm, if given
        command.add(options.getJvm().orElse(getDefaultJvm()));

        // use supplied jvm args, if given
        command.addAll(getJvmArgs(benchmark));

        // add any compiler oracle hints
        CompilerHints.addCompilerHints(command);

        // assemble final process command
        command.add("-cp");
        if (isWindows()) {
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

    private boolean isWindows() {
        return System.getProperty("os.name").contains("indows");
    }

    private String getDefaultJvm() {
        StringBuilder javaExecutable = new StringBuilder();
        javaExecutable.append(System.getProperty("java.home"));
        javaExecutable.append(File.separator);
        javaExecutable.append("bin");
        javaExecutable.append(File.separator);
        javaExecutable.append("java");
        javaExecutable.append(isWindows() ? ".exe" : "");
        return javaExecutable.toString();
    }

    private Collection<String> getJvmArgs(BenchmarkRecord benchmark) {
        Collection<String> res = new ArrayList<String>();
        res.addAll(options.getJvmArgsPrepend().orElse(benchmark.getJvmArgsPrepend().orElse(Collections.<String>emptyList())));
        res.addAll(options.getJvmArgs().orElse(benchmark.getJvmArgs().orElse(ManagementFactory.getRuntimeMXBean().getInputArguments())));
        res.addAll(options.getJvmArgsAppend().orElse(benchmark.getJvmArgsAppend().orElse(Collections.<String>emptyList())));
        return res;
    }

}
