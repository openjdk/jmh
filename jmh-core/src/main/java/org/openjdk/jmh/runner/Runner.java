/**
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

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.logic.results.IterationData;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.output.OutputFormatFactory;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.output.format.internal.BinaryOutputFormatReader;
import org.openjdk.jmh.runner.options.HarnessOptions;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParametersFactory;
import org.openjdk.jmh.runner.parameters.ThreadIterationParams;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.InputStreamDrainer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    /** Class holding all our runtime options/arguments */
    private final HarnessOptions options;

    /**
     * Main constructor.
     *
     * @param options Option instance with options to use
     */
    public Runner(HarnessOptions options) {
        super(options);
        this.options = options;
    }

    /**
     * Auxiliary constructor, used from test which are required custom outputHandler.
     * @param options
     * @param handler - custom outputHandler
     */
    public Runner(HarnessOptions options, OutputFormat handler) {
        super(options);
        this.options = options;
        this.outputHandler = handler;
    }

    public enum ExecutionMode {
        /**
         * classic mode:
         *   - single JVM for all microbenchmarks (unless benchmark requires forked JVM itself),
         *   - warmup performed before each microbenchmark
         *   - -frw options will exec additional warmup on increasing amount of threads
         */
        CLASSIC,
        /**
         * - exec bulk warmup micrbenchmarks using --warmupmicrobenchmarks option or -warmupmode beforeAny
         * - there is no any kind of warmup when iteration started
         */
        BULK_WARMUP,
        /**
         *   Fork JVM for each benchmark.
         *   Child harness (forked) is running in CLASSIC mode.
         */
        FORK_ALL;

        public static ExecutionMode getExecutionMode(HarnessOptions options) {
            if (options.getForkCount() > 0) {
                return FORK_ALL;
            } else if ((options.getWarmupMicros() != null && !options.getWarmupMicros().isEmpty()) ||
                       (options.getWarmupMode() == HarnessOptions.WarmupMode.BEFOREANY)) {
                return BULK_WARMUP;
            } else {
                return CLASSIC;
            }
        }
    }

    /** Setup helper method, creates OutputHandler according to argv options. */
    public static OutputFormat createOutputHandler(HarnessOptions options) throws RunnerException {
        PrintStream out;
        // setup OutputHandler singleton
        if (options.getOutput() == null) {
            out = System.out;
        } else {
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(options.getOutput()))));
                System.setOut(out); // override to print everything to file
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                throw new RunnerException(ex);
            }
        }

        return OutputFormatFactory.createFormatInstance(out, options.getOutputFormat(), options.getTraceLevel(), options.isVerbose());
    }

    /** Main entry point */
    public void run(MicroBenchmarkList list) throws RunnerException {
        if (outputHandler == null) {
            // setup our output handler
            outputHandler = createOutputHandler(options);
        }

        Set<String> benchmarks;

        // get a list of benchmarks
        if (options.getRegexps().isEmpty()) {
            outputHandler.println("No regexp to match against benchmarks was given. Use -h for help or \".*\" for every benchmark.");
            outputHandler.flush();
            benchmarks = Collections.<String>emptySet();
        } else {
            benchmarks = list.find(outputHandler, options.getRegexps(), options.getExcludes());
        }

        if (benchmarks.isEmpty() && !options.getRegexps().isEmpty()) {
            outputHandler.println("No matching benchmarks. Miss-spelled regexp? Use -v for verbose output.");
            outputHandler.flush();
        }

        if (options.shouldList() || options.isVerbose()) {
            outputHandler.println("Benchmarks: ");

            // list microbenchmarks if -l and/or -v
            for (String benchmark : benchmarks) {
                outputHandler.println(benchmark);
            }
        }
        // exit if list only, else run benchmarks
        if (!options.shouldList() && !benchmarks.isEmpty()) {
            switch (ExecutionMode.getExecutionMode(options)) {
                case CLASSIC:
                case FORK_ALL:
                    runBenchmarks(benchmarks);
                    break;
                case BULK_WARMUP:
                    runBulkWarmupBenchmarks(benchmarks, list);
                    break;
                default:
                    throw new IllegalArgumentException("Internal error: unknown execution mode");

            }
        }

        outputHandler.flush();
        outputHandler.close();
    }

    /**
     * Run specified warmup microbenchmarks prior to running
     * any requested mircobenchmarks.
     * currently valid only for non-external JVM runs
     * @param benchmarks
     * @param list
     */
    private void runBulkWarmupBenchmarks(Set<String> benchmarks, MicroBenchmarkList list) {
        // Attention: Here is violation of outputHandler.startRun/endRun contract,
        // but because of such code was done before me,
        // I won't touch this in order do not crash output parsers. (SK)

        // list of micros executed before iteration
        Set<String> warmupMicros = new TreeSet<String>();

        List<String> warmupMicrosRegexp = options.getWarmupMicros();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty()) {
            warmupMicros.addAll(list.find(outputHandler, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode() == HarnessOptions.WarmupMode.BEFOREANY) {
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
            outputHandler.startRun("Warmup Section");
            for (String benchmark : warmupMicros) {
                runBulkWarmupModeMicroBenchmark(benchmark, true);
            }
            outputHandler.endRun(null);
        }
        // run microbenchmarks
        //
        outputHandler.startRun("Measurement Section");
        for (String benchmark : benchmarks) {
            runBulkWarmupModeMicroBenchmark(benchmark, false);
        }
        outputHandler.endRun(null);
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

    private void runBenchmarks(Set<String> benchmarks) {
        Set<String> embedded = new TreeSet<String>();
        Set<String> forked = new TreeSet<String>();

        outputHandler.startRun("Measurement Section");
        for (String benchmark : benchmarks) {
            int f = decideForks(options.getForkCount(), benchForks(benchmark));
            if (f > 0) {
                forked.add(benchmark);
            } else {
                embedded.add(benchmark);
            }
        }

        for (String benchmark : embedded) {
            runClassicBenchmark(benchmark);
        }

        runSeparate(forked);
        outputHandler.endRun(null);
    }

    private void runSeparate(Set<String> benchmarksToFork) {
        BinaryOutputFormatReader reader = null;
        try {
            reader = new BinaryOutputFormatReader(outputHandler);
            for (String benchmark : benchmarksToFork) {
                runSeparateMicroBenchmark(benchmark, reader.getHost(), reader.getPort());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (reader != null) {
                reader.terminate();
            }
        }
    }

    /**
     * tests if the benchmark has the mandatory fork annotation
     * @param benchmark
     * @return
     */
    private int benchForks(String benchmark) {
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
    private void runSeparateMicroBenchmark(String benchmark, String host, int port) {

        /*
         * Running microbenchmark in separate JVM requires to read some options from annotations.
         */

        final Method benchmarkMethod = MicroBenchmarkHandlers.findBenchmarkMethod(benchmark);
        Fork forkAnnotation = benchmarkMethod.getAnnotation(Fork.class);

        String annJvmArgs = null;
        if (forkAnnotation != null && !forkAnnotation.jvmArgs().equals(Fork.PARAM_NOT_SET)) {
            annJvmArgs = forkAnnotation.jvmArgs().trim();
        }

        String annJvmArgsAppend = null;
        if (forkAnnotation != null && !forkAnnotation.jvmArgsAppend().equals(Fork.PARAM_NOT_SET)) {
            annJvmArgsAppend = forkAnnotation.jvmArgsAppend().trim();
        }

        String annJvmArgsPrepend = null;
        if (forkAnnotation != null && !forkAnnotation.jvmArgsPrepend().equals(Fork.PARAM_NOT_SET)) {
            annJvmArgsPrepend = forkAnnotation.jvmArgsPrepend().trim();
        }

        String commandString = options.getSeparateExecutionCommand(benchmark, annJvmArgs, annJvmArgsPrepend, annJvmArgsAppend, host, port);

        int forkCount = decideForks(options.getForkCount(), benchForks(benchmark));
        int warmupForkCount = forkAnnotation != null ? forkAnnotation.warmups() : 0;
        if( warmupForkCount>0) {
            String warmupForkCheat = commandString+" -wi 1 -i 0";
            if (warmupForkCount == 1) {
                outputHandler.verbosePrintln("Warmup forking using command: " + warmupForkCheat);
            } else {
                outputHandler.verbosePrintln("Warmup forking " + warmupForkCount + " times using command: " + warmupForkCheat);
            }
            for (int i = 0; i < warmupForkCount; i++) {
                doFork(warmupForkCheat);
            }
        }
        if (forkCount == 1) {
            outputHandler.verbosePrintln("Forking using command: " + commandString);
        } else {
            outputHandler.verbosePrintln("Forking " + forkCount + " times using command: " + commandString);
        }
        for (int i = 0; i < forkCount; i++) { // TODO should we report fork number somehow?
            doFork(commandString);
        }
    }

    private void doFork(String commandString) {
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

            if (ecode != 0) {
                outputHandler.println("WARNING: Forked process returned code: " + ecode);
                if (options.shouldFailOnError()) {
                    throw new IllegalStateException("WARNING: Forked process returned code: " + ecode);
                }
            }

        } catch (IOException ex) {
            outputHandler.exception(ex);
        } catch (InterruptedException ex) {
            outputHandler.exception(ex);
        }
    }

    /**
     * Run a micro benchmark in bulk warmup mode
     *
     * @param benchmark benchmark to run
     */
    private List<IterationData> runBulkWarmupModeMicroBenchmark(String benchmark, boolean warmup) {
        List<IterationData> allResults = new ArrayList<IterationData>();
        try {
            int index = benchmark.lastIndexOf('.');
            String className = benchmark.substring(0, index);
            String methodName = benchmark.substring(index + 1);

            Class<?> clazz = ClassUtils.loadClass(className);
            Method method = MicroBenchmarkHandlers.findBenchmarkMethod(clazz, methodName);

            MicroBenchmarkParameters executionParams = MicroBenchmarkParametersFactory.makeParams(options, method);
            MicroBenchmarkHandler handler = MicroBenchmarkHandlers.getInstance(outputHandler, benchmark, clazz, method, executionParams, options);
            if (warmup) {
                executionParams = executionParams.warmupToIteration();
            }
            outputHandler.startBenchmark(handler.getName(), executionParams, options.isVerbose());
            int iteration = 0;
            final Collection<ThreadIterationParams> threadIterationSequence = executionParams.getThreadIterationSequence();
            for (ThreadIterationParams tip : threadIterationSequence) {
                List<IterationData> ticResults = runMicroBenchmark(handler, iteration, tip, options.shouldOutputDetailedResults(), options.shouldOutputThreadSubStatistics() && threadIterationSequence.size() > 1);
                allResults.addAll(ticResults);
                iteration += tip.getCount();
            }
            // only print end-of-run output if we have actual results
            if (!allResults.isEmpty()) {
                RunResult result = aggregateIterationData(allResults);
                outputHandler.endBenchmark(handler.getName(), result);
            }

            handler.shutdown();

        } catch (Throwable ex) {
            outputHandler.exception(ex);
            if (options.shouldFailOnError()) {
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        }
        return allResults;
    }

}
