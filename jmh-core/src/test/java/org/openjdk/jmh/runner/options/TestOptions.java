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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author anders.astrand@oracle.com
 * @author Aleksey Shipilev
 */
public class TestOptions {

    private Options EMPTY_BUILDER;
    private CommandLineOptions EMPTY_CMDLINE;

    private static CommandLineOptions getOptions(String[] argv) throws Exception {
        CommandLineOptions opts = CommandLineOptions.newInstance();
        opts.parseArguments(argv);
        return opts;
    }

    @Before
    public void setUp() throws Exception {
        EMPTY_CMDLINE = getOptions(new String[]{});
        EMPTY_BUILDER = new OptionsBuilder().build();
    }

    @Test
    public void testIncludes() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{".*", ".*test.*", "test"});
        Options builder = new OptionsBuilder().include(".*").include(".*test.*").include("test").build();
        Assert.assertEquals(builder.getRegexps(), cmdLine.getRegexps());
    }

    @Test
    public void testIncludes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getRegexps(), EMPTY_CMDLINE.getRegexps());
    }

    @Test
    public void testExcludes() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-e", ".*", "-e", ".*test.*", "-e", "test"});
        Options builder = new OptionsBuilder().exclude(".*").exclude(".*test.*").exclude("test").build();
        Assert.assertEquals(builder.getExcludes(), cmdLine.getExcludes());
    }

    @Test
    public void testExcludes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getExcludes(), EMPTY_CMDLINE.getExcludes());
    }

    @Test
    public void testOutputFormats() throws Exception {
        for (OutputFormatType type : OutputFormatType.values()) {
            CommandLineOptions cmdLine = getOptions(new String[]{ "-of", type.toString()});
            Options builder = new OptionsBuilder().outputFormat(type).build();
            Assert.assertEquals(builder.getOutputFormat(), cmdLine.getOutputFormat());
        }
    }

    @Test
    public void testOutputFormats_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getOutputFormat(), EMPTY_CMDLINE.getOutputFormat());
    }

    @Test
    public void testOutput() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-o", "sample.out"});
        Options builder = new OptionsBuilder().output("sample.out").build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testOutput_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getOutput(), EMPTY_CMDLINE.getOutput());
    }

    @Test
    public void testResultFormats() throws Exception {
        for (ResultFormatType type : ResultFormatType.values()) {
            CommandLineOptions cmdLine = getOptions(new String[]{ "-rf", type.toString()});
            Options builder = new OptionsBuilder().resultFormat(type).build();
            Assert.assertEquals(builder.getResultFormat(), cmdLine.getResultFormat());
        }
    }

    @Test
    public void testResultFormats_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getResultFormat(), EMPTY_CMDLINE.getResultFormat());
    }

    @Test
    public void testResult() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-rff", "sample.out"});
        Options builder = new OptionsBuilder().result("sample.out").build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testResult_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getResult(), EMPTY_CMDLINE.getResult());
    }

    @Test
    public void testGC_Set() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-gc"});
        Options builder = new OptionsBuilder().shouldDoGC(true).build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testGC_True() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-gc", "true"});
        Options builder = new OptionsBuilder().shouldDoGC(true).build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testGC_False() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-gc", "false"});
        Options builder = new OptionsBuilder().shouldDoGC(false).build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testGC_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.shouldDoGC(), EMPTY_CMDLINE.shouldDoGC());
    }

    @Test
    public void testProfilers() throws Exception {
        // TODO: Should be able to accept multiple values without concat?
        CommandLineOptions cmdLine = getOptions(new String[]{"-prof", ProfilerType.CL.id() + "," + ProfilerType.COMP.id()});
        Options builder = new OptionsBuilder().addProfiler(ProfilerType.CL).addProfiler(ProfilerType.COMP).build();
        Assert.assertEquals(builder.getProfilers(), cmdLine.getProfilers());
    }

    @Test
    public void testProfilers_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getProfilers(), EMPTY_CMDLINE.getProfilers());
    }

    @Test
    public void testVerbose_Set() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-v"});
        Options builder = new OptionsBuilder().verbose(true).build();
        Assert.assertEquals(builder.isVerbose(), cmdLine.isVerbose());
    }

    @Test
    public void testVerbose_True() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-v", "true"});
        Options builder = new OptionsBuilder().verbose(true).build();
        Assert.assertEquals(builder.isVerbose(), cmdLine.isVerbose());
    }

    @Test
    public void testVerbose_False() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-v", "false"});
        Options builder = new OptionsBuilder().verbose(false).build();
        Assert.assertEquals(builder.isVerbose(), cmdLine.isVerbose());
    }

    @Test
    public void testVerbose_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.isVerbose(), EMPTY_CMDLINE.isVerbose());
    }

    @Test
    public void testSFOE_Set() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-foe"});
        Options builder = new OptionsBuilder().failOnError(true).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_True() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-foe", "true"});
        Options builder = new OptionsBuilder().failOnError(true).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_False() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-foe", "false"});
        Options builder = new OptionsBuilder().failOnError(false).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.shouldFailOnError(), EMPTY_CMDLINE.shouldFailOnError());
    }

    @Test
    public void testThreads_Set() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-t", "2"});
        Options builder = new OptionsBuilder().threads(2).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreads_Max() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-t", "max"});
        Options builder = new OptionsBuilder().threads(Threads.MAX).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreads_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getThreads(), EMPTY_CMDLINE.getThreads());
    }

    @Test
    public void testThreadGroups() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-tg", "3,4"});
        Options builder = new OptionsBuilder().threadGroups(3, 4).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreadGroups_Default() throws Exception {
        Assert.assertArrayEquals(EMPTY_BUILDER.getThreadGroups(), EMPTY_CMDLINE.getThreadGroups());
    }

    @Test
    public void testSynchIterations_Set() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-si"});
        Options builder = new OptionsBuilder().syncIterations(true).build();
        Assert.assertEquals(builder.getSynchIterations(), cmdLine.getSynchIterations());
    }

    @Test
    public void testSynchIterations_True() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-si", "true"});
        Options builder = new OptionsBuilder().syncIterations(true).build();
        Assert.assertEquals(builder.getSynchIterations(), cmdLine.getSynchIterations());
    }

    @Test
    public void testSynchIterations_False() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-si", "false"});
        Options builder = new OptionsBuilder().syncIterations(false).build();
        Assert.assertEquals(builder.getSynchIterations(), cmdLine.getSynchIterations());
    }

    @Test
    public void testSynchIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getSynchIterations(), EMPTY_CMDLINE.getSynchIterations());
    }

    @Test
    public void testWarmupIterations() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-wi", "34"});
        Options builder = new OptionsBuilder().warmupIterations(34).build();
        Assert.assertEquals(builder.getWarmupIterations(), cmdLine.getWarmupIterations());
    }

    @Test
    public void testWarmupIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupIterations(), EMPTY_CMDLINE.getWarmupIterations());
    }

    @Test
    public void testWarmupTime() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-w", "34ms"});
        Options builder = new OptionsBuilder().warmupTime(TimeValue.milliseconds(34)).build();
        Assert.assertEquals(builder.getWarmupTime(), cmdLine.getWarmupTime());
    }

    @Test
    public void testWarmupTime_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupTime(), EMPTY_CMDLINE.getWarmupTime());
    }

    @Test
    public void testRuntimeIterations() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-i", "34"});
        Options builder = new OptionsBuilder().measurementIterations(34).build();
        Assert.assertEquals(builder.getIterations(), cmdLine.getIterations());
    }

    @Test
    public void testRuntimeIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getIterations(), EMPTY_CMDLINE.getIterations());
    }

    @Test
    public void testRuntime() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-r", "34ms"});
        Options builder = new OptionsBuilder().measurementTime(TimeValue.milliseconds(34)).build();
        Assert.assertEquals(builder.getRuntime(), cmdLine.getRuntime());
    }

    @Test
    public void testRuntime_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getRuntime(), EMPTY_CMDLINE.getRuntime());
    }

    @Test
    public void testWarmupMicros() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-wmb", ".*", "-wmb", ".*test.*", "-wmb", "test"});
        Options builder = new OptionsBuilder().includeWarmup(".*").includeWarmup(".*test.*").includeWarmup("test").build();
        Assert.assertEquals(builder.getWarmupMicros(), cmdLine.getWarmupMicros());
    }

    @Test
    public void testWarmupMicros_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupMicros(), EMPTY_CMDLINE.getWarmupMicros());
    }

    @Test
    public void testBenchModes() throws Exception {
        // TODO: Accept multiple options instead of concatenation?
        CommandLineOptions cmdLine = getOptions(new String[]{"-bm", Mode.AverageTime.shortLabel() + "," + Mode.Throughput.shortLabel()});
        Options builder = new OptionsBuilder().mode(Mode.AverageTime).mode(Mode.Throughput).build();
        Assert.assertEquals(builder.getBenchModes(), cmdLine.getBenchModes());
    }

    @Test
    public void testBenchModes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getBenchModes(), EMPTY_CMDLINE.getBenchModes());
    }

    @Test
    public void testTimeunit() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-tu", "ns"});
        Options builder = new OptionsBuilder().timeUnit(TimeUnit.NANOSECONDS).build();
        Assert.assertEquals(builder.getTimeUnit(), cmdLine.getTimeUnit());
    }

    @Test
    public void testTimeunit_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getTimeUnit(), EMPTY_CMDLINE.getTimeUnit());
    }

    @Test
    public void testFork() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-f"});
        Options builder = new OptionsBuilder().forks(1).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_0() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-f", "0"});
        Options builder = new OptionsBuilder().forks(0).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_1() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-f", "1"});
        Options builder = new OptionsBuilder().forks(1).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getForkCount(), EMPTY_CMDLINE.getForkCount());
    }

    @Test
    public void testWarmupFork_0() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-wf", "0"});
        Options builder = new OptionsBuilder().warmupForks(0).build();
        Assert.assertEquals(builder.getWarmupForkCount(), cmdLine.getWarmupForkCount());
    }

    @Test
    public void testWarmupFork_1() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"-wf", "1"});
        Options builder = new OptionsBuilder().warmupForks(1).build();
        Assert.assertEquals(builder.getWarmupForkCount(), cmdLine.getWarmupForkCount());
    }

    @Test
    public void testWarmupFork_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupForkCount(), EMPTY_CMDLINE.getWarmupForkCount());
    }

    @Test
    public void testJvmClasspath() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"--jvmclasspath", "sample.jar"});
        Options builder = new OptionsBuilder().jvmClasspath("sample.jar").build();
        Assert.assertEquals(builder.getJvmClassPath(), cmdLine.getJvmClassPath());
    }

    @Test
    public void testJvmClasspath_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvmClassPath(), EMPTY_CMDLINE.getJvmClassPath());
    }

    @Test
    public void testJvm() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"--jvm", "sample.jar"});
        Options builder = new OptionsBuilder().jvm("sample.jar").build();
        Assert.assertEquals(builder.getJvm(), cmdLine.getJvm());
    }

    @Test
    public void testJvm_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvm(), EMPTY_CMDLINE.getJvm());
    }

    @Test
    public void testJvmArgs() throws Exception {
        CommandLineOptions cmdLine = getOptions(new String[]{"--jvmargs", "sample.jar"});
        Options builder = new OptionsBuilder().jvmArgs("sample.jar").build();
        Assert.assertEquals(builder.getJvmArgs(), cmdLine.getJvmArgs());
    }

    @Test
    public void testJvmArgs_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvmArgs(), EMPTY_CMDLINE.getJvmArgs());
    }

}
