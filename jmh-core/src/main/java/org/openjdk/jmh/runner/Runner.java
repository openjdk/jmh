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
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.OutputFormatFactory;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.WarmupMode;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.util.AnnotationUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.Utils;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getRegexps(), options.getExcludes());

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
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getRegexps(), options.getExcludes());

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
        Set<BenchmarkRecord> benchmarks = list.find(out, options.getRegexps(), options.getExcludes());

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

        Map<BenchmarkRecord, RunResult> results;
        if ((!options.getWarmupMicros().isEmpty()) ||
                (options.getWarmupMode() == WarmupMode.BEFOREANY)) {
            results = runBulkWarmupBenchmarks(benchmarks);
        } else {
            results = runBenchmarks(benchmarks);
        }

        out.flush();
        out.close();

        return results;
    }

    /**
     * Run specified warmup microbenchmarks prior to running any requested mircobenchmarks.
     * TODO: Currently valid only for non-external JVM runs
     */
    private Map<BenchmarkRecord, RunResult> runBulkWarmupBenchmarks(Set<BenchmarkRecord> benchmarks) {
        out.startRun();

        // list of micros executed before iteration
        Set<BenchmarkRecord> warmupMicros = new TreeSet<BenchmarkRecord>();

        List<String> warmupMicrosRegexp = options.getWarmupMicros();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupMicros.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode() == WarmupMode.BEFOREANY) {
            warmupMicros.addAll(benchmarks);
        }

        if (!warmupMicros.isEmpty()) {
            // run warmup iterations of the requested benchmarks before running
            // any measured iterations of any of the requested benchmarks. This
            // has the effect of getting all the classes loaded getting the JITed
            // code the the final state, possibly invalidating optimizations that
            // might not be invalided until later and quite possibly invalidated
            // during measurement iteration causing a performance shift or simply
            // increased variance.
            // currently valid only for non-external JVM runs

            int count = 0;
            for (BenchmarkRecord benchmark : warmupMicros) {
                out.println("# Fork: N/A, bulk warmup in progress, " + (++count) + " of " + warmupMicros.size());
                runBenchmark(benchmark, true, false);
                out.println("");
            }
        }
        // run microbenchmarks
        //
        Map<BenchmarkRecord, RunResult> results = new TreeMap<BenchmarkRecord, RunResult>();
        for (BenchmarkRecord benchmark : benchmarks) {
            out.println("# Fork: N/A, test runs in same VM");
            RunResult result = runBenchmark(benchmark, false, true);
            results.put(benchmark, RunResult.merge(results.get(benchmark), result));
        }
        out.endRun();

        return results;
    }

    private int decideForks(int optionForks, int benchForks) {
        if (optionForks == -1) {
            if (benchForks == -1) {
                return Defaults.DEFAULT_FORK_TIMES;
            } else {
                return benchForks;
            }
        } else {
            return optionForks;
        }
    }

    private int decideWarmupForks(int optionWarmupForks, Fork forkAnnotation) {
        if (optionWarmupForks == -1) {
            return (forkAnnotation != null) ? forkAnnotation.warmups() : 0;
        } else {
            return optionWarmupForks;
        }
    }

    private Map<BenchmarkRecord, RunResult> runBenchmarks(Set<BenchmarkRecord> benchmarks) {
        Set<BenchmarkRecord> embedded = new TreeSet<BenchmarkRecord>();
        Set<BenchmarkRecord> forked = new TreeSet<BenchmarkRecord>();

        out.startRun();
        for (BenchmarkRecord benchmark : benchmarks) {
            int f = decideForks(options.getForkCount(), benchForks(benchmark));
            if (f > 0) {
                forked.add(benchmark);
            } else {
                embedded.add(benchmark);
            }
        }

        Map<BenchmarkRecord, RunResult> results = new TreeMap<BenchmarkRecord, RunResult>();
        for (BenchmarkRecord benchmark : embedded) {
            out.println("# Fork: N/A, test runs in same VM");
            RunResult r = runBenchmark(benchmark, true, true);
            results.put(benchmark, RunResult.merge(results.get(benchmark), r));
        }

        Map<BenchmarkRecord, RunResult> separateResults = runSeparate(forked);
        for (Map.Entry<BenchmarkRecord, RunResult> e : separateResults.entrySet()) {
            results.put(e.getKey(), RunResult.merge(results.get(e.getKey()), e.getValue()));
        }

        out.endRun();

        return results;
    }

    private Map<BenchmarkRecord, RunResult> runSeparate(Set<BenchmarkRecord> benchmarksToFork) {
        BinaryLinkServer server = null;
        try {
            server = new BinaryLinkServer(options, out);
            for (BenchmarkRecord benchmark : benchmarksToFork) {
                runSeparateMicroBenchmark(server, benchmark, server.getHost(), server.getPort());
            }
            return server.getResults();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (server != null) {
                server.terminate();
            }
        }
    }

    /**
     * Tests if the benchmark has the fork annotation
     */
    private int benchForks(BenchmarkRecord benchmark) {
        Method m = MicroBenchmarkHandlers.findBenchmarkMethod(benchmark);
        Fork fork = m.getAnnotation(Fork.class);
        return (fork != null) ? fork.value() : -1;
    }

    /**
     * Run the micro benchmark in a separate JVM process
     *
     * @param benchmark micro to run
     * @param host host VM host
     * @param port host VM port
     */
    private void runSeparateMicroBenchmark(BinaryLinkServer reader, BenchmarkRecord benchmark, String host, int port) {

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

        String[] commandString = getSeparateExecutionCommand(benchmark, annJvmArgs, annJvmArgsPrepend, annJvmArgsAppend, host, port);

        int forkCount = decideForks(options.getForkCount(), benchForks(benchmark));
        int warmupForkCount = decideWarmupForks(options.getWarmupForkCount(), forkAnnotation);
        if (warmupForkCount > 0) {
            String[] warmupForkCheat = Utils.concat(commandString, new String[]{"-wi", "1", "-i", "0"});
            out.verbosePrintln("Warmup forking " + warmupForkCount + " times using command: " + Arrays.toString(warmupForkCheat));
            for (int i = 0; i < warmupForkCount; i++) {
                out.println("# Warmup Fork: " + (i+1) + " of " + forkCount);
                doFork(reader, warmupForkCheat);
            }
        }

        out.verbosePrintln("Forking " + forkCount + " times using command: " + Arrays.toString(commandString));
        for (int i = 0; i < forkCount; i++) {
            out.println("# Fork: " + (i+1) + " of " + forkCount);
            doFork(reader, commandString);
        }
    }

    private void doFork(BinaryLinkServer reader, String[] commandString) {
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
    }

    /**
     * Helper method for assembling the command to execute the forked JVM with
     *
     * @param benchmark benchmark to execute
     * @param host host VM host
     * @param port host VM port
     * @return the final command to execute
     */
    public String[] getSeparateExecutionCommand(BenchmarkRecord benchmark, String annJvmArgs, String annJvmArgsPrepend, String annJvmArgsAppend, String host, int port) {

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
        {
            Set<String> hints = CompilerHints.defaultList().get();
            if (!hints.isEmpty()) {
                command.add("-XX:CompileCommand=quiet ");
            }
            for (String l : hints) {
                command.add("-XX:CompileCommand=" + l);
            }
        }

        // assemble final process command
        command.add("-cp");
        command.add(classPath);
        command.add(ForkedMain.class.getName());

        // Forked VM assumes the exact order of arguments:
        //   1) host name to back-connect
        //   2) host port to back-connect
        //   3) benchmark to execute (saves benchmark lookup via Options)
        command.add(host);
        command.add(String.valueOf(port));
        command.add(benchmark.toLine());

        return command.toArray(new String[command.size()]);
    }

}
