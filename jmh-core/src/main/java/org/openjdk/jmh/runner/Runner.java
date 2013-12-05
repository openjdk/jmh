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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.link.BinaryLinkServer;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.OutputFormatFactory;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.output.results.ResultFormat;
import org.openjdk.jmh.output.results.ResultFormatFactory;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.util.AnnotationUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
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
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runner frontend class. Responsible for running micro benchmarks in this JVM.
 *
 * @author staffan.friberg@oracle.com,
 * @author anders.astrand@oracle.com
 * @author sergey.kuksenko@oracle.com
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
        PrintStream out;
        // setup OutputFormat singleton
        if (options.getOutput() == null) {
            out = System.out;
        } else {
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(options.getOutput()))));
                System.setOut(out); // override to print everything to file
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                throw new IllegalStateException(ex);
            }
        }

        return OutputFormatFactory.createFormatInstance(out, options.getOutputFormat(), options.isVerbose());
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
     * @throws IllegalStateException if more than one benchmark is found
     */
    public RunResult runSingle() throws RunnerException {
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.size() == 1) {
            Map<BenchmarkRecord, RunResult> rs = run();
            return rs.values().iterator().next();
        } else {
            throw new IllegalStateException("More than single benchmark is matching the options");
        }
    }

    /**
     * Run benchmarks.
     *
     * @return map of benchmark results
     */
    public Map<BenchmarkRecord, RunResult> run() throws RunnerException {
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.isEmpty()) {
            out.println("No matching benchmarks. Miss-spelled regexp? Use -v for verbose output.");
            out.flush();
            out.close();
            return null;
        }

        // list microbenchmarks if -v
        if (options.isVerbose()) {
            list();
        }

        // override the benchmark types;
        // this may yield new benchmark records
        if (options.getBenchModes() != null) {
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

        Map<BenchmarkRecord, RunResult> results = runBenchmarks(benchmarks);

        out.flush();
        out.close();

        ResultFormat resultFormat = ResultFormatFactory.getInstance(options.getResultFormat(), options.getResult());
        resultFormat.writeOut(results);

        return results;
    }

    private ActionPlan getEmbeddedActionPlan(Set<BenchmarkRecord> benchmarks) {
        ActionPlan r = new ActionPlan();

        LinkedHashSet<BenchmarkRecord> warmupBenches = new LinkedHashSet<BenchmarkRecord>();

        List<String> warmupMicrosRegexp = options.getWarmupIncludes();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupBenches.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode().isBulk()) {
            warmupBenches.addAll(benchmarks);
        }

        for (BenchmarkRecord wr : warmupBenches) {
            r.addWarmup(wr);
        }

        for (BenchmarkRecord br : benchmarks) {
            BenchmarkParams params = new BenchmarkParams(options, br, ActionMode.UNDEF);
            if (params.getForks() <= 0) {
                if (options.getWarmupMode().isIndi()) {
                    r.addWarmupMeasurement(br);
                } else {
                    r.addMeasurement(br);
                }
            }
        }

        return r;
    }

    private Set<ActionPlan> getForkedActionPlans(Set<BenchmarkRecord> benchmarks) {
        ActionPlan base = new ActionPlan();

        LinkedHashSet<BenchmarkRecord> warmupBenches = new LinkedHashSet<BenchmarkRecord>();

        List<String> warmupMicrosRegexp = options.getWarmupIncludes();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupBenches.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode().isBulk()) {
            warmupBenches.addAll(benchmarks);
        }

        for (BenchmarkRecord wr : warmupBenches) {
            base.addWarmup(wr);
        }

        Set<ActionPlan> result = new HashSet<ActionPlan>();
        for (BenchmarkRecord br : benchmarks) {
            BenchmarkParams params = new BenchmarkParams(options, br, ActionMode.UNDEF);
            if (params.getForks() > 0) {
                ActionPlan r = new ActionPlan();
                r.mixIn(base);
                if (options.getWarmupMode().isIndi()) {
                    r.addWarmupMeasurement(br);
                } else {
                    r.addMeasurement(br);
                }
                result.add(r);
            }
        }

        return result;
    }

    private Map<BenchmarkRecord, RunResult> runBenchmarks(Set<BenchmarkRecord> benchmarks) {
        out.startRun();

        Multimap<BenchmarkRecord, BenchResult> results = new TreeMultimap<BenchmarkRecord, BenchResult>();

        {
            ActionPlan actionPlan = getEmbeddedActionPlan(benchmarks);
            Multimap<BenchmarkRecord, BenchResult> res = runBenchmarks(false, actionPlan);
            for (BenchmarkRecord br : res.keys()) {
                results.putAll(br, res.get(br));
            }
        }

        for (ActionPlan r : getForkedActionPlans(benchmarks)) {
            Multimap<BenchmarkRecord, BenchResult> res = runSeparate(r);
            for (BenchmarkRecord br : res.keys()) {
                results.putAll(br, res.get(br));
            }
        }

        Map<BenchmarkRecord, RunResult> runResults = mergeRunResults(results);
        out.endRun(runResults);
        return runResults;
    }

    private Map<BenchmarkRecord, RunResult> mergeRunResults(Multimap<BenchmarkRecord, BenchResult> results) {
        Map<BenchmarkRecord, RunResult> result = new TreeMap<BenchmarkRecord, RunResult>();
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

                // Running microbenchmark in separate JVM requires to read some options from annotations.
                final Method benchmarkMethod = MicroBenchmarkHandlers.findBenchmarkMethod(benchmark);
                Fork forkAnnotation = benchmarkMethod.getAnnotation(Fork.class);

                String annJvmArgs = null;
                if (forkAnnotation != null && AnnotationUtils.isSet(forkAnnotation.jvmArgs())) {
                    annJvmArgs = forkAnnotation.jvmArgs().trim();
                }

                String annJvmArgsAppend = null;
                if (forkAnnotation != null && AnnotationUtils.isSet(forkAnnotation.jvmArgsAppend())) {
                    annJvmArgsAppend = forkAnnotation.jvmArgsAppend().trim();
                }

                String annJvmArgsPrepend = null;
                if (forkAnnotation != null && AnnotationUtils.isSet(forkAnnotation.jvmArgsPrepend())) {
                    annJvmArgsPrepend = forkAnnotation.jvmArgsPrepend().trim();
                }

                String[] commandString = getSeparateExecutionCommand(annJvmArgs, annJvmArgsPrepend, annJvmArgsAppend, server.getHost(), server.getPort());

                BenchmarkParams params = new BenchmarkParams(options, benchmark, ActionMode.UNDEF);

                int forkCount = params.getForks();
                int warmupForkCount = params.getWarmupForks();
                if (warmupForkCount > 0) {
                    out.verbosePrintln("Warmup forking " + warmupForkCount + " times using command: " + Arrays.toString(commandString));
                    for (int i = 0; i < warmupForkCount; i++) {
                        out.println("# Warmup Fork: " + (i + 1) + " of " + forkCount);
                        doFork(server, commandString);
                    }
                }

                out.verbosePrintln("Forking " + forkCount + " times using command: " + Arrays.toString(commandString));
                for (int i = 0; i < forkCount; i++) {
                    out.println("# Fork: " + (i + 1) + " of " + forkCount);
                    Multimap<BenchmarkRecord, BenchResult> result = doFork(server, commandString);
                    results.merge(result);
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


    private Multimap<BenchmarkRecord, BenchResult> doFork(BinaryLinkServer reader, String[] commandString) {
        try {
            Process p = Runtime.getRuntime().exec(commandString);

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), System.err);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), System.out);

            errDrainer.start();
            outDrainer.start();

            int ecode = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            // need to wait for all pending messages to be processed
            // before starting the next benchmark
            reader.waitFinish();

            if (ecode != 0) {
                out.println("WARNING: Forked process returned code: " + ecode);
                if (options.shouldFailOnError()) {
                    throw new IllegalStateException("WARNING: Forked process returned code: " + ecode);
                }
            }

        } catch (IOException ex) {
            out.exception(ex);
        } catch (InterruptedException ex) {
            out.exception(ex);
        }

        return reader.getResults();
    }

    /**
     * Helper method for assembling the command to execute the forked JVM with
     *
     * @param host host VM host
     * @param port host VM port
     * @return the final command to execute
     */
    public String[] getSeparateExecutionCommand(String annJvmArgs, String annJvmArgsPrepend, String annJvmArgsAppend, String host, int port) {

        Properties props = System.getProperties();
        String javaHome = (String) props.get("java.home");
        String separator = File.separator;
        String osName = props.getProperty("os.name");
        boolean isOnWindows = osName.contains("indows");
        String platformSpecificBinaryPostfix = isOnWindows ? ".exe" : "";

        String classPath;

        if (options.getJvmClassPath() != null) {
            classPath = options.getJvmClassPath();
        } else {
            classPath = (String) props.get("java.class.path");
        }

        if (isOnWindows) {
            classPath = '"' + classPath + '"';
        }

        List<String> command = new ArrayList<String>();

        // use supplied jvm if given
        if (options.getJvm() != null) {
            command.add(options.getJvm());
        } else {
            // else find out which one parent is and use that
            StringBuilder javaExecutable = new StringBuilder();
            javaExecutable.append(javaHome);
            javaExecutable.append(separator);
            javaExecutable.append("bin");
            javaExecutable.append(separator);
            javaExecutable.append("java");
            javaExecutable.append(platformSpecificBinaryPostfix);
            command.add(javaExecutable.toString());
        }

        if (options.getJvmArgs() != null) { // use supplied jvm args if given in cmd line
            command.addAll(Arrays.asList(options.getJvmArgs().split("[ ]+")));
        } else if (annJvmArgs != null) { // use jvm args supplied in annotation which shuns implicit args
            command.addAll(Arrays.asList(annJvmArgs.split("[ ]+")));
        } else {
            // else use same jvm args given to this runner
            RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
            List<String> args = RuntimemxBean.getInputArguments();

            // prepend jvm args
            if (annJvmArgsPrepend != null) {
                command.addAll(Arrays.asList(annJvmArgsPrepend.split(" ")));
            }

            for (String arg : args) {
                command.add(arg);
            }

            // append jvm args
            if (annJvmArgsAppend != null) {
                command.addAll(Arrays.asList(annJvmArgsAppend.split(" ")));
            }
        }

        // add any compiler oracle hints
        command.add("-XX:CompileCommandFile=" + CompilerHints.hintsFile());

        // assemble final process command
        command.add("-cp");
        command.add(classPath);
        command.add(ForkedMain.class.getName());

        // Forked VM assumes the exact order of arguments:
        //   1) host name to back-connect
        //   2) host port to back-connect
        command.add(host);
        command.add(String.valueOf(port));

        return command.toArray(new String[command.size()]);
    }

}
