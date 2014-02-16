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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author anders.astrand@oracle.com
 * @author Aleksey Shipilev
 */
public class TestOptions {

    private Options EMPTY_BUILDER;
    private CommandLineOptions EMPTY_CMDLINE;

    @Before
    public void setUp() throws Exception {
        EMPTY_CMDLINE = new CommandLineOptions();
        EMPTY_BUILDER = new OptionsBuilder().build();
    }

    @Test
    public void testSerializable_Cmdline() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
        oos.writeObject(EMPTY_CMDLINE);
        oos.flush();
    }

    @Test
    public void testSerializable_Builder() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
        oos.writeObject(EMPTY_BUILDER);
        oos.flush();
    }

    @Test
    public void testIncludes() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions(".*", ".*test.*", "test");
        Options builder = new OptionsBuilder().include(".*").include(".*test.*").include("test").build();
        Assert.assertEquals(builder.getIncludes(), cmdLine.getIncludes());
    }

    @Test
    public void testIncludes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getIncludes(), EMPTY_CMDLINE.getIncludes());
    }

    @Test
    public void testExcludes() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-e", ".*", "-e", ".*test.*", "-e", "test");
        Options builder = new OptionsBuilder().exclude(".*").exclude(".*test.*").exclude("test").build();
        Assert.assertEquals(builder.getExcludes(), cmdLine.getExcludes());
    }

    @Test
    public void testExcludes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getExcludes(), EMPTY_CMDLINE.getExcludes());
    }

    @Test
    public void testOutput() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-o", "sample.out");
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
            CommandLineOptions cmdLine = new CommandLineOptions("-rf", type.toString());
            Options builder = new OptionsBuilder().resultFormat(type).build();
            Assert.assertEquals(builder.getResultFormat(), cmdLine.getResultFormat());
        }
    }

    @Test
    public void testResultFormats_UC() throws Exception {
        for (ResultFormatType type : ResultFormatType.values()) {
            CommandLineOptions cmdLine = new CommandLineOptions("-rf", type.toString().toUpperCase());
            Options builder = new OptionsBuilder().resultFormat(type).build();
            Assert.assertEquals(builder.getResultFormat(), cmdLine.getResultFormat());
        }
    }

    @Test
    public void testResultFormats_LC() throws Exception {
        for (ResultFormatType type : ResultFormatType.values()) {
            CommandLineOptions cmdLine = new CommandLineOptions("-rf", type.toString().toLowerCase());
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
        CommandLineOptions cmdLine = new CommandLineOptions("-rff", "sample.out");
        Options builder = new OptionsBuilder().result("sample.out").build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testResult_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getResult(), EMPTY_CMDLINE.getResult());
    }

    @Test
    public void testGC_Set() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-gc");
        Options builder = new OptionsBuilder().shouldDoGC(true).build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testGC_True() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-gc", "true");
        Options builder = new OptionsBuilder().shouldDoGC(true).build();
        Assert.assertEquals(builder.getOutput(), cmdLine.getOutput());
    }

    @Test
    public void testGC_False() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-gc", "false");
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
        CommandLineOptions cmdLine = new CommandLineOptions("-prof", ProfilerType.CL.id() + "," + ProfilerType.COMP.id());
        Options builder = new OptionsBuilder().addProfiler(ProfilerType.CL).addProfiler(ProfilerType.COMP).build();
        Assert.assertEquals(builder.getProfilers(), cmdLine.getProfilers());
    }

    @Test
    public void testProfilers_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getProfilers(), EMPTY_CMDLINE.getProfilers());
    }

    @Test
    public void testVerbose() throws Exception {
        for (VerboseMode mode : VerboseMode.values()) {
            CommandLineOptions cmdLine = new CommandLineOptions("-v", mode.toString());
            Options builder = new OptionsBuilder().verbosity(mode).build();
            Assert.assertEquals(builder.verbosity(), cmdLine.verbosity());
        }
    }

    @Test
    public void testVerbose_LC() throws Exception {
        for (VerboseMode mode : VerboseMode.values()) {
            CommandLineOptions cmdLine = new CommandLineOptions("-v", mode.toString().toLowerCase());
            Options builder = new OptionsBuilder().verbosity(mode).build();
            Assert.assertEquals(builder.verbosity(), cmdLine.verbosity());
        }
    }

    @Test
    public void testVerbose_UC() throws Exception {
        for (VerboseMode mode : VerboseMode.values()) {
            CommandLineOptions cmdLine = new CommandLineOptions("-v", mode.toString().toUpperCase());
            Options builder = new OptionsBuilder().verbosity(mode).build();
            Assert.assertEquals(builder.verbosity(), cmdLine.verbosity());
        }
    }

    @Test
    public void testVerbose_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.verbosity(), EMPTY_CMDLINE.verbosity());
    }

    @Test
    public void testSFOE_Set() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-foe");
        Options builder = new OptionsBuilder().shouldFailOnError(true).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_True() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-foe", "true");
        Options builder = new OptionsBuilder().shouldFailOnError(true).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_False() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-foe", "false");
        Options builder = new OptionsBuilder().shouldFailOnError(false).build();
        Assert.assertEquals(builder.shouldFailOnError(), cmdLine.shouldFailOnError());
    }

    @Test
    public void testSFOE_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.shouldFailOnError(), EMPTY_CMDLINE.shouldFailOnError());
    }

    @Test
    public void testThreads_Set() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-t", "2");
        Options builder = new OptionsBuilder().threads(2).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreads_Max() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-t", "max");
        Options builder = new OptionsBuilder().threads(Threads.MAX).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreads_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getThreads(), EMPTY_CMDLINE.getThreads());
    }

    @Test
    public void testThreadGroups() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-tg", "3,4");
        Options builder = new OptionsBuilder().threadGroups(3, 4).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreadGroups_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getThreadGroups(), EMPTY_CMDLINE.getThreadGroups());
    }

    @Test
    public void testSynchIterations_Set() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-si");
        Options builder = new OptionsBuilder().syncIterations(true).build();
        Assert.assertEquals(builder.shouldSyncIterations(), cmdLine.shouldSyncIterations());
    }

    @Test
    public void testSynchIterations_True() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-si", "true");
        Options builder = new OptionsBuilder().syncIterations(true).build();
        Assert.assertEquals(builder.shouldSyncIterations(), cmdLine.shouldSyncIterations());
    }

    @Test
    public void testSynchIterations_False() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-si", "false");
        Options builder = new OptionsBuilder().syncIterations(false).build();
        Assert.assertEquals(builder.shouldSyncIterations(), cmdLine.shouldSyncIterations());
    }

    @Test
    public void testSynchIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.shouldSyncIterations(), EMPTY_CMDLINE.shouldSyncIterations());
    }

    @Test
    public void testWarmupIterations() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wi", "34");
        Options builder = new OptionsBuilder().warmupIterations(34).build();
        Assert.assertEquals(builder.getWarmupIterations(), cmdLine.getWarmupIterations());
    }

    @Test
    public void testWarmupIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupIterations(), EMPTY_CMDLINE.getWarmupIterations());
    }

    @Test
    public void testWarmupTime() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-w", "34ms");
        Options builder = new OptionsBuilder().warmupTime(TimeValue.milliseconds(34)).build();
        Assert.assertEquals(builder.getWarmupTime(), cmdLine.getWarmupTime());
    }

    @Test
    public void testWarmupTime_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupTime(), EMPTY_CMDLINE.getWarmupTime());
    }

    @Test
    public void testRuntimeIterations() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-i", "34");
        Options builder = new OptionsBuilder().measurementIterations(34).build();
        Assert.assertEquals(builder.getMeasurementIterations(), cmdLine.getMeasurementIterations());
    }

    @Test
    public void testRuntimeIterations_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getMeasurementIterations(), EMPTY_CMDLINE.getMeasurementIterations());
    }

    @Test
    public void testRuntime() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-r", "34ms");
        Options builder = new OptionsBuilder().measurementTime(TimeValue.milliseconds(34)).build();
        Assert.assertEquals(builder.getMeasurementTime(), cmdLine.getMeasurementTime());
    }

    @Test
    public void testRuntime_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getMeasurementTime(), EMPTY_CMDLINE.getMeasurementTime());
    }

    @Test
    public void testWarmupMicros() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wmb", ".*", "-wmb", ".*test.*", "-wmb", "test");
        Options builder = new OptionsBuilder().includeWarmup(".*").includeWarmup(".*test.*").includeWarmup("test").build();
        Assert.assertEquals(builder.getWarmupIncludes(), cmdLine.getWarmupIncludes());
    }

    @Test
    public void testWarmupMicros_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupIncludes(), EMPTY_CMDLINE.getWarmupIncludes());
    }

    @Test
    public void testBenchModes() throws Exception {
        // TODO: Accept multiple options instead of concatenation?
        CommandLineOptions cmdLine = new CommandLineOptions("-bm", Mode.AverageTime.shortLabel() + "," + Mode.Throughput.shortLabel());
        Options builder = new OptionsBuilder().mode(Mode.AverageTime).mode(Mode.Throughput).build();
        Assert.assertEquals(builder.getBenchModes(), cmdLine.getBenchModes());
    }

    @Test
    public void testBenchModes_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getBenchModes(), EMPTY_CMDLINE.getBenchModes());
    }

    @Test
    public void testTimeunit() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-tu", "ns");
        Options builder = new OptionsBuilder().timeUnit(TimeUnit.NANOSECONDS).build();
        Assert.assertEquals(builder.getTimeUnit(), cmdLine.getTimeUnit());
    }

    @Test
    public void testTimeunit_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getTimeUnit(), EMPTY_CMDLINE.getTimeUnit());
    }

    @Test
    public void testFork() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-f");
        Options builder = new OptionsBuilder().forks(1).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_0() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-f", "0");
        Options builder = new OptionsBuilder().forks(0).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_1() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-f", "1");
        Options builder = new OptionsBuilder().forks(1).build();
        Assert.assertEquals(builder.getForkCount(), cmdLine.getForkCount());
    }

    @Test
    public void testFork_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getForkCount(), EMPTY_CMDLINE.getForkCount());
    }

    @Test
    public void testWarmupFork_0() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wf", "0");
        Options builder = new OptionsBuilder().warmupForks(0).build();
        Assert.assertEquals(builder.getWarmupForkCount(), cmdLine.getWarmupForkCount());
    }

    @Test
    public void testWarmupFork_1() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wf", "1");
        Options builder = new OptionsBuilder().warmupForks(1).build();
        Assert.assertEquals(builder.getWarmupForkCount(), cmdLine.getWarmupForkCount());
    }

    @Test
    public void testWarmupFork_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupForkCount(), EMPTY_CMDLINE.getWarmupForkCount());
    }

    @Test
    public void testJvm() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("--jvm", "sample.jar");
        Options builder = new OptionsBuilder().jvm("sample.jar").build();
        Assert.assertEquals(builder.getJvm(), cmdLine.getJvm());
    }

    @Test
    public void testJvm_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvm(), EMPTY_CMDLINE.getJvm());
    }

    @Test
    public void testJvmArgs() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("--jvmArgs", "sample.jar");
        Options builder = new OptionsBuilder().jvmArgs("sample.jar").build();
        Assert.assertEquals(builder.getJvmArgs(), cmdLine.getJvmArgs());
    }

    @Test
    public void testJvmArgs_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvmArgs(), EMPTY_CMDLINE.getJvmArgs());
    }

    @Test
    public void testBatchSize() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-bs", "42");
        Options builder = new OptionsBuilder().measurementBatchSize(42).build();
        Assert.assertEquals(builder.getMeasurementBatchSize(), cmdLine.getMeasurementBatchSize());
    }

    @Test
    public void testBatchSize_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getMeasurementBatchSize(), EMPTY_CMDLINE.getMeasurementBatchSize());
    }

    @Test
    public void testWarmupBatchSize() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wbs", "43");
        Options builder = new OptionsBuilder().warmupBatchSize(43).build();
        Assert.assertEquals(builder.getWarmupBatchSize(), cmdLine.getWarmupBatchSize());
    }

    @Test
    public void testWarmupBatchSize_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getWarmupBatchSize(), EMPTY_CMDLINE.getWarmupBatchSize());
    }

    @Test
    public void testParam_Default() {
        Assert.assertEquals(EMPTY_BUILDER.getParameter("sample"), EMPTY_CMDLINE.getParameter("sample"));
    }

    @Test
    public void testParam() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-p", "x=1,2,3");
        Options builder = new OptionsBuilder().param("x", "1", "2", "3").build();

        Collection<String> bp = builder.getParameter("x").get();
        Collection<String> cp = cmdLine.getParameter("x").get();

        for (String b : bp) {
            Assert.assertTrue("CP does not contain: " + b, cp.contains(b));
        }
        for (String c : cp) {
            Assert.assertTrue("BP does not contain: " + c, bp.contains(c));
        }
    }

}
