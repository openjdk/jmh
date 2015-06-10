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
import org.openjdk.jmh.results.format.ResultFormatType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
        CommandLineOptions cmdLine = new CommandLineOptions("-prof", "cl", "-prof", "comp");
        Options builder = new OptionsBuilder().addProfiler("cl").addProfiler("comp").build();
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
    public void testThreads_Zero() throws Exception {
        try {
            new CommandLineOptions("-t", "0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '0' of option ['t']. The given value 0 should be positive", e.getMessage());
        }
    }

    @Test
    public void testThreads_Zero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().threads(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Threads (0) should be positive", e.getMessage());
        }
    }

    @Test
    public void testThreads_MinusOne() throws Exception {
        try {
            new CommandLineOptions("-t", "-1");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-1' of option ['t']. The given value -1 should be positive", e.getMessage());
        }
    }

    @Test
    public void testThreads_Minus42() throws Exception {
        try {
            new CommandLineOptions("-t", "-42");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-42' of option ['t']. The given value -42 should be positive", e.getMessage());
        }
    }

    @Test
    public void testThreads_Minus42_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().threads(-42);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Threads (-42) should be positive", e.getMessage());
        }
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
    public void testThreadGroups_WithZero() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-tg", "3,4,0");
        Options builder = new OptionsBuilder().threadGroups(3, 4, 0).build();
        Assert.assertEquals(builder.getThreads(), cmdLine.getThreads());
    }

    @Test
    public void testThreadGroups_AllZero() throws Exception {
        try {
            new CommandLineOptions("-tg", "0,0,0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Group thread count should be positive, but it is 0", e.getMessage());
        }
    }

    @Test
    public void testThreadGroups_AllZero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().threadGroups(0, 0, 0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Group thread count (0) should be positive", e.getMessage());
        }
    }

    @Test
    public void testThreadGroups_WithNegative() throws Exception {
        try {
            new CommandLineOptions("-tg", "-1,-2");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-1' of option ['tg']. The given value -1 should be non-negative", e.getMessage());
        }
    }

    @Test
    public void testThreadGroups_WithNegative_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().threadGroups(-1,-2);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Group #0 thread count (-1) should be non-negative", e.getMessage());
        }
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
    public void testWarmupIterations_Zero() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-wi", "0");
        Options builder = new OptionsBuilder().warmupIterations(0).build();
        Assert.assertEquals(builder.getWarmupIterations(), cmdLine.getWarmupIterations());
    }

    @Test
    public void testWarmupIterations_MinusOne() throws Exception {
        try {
            new CommandLineOptions("-wi", "-1");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-1' of option ['wi']. The given value -1 should be non-negative", e.getMessage());
        }
    }

    @Test
    public void testWarmupIterations_MinusOne_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().warmupIterations(-1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Warmup iterations (-1) should be non-negative", e.getMessage());
        }
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
    public void testRuntimeIterations_Zero() throws Exception {
        try {
            new CommandLineOptions("-i", "0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '0' of option ['i']. The given value 0 should be positive", e.getMessage());
        }
    }

    @Test
    public void testRuntimeIterations_Zero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().measurementIterations(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Measurement iterations (0) should be positive", e.getMessage());
        }
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
    public void testOPI() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-opi", "42");
        Options builder = new OptionsBuilder().operationsPerInvocation(42).build();
        Assert.assertEquals(builder.getTimeUnit(), cmdLine.getTimeUnit());
    }

    @Test
    public void testOPI_Zero() throws Exception {
        try {
            new CommandLineOptions("-opi", "0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '0' of option ['opi']. The given value 0 should be positive", e.getMessage());
        }
    }

    @Test
    public void testOPI_Zero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().operationsPerInvocation(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Operations per invocation (0) should be positive", e.getMessage());
        }
    }

    @Test
    public void testOPI_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getOperationsPerInvocation(), EMPTY_CMDLINE.getOperationsPerInvocation());
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
    public void testFork_MinusOne() throws Exception {
        try {
            new CommandLineOptions("-f", "-1");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-1' of option ['f']. The given value -1 should be non-negative", e.getMessage());
        }
    }

    @Test
    public void testFork__MinusOne_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().forks(-1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Forks (-1) should be non-negative", e.getMessage());
        }
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
    public void testWarmupFork_MinusOne() throws Exception {
        try {
            new CommandLineOptions("-wf", "-1");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '-1' of option ['wf']. The given value -1 should be non-negative", e.getMessage());
        }
    }

    @Test
    public void testWarmupFork_MinusOne_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().warmupForks(-1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Warmup forks (-1) should be non-negative", e.getMessage());
        }
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
    public void testJvmArgsAppend() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("--jvmArgsAppend", "sample.jar");
        Options builder = new OptionsBuilder().jvmArgsAppend("sample.jar").build();
        Assert.assertEquals(builder.getJvmArgsAppend(), cmdLine.getJvmArgsAppend());
    }

    @Test
    public void testJvmArgsAppend_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvmArgsAppend(), EMPTY_CMDLINE.getJvmArgsAppend());
    }

    @Test
    public void testJvmArgsPrepend() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("--jvmArgsPrepend", "sample.jar");
        Options builder = new OptionsBuilder().jvmArgsPrepend("sample.jar").build();
        Assert.assertEquals(builder.getJvmArgsPrepend(), cmdLine.getJvmArgsPrepend());
    }

    @Test
    public void testJvmArgsPrepend_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getJvmArgsPrepend(), EMPTY_CMDLINE.getJvmArgsPrepend());
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
    public void testBatchSize_Zero() throws Exception {
        try {
            new CommandLineOptions("-bs", "0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '0' of option ['bs']. The given value 0 should be positive", e.getMessage());
        }
    }

    @Test
    public void testBatchSize_Zero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().measurementBatchSize(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Measurement batch size (0) should be positive", e.getMessage());
        }
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
    public void testWarmupBatchSize_Zero() throws Exception {
        try {
            new CommandLineOptions("-wbs", "0");
            Assert.fail();
        } catch (CommandLineOptionException e) {
            Assert.assertEquals("Cannot parse argument '0' of option ['wbs']. The given value 0 should be positive", e.getMessage());
        }
    }

    @Test
    public void testWarmupBatchSize_Zero_OptionsBuilder() throws Exception {
        try {
            new OptionsBuilder().warmupBatchSize(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Warmup batch size (0) should be positive", e.getMessage());
        }
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

    @Test
    public void testParam_EmptyString() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-p", "x=");
        Options builder = new OptionsBuilder().param("x", "").build();

        Collection<String> bp = builder.getParameter("x").get();
        Collection<String> cp = cmdLine.getParameter("x").get();

        for (String b : bp) {
            Assert.assertTrue("CP does not contain: " + b, cp.contains(b));
        }
        for (String c : cp) {
            Assert.assertTrue("BP does not contain: " + c, bp.contains(c));
        }
    }

    @Test
    public void testTimeout() throws Exception {
        CommandLineOptions cmdLine = new CommandLineOptions("-to", "34ms");
        Options builder = new OptionsBuilder().timeout(TimeValue.milliseconds(34)).build();
        Assert.assertEquals(builder.getTimeout(), cmdLine.getTimeout());
    }

    @Test
    public void testTimeout_Default() throws Exception {
        Assert.assertEquals(EMPTY_BUILDER.getTimeout(), EMPTY_CMDLINE.getTimeout());
    }

}
