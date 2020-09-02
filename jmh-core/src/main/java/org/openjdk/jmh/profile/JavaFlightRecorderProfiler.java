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
import org.openjdk.jmh.util.JDKVersion;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A profiler based on Java Flight Recorder.
 *
 * @author Jason Zaugg
 */
public final class JavaFlightRecorderProfiler implements ExternalProfiler, InternalProfiler {

    private final boolean verbose;
    private final File outDir;
    private final boolean debugNonSafePoints;
    private final String configName;
    private final Collection<String> flightRecorderOptions = new ArrayList<>();
    private PostProcessor postProcessor = null;

    private boolean measurementStarted = false;
    private int measurementIterationCount;
    private String profileName;
    private final List<File> generated = new ArrayList<>();

    public JavaFlightRecorderProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();

        parser.formatHelpWith(new ProfilerOptionFormatter("jfr"));

        OptionSpec<String> optDir = parser.accepts("dir",
            "Output directory.")
            .withRequiredArg().ofType(String.class).describedAs("dir");

        OptionSpec<String> optConfig = parser.accepts("configName",
            "Name of a predefined Flight Recorder configuration, e.g. profile or default")
            .withRequiredArg().ofType(String.class).describedAs("name").defaultsTo("profile");

        OptionSpec<Boolean> optDebugNonSafePoints = parser.accepts("debugNonSafePoints",
            "Gather cpu samples asynchronously, rather than at the subsequent safepoint.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(true);

        OptionSpec<Integer> optStackDepth = parser.accepts("stackDepth",
            "Maximum number of stack frames collected for each event.")
            .withRequiredArg().ofType(Integer.class).describedAs("frames");

        OptionSpec<String> optPostProcessor = parser.accepts("postProcessor",
            "The fully qualified name of a class that implements " + PostProcessor.class + ". " +
            "This must have a public, no-argument constructor.")
            .withRequiredArg().ofType(String.class).describedAs("fqcn");

        OptionSpec<Boolean> optVerbose = parser.accepts("verbose",
            "Output the sequence of commands")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false).describedAs("bool");

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            this.debugNonSafePoints = optDebugNonSafePoints.value(set);
            this.configName = optConfig.value(set);
            if (set.has(optStackDepth)) {
                flightRecorderOptions.add("stackdepth=" + optStackDepth.value(set));
            }
            verbose = optVerbose.value(set);
            if (!set.has(optDir)) {
                outDir = new File(System.getProperty("user.dir"));
            } else {
                outDir = new File(set.valueOf(optDir));
            }
            if (set.has(optPostProcessor)) {
                try {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class<?> postProcessorClass = loader.loadClass(optPostProcessor.value(set));
                    postProcessor = (PostProcessor) postProcessorClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new ProfilerException(e);
                }
            }
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        if (iterationParams.getType() == IterationType.MEASUREMENT) {
            if (!measurementStarted) {
                profileName = benchmarkParams.id();
                execute(benchmarkParams.getJvm(), "JFR.start", Collections.singletonList("settings=" + configName));
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
                File jfrFile = new File(trialOutDir, "profile.jfr");
                String filenameOption = "filename=" + jfrFile.getAbsolutePath();
                execute(benchmarkParams.getJvm(), "JFR.stop", Collections.singletonList(filenameOption));
                generated.add(jfrFile);
                if (postProcessor != null) {
                    generated.addAll(postProcessor.postProcess(benchmarkParams, jfrFile));
                }
                return Collections.singletonList(result());
            }
        }

        return Collections.emptyList();
    }

    private TextResult result() {
        StringWriter output = new StringWriter();
        PrintWriter pw = new PrintWriter(output);
        pw.println("JFR profiler results:");
        for (File file : generated) {
            pw.print("  ");
            pw.println(file.getPath());
        }
        pw.flush();
        pw.close();
        return new TextResult(output.toString(), "jfr");
    }

    private File createTrialOutDir(BenchmarkParams benchmarkParams) {
        String fileName = benchmarkParams.id();
        File trialOutDir = new File(this.outDir, fileName);
        trialOutDir.mkdirs();
        return trialOutDir;
    }

    private void execute(String jvm, String cmd, Collection<String> options) {
        long pid = Utils.getPid();
        ArrayList<String> fullCommand = new ArrayList<>();
        fullCommand.add(findJcmd(jvm).getAbsolutePath());
        fullCommand.add(String.valueOf(pid));
        fullCommand.add(cmd);
        fullCommand.add("name=" + profileName);
        fullCommand.addAll(options);
        if (verbose) {
            System.out.println("[jfr] " + fullCommand);
        }
        // TODO Use JMX to control FlightRecorder when the baseline of JDK support in JMH
        // advances to a version with that included.
        Collection<String> errorOutput = Utils.tryWith(fullCommand.toArray(new String[0]));
        if (!errorOutput.isEmpty()) {
            throw new RuntimeException("Error executing: " + fullCommand + System.lineSeparator() +
                Utils.join(errorOutput, System.lineSeparator()));
        }
    }

    private File findJcmd(String jvm) {
        // Try 1: same directory as JVM binary
        File bin = new File(jvm).getParentFile();
        {
            File jcmd = new File(bin, "jcmd" + (Utils.isWindows() ? ".exe" : ""));
            if (jcmd.exists()) {
                return jcmd;
            }
        }

        // Try 2: parent bin/ directory
        {
            final String s = File.separator;
            File jcmd = new File(bin, ".." + s + ".." + s + "bin" + s + "jcmd" + (Utils.isWindows() ? ".exe" : ""));
            if (jcmd.exists()) {
                return jcmd;
            }
        }

        // No dice.
        throw new IllegalStateException("Cannot find jcmd anywhere");
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        List<String> args = new ArrayList<>();
        if (debugNonSafePoints) {
            args.add("-XX:+UnlockDiagnosticVMOptions");
            args.add("-XX:+DebugNonSafepoints");
        }

        if (!flightRecorderOptions.isEmpty()) {
            args.add("-XX:FlightRecorderOptions=" + Utils.join(flightRecorderOptions, ","));
        }

        // JDK 13+ does not need need the opt-in
        if (JDKVersion.parseMajor(params.getJdkVersion()) < 13) {
            args.add("-XX:+FlightRecorder");
        }

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
        return "Java Flight Recorder profiler";
    }

    public interface PostProcessor {
        List<File> postProcess(BenchmarkParams benchmarkParams, File jfrFile);
    }
}
