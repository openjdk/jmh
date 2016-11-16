/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class TestParentOptions {

    @Test
    public void testIncludes_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getIncludes().isEmpty());
    }

    @Test
    public void testIncludes_Parent() throws Exception {
        Options parent = new OptionsBuilder().include(".*").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Collections.singletonList(".*"), builder.getIncludes());
    }

    @Test
    public void testIncludes_Merge() throws Exception {
        Options parent = new OptionsBuilder().include(".*").build();
        Options builder = new OptionsBuilder().parent(parent).include(".*test.*").build();
        Assert.assertEquals(Arrays.asList(".*test.*", ".*"), builder.getIncludes());
    }

    @Test
    public void testExcludes_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getExcludes().isEmpty());
    }

    @Test
    public void testExcludes_Parent() throws Exception {
        Options parent = new OptionsBuilder().include(".*").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Collections.singletonList(".*"), builder.getIncludes());
    }

    @Test
    public void testExcludes_Merge() throws Exception {
        Options parent = new OptionsBuilder().exclude(".*").build();
        Options builder = new OptionsBuilder().parent(parent).exclude(".*test.*").build();
        Assert.assertEquals(Arrays.asList(".*test.*", ".*"), builder.getExcludes());
    }

    @Test
    public void testProfiler_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getProfilers().isEmpty());
    }

    @Test
    public void testProfiler_Parent() throws Exception {
        Options parent = new OptionsBuilder().addProfiler("cl").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getProfilers().size() == 1);
        Assert.assertTrue(builder.getProfilers().contains(new ProfilerConfig("cl", "")));
    }

    @Test
    public void testProfiler_Merge() throws Exception {
        Options parent = new OptionsBuilder().addProfiler("cl").build();
        Options builder = new OptionsBuilder().parent(parent).addProfiler("comp").build();
        Assert.assertTrue(builder.getProfilers().size() == 2);
        Assert.assertTrue(builder.getProfilers().contains(new ProfilerConfig("cl", "")));
        Assert.assertTrue(builder.getProfilers().contains(new ProfilerConfig("comp", "")));
    }

    @Test
    public void testBenchModes_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getBenchModes().isEmpty());
    }

    @Test
    public void testBenchModes_Parent() throws Exception {
        Options parent = new OptionsBuilder().mode(Mode.AverageTime).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(EnumSet.of(Mode.AverageTime), builder.getBenchModes());
    }

    @Test
    public void testBenchModes_Merge() throws Exception {
        Options parent = new OptionsBuilder().mode(Mode.AverageTime).build();
        Options builder = new OptionsBuilder().parent(parent).mode(Mode.SingleShotTime).build();
        Assert.assertEquals(EnumSet.of(Mode.SingleShotTime), builder.getBenchModes());
    }

    @Test
    public void testForks_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getForkCount().hasValue());
    }

    @Test
    public void testForks_Parent() throws Exception {
        Options parent = new OptionsBuilder().forks(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getForkCount().get());
    }

    @Test
    public void testForks_Merge() throws Exception {
        Options parent = new OptionsBuilder().forks(42).build();
        Options builder = new OptionsBuilder().parent(parent).forks(84).build();
        Assert.assertEquals(Integer.valueOf(84), builder.getForkCount().get());
    }

    @Test
    public void testGC_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.shouldDoGC().hasValue());
    }

    @Test
    public void testGC_Parent() throws Exception {
        Options parent = new OptionsBuilder().shouldDoGC(true).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(true, builder.shouldDoGC().get());
    }

    @Test
    public void testGC_Merge() throws Exception {
        Options parent = new OptionsBuilder().shouldDoGC(true).build();
        Options builder = new OptionsBuilder().parent(parent).shouldDoGC(false).build();
        Assert.assertEquals(false, builder.shouldDoGC().get());
    }

    @Test
    public void testJVM_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getJvm().hasValue());
    }

    @Test
    public void testJVM_Parent() throws Exception {
        Options parent = new OptionsBuilder().jvm("path").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals("path", builder.getJvm().get());
    }

    @Test
    public void testJVM_Merge() throws Exception {
        Options parent = new OptionsBuilder().jvm("path").build();
        Options builder = new OptionsBuilder().parent(parent).jvm("path2").build();
        Assert.assertEquals("path2", builder.getJvm().get());
    }

    @Test
    public void testJVMArgs_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getJvmArgs().hasValue());
    }

    @Test
    public void testJVMArgs_Parent() throws Exception {
        Options parent = new OptionsBuilder().jvmArgs("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Arrays.asList("opt1", "opt2"), builder.getJvmArgs().get());
    }

    @Test
    public void testJVMArgs_Merge() throws Exception {
        Options parent = new OptionsBuilder().jvmArgs("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).jvmArgs("opt3", "opt4").build();
        Assert.assertEquals(Arrays.asList("opt3", "opt4"), builder.getJvmArgs().get());
    }

    @Test
    public void testJVMArgsAppend_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getJvmArgsAppend().hasValue());
    }

    @Test
    public void testJVMArgsAppend_Parent() throws Exception {
        Options parent = new OptionsBuilder().jvmArgsAppend("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Arrays.asList("opt1", "opt2"), builder.getJvmArgsAppend().get());
    }

    @Test
    public void testJVMArgsAppend_Merge() throws Exception {
        Options parent = new OptionsBuilder().jvmArgsAppend("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).jvmArgsAppend("opt3", "opt4").build();
        Assert.assertEquals(Arrays.asList("opt3", "opt4"), builder.getJvmArgsAppend().get());
    }

    @Test
    public void testJVMArgsPrepend_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getJvmArgsPrepend().hasValue());
    }

    @Test
    public void testJVMArgsPrepend_Parent() throws Exception {
        Options parent = new OptionsBuilder().jvmArgsPrepend("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Arrays.asList("opt1", "opt2"), builder.getJvmArgsPrepend().get());
    }

    @Test
    public void testJVMArgsPrepend_Merge() throws Exception {
        Options parent = new OptionsBuilder().jvmArgsPrepend("opt1", "opt2").build();
        Options builder = new OptionsBuilder().parent(parent).jvmArgsPrepend("opt3", "opt4").build();
        Assert.assertEquals(Arrays.asList("opt3", "opt4"), builder.getJvmArgsPrepend().get());
    }

    @Test
    public void testOutput_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getOutput().hasValue());
    }

    @Test
    public void testOutput_Parent() throws Exception {
        Options parent = new OptionsBuilder().output("out1").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals("out1", builder.getOutput().get());
    }

    @Test
    public void testOutput_Merged() throws Exception {
        Options parent = new OptionsBuilder().output("out1").build();
        Options builder = new OptionsBuilder().parent(parent).output("out2").build();
        Assert.assertEquals("out2", builder.getOutput().get());
    }

    @Test
    public void testResult_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getResult().hasValue());
    }

    @Test
    public void testResult_Parent() throws Exception {
        Options parent = new OptionsBuilder().result("out1").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals("out1", builder.getResult().get());
    }

    @Test
    public void testResult_Merged() throws Exception {
        Options parent = new OptionsBuilder().result("out1").build();
        Options builder = new OptionsBuilder().parent(parent).result("out2").build();
        Assert.assertEquals("out2", builder.getResult().get());
    }

    @Test
    public void testResultFormat_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getResultFormat().hasValue());
    }

    @Test
    public void testResultFormat_Parent() throws Exception {
        Options parent = new OptionsBuilder().resultFormat(ResultFormatType.JSON).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(ResultFormatType.JSON, builder.getResultFormat().get());
    }

    @Test
    public void testResultFormat_Merged() throws Exception {
        Options parent = new OptionsBuilder().resultFormat(ResultFormatType.JSON).build();
        Options builder = new OptionsBuilder().parent(parent).resultFormat(ResultFormatType.SCSV).build();
        Assert.assertEquals(ResultFormatType.SCSV, builder.getResultFormat().get());
    }

    @Test
    public void testRuntime_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getMeasurementTime().hasValue());
    }

    @Test
    public void testRuntime_Parent() throws Exception {
        Options parent = new OptionsBuilder().measurementTime(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(TimeValue.hours(42), builder.getMeasurementTime().get());
    }

    @Test
    public void testRuntime_Merged() throws Exception {
        Options parent = new OptionsBuilder().measurementTime(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).measurementTime(TimeValue.days(42)).build();
        Assert.assertEquals(TimeValue.days(42), builder.getMeasurementTime().get());
    }

    @Test
    public void testMeasureIters_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getMeasurementIterations().hasValue());
    }

    @Test
    public void testMeasureIters_Parent() throws Exception {
        Options parent = new OptionsBuilder().measurementIterations(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getMeasurementIterations().get());
    }

    @Test
    public void testMeasureIters_Merged() throws Exception {
        Options parent = new OptionsBuilder().measurementIterations(42).build();
        Options builder = new OptionsBuilder().parent(parent).measurementIterations(84).build();
        Assert.assertEquals(Integer.valueOf(84), builder.getMeasurementIterations().get());
    }

    @Test
    public void testSFOE_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.shouldFailOnError().hasValue());
    }

    @Test
    public void testSFOE_Parent() throws Exception {
        Options parent = new OptionsBuilder().shouldFailOnError(true).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(true, builder.shouldFailOnError().get());
    }

    @Test
    public void testSFOE_Merged() throws Exception {
        Options parent = new OptionsBuilder().shouldFailOnError(true).build();
        Options builder = new OptionsBuilder().parent(parent).shouldFailOnError(false).build();
        Assert.assertEquals(false, builder.shouldFailOnError().get());
    }

    @Test
    public void testSyncIters_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.shouldSyncIterations().hasValue());
    }

    @Test
    public void testSyncIters_Parent() throws Exception {
        Options parent = new OptionsBuilder().syncIterations(true).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(true, builder.shouldSyncIterations().get());
    }

    @Test
    public void testSyncIters_Merged() throws Exception {
        Options parent = new OptionsBuilder().syncIterations(true).build();
        Options builder = new OptionsBuilder().parent(parent).syncIterations(false).build();
        Assert.assertEquals(false, builder.shouldSyncIterations().get());
    }

    @Test
    public void testThreadGroups_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getThreadGroups().hasValue());
    }

    @Test
    public void testThreadGroups_Parent() throws Exception {
        Options parent = new OptionsBuilder().threadGroups(1, 2).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertArrayEquals(new int[]{1, 2}, builder.getThreadGroups().get());
    }

    @Test
    public void testThreadGroups_Merged() throws Exception {
        Options parent = new OptionsBuilder().threadGroups(1, 2).build();
        Options builder = new OptionsBuilder().parent(parent).threadGroups(3, 4).build();
        Assert.assertArrayEquals(new int[]{3, 4}, builder.getThreadGroups().get());
    }

    @Test
    public void testThreads_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getThreads().hasValue());
    }

    @Test
    public void testThreads_Parent() throws Exception {
        Options parent = new OptionsBuilder().threads(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getThreads().get());
    }

    @Test
    public void testThreads_Merged() throws Exception {
        Options parent = new OptionsBuilder().threads(42).build();
        Options builder = new OptionsBuilder().parent(parent).threads(84).build();
        Assert.assertEquals(Integer.valueOf(84), builder.getThreads().get());
    }

    @Test
    public void testTimeUnit_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getTimeUnit().hasValue());
    }

    @Test
    public void testTimeUnit_Parent() throws Exception {
        Options parent = new OptionsBuilder().timeUnit(TimeUnit.DAYS).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(TimeUnit.DAYS, builder.getTimeUnit().get());
    }

    @Test
    public void testTimeUnit_Merged() throws Exception {
        Options parent = new OptionsBuilder().timeUnit(TimeUnit.DAYS).build();
        Options builder = new OptionsBuilder().parent(parent).timeUnit(TimeUnit.HOURS).build();
        Assert.assertEquals(TimeUnit.HOURS, builder.getTimeUnit().get());
    }

    @Test
    public void testOPI_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getOperationsPerInvocation().hasValue());
    }

    @Test
    public void testOPI_Parent() throws Exception {
        Options parent = new OptionsBuilder().operationsPerInvocation(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getOperationsPerInvocation().get());
    }

    @Test
    public void testOPI_Merged() throws Exception {
        Options parent = new OptionsBuilder().operationsPerInvocation(42).build();
        Options builder = new OptionsBuilder().parent(parent).operationsPerInvocation(43).build();
        Assert.assertEquals(Integer.valueOf(43), builder.getOperationsPerInvocation().get());
    }

    @Test
    public void testVerbose_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.verbosity().hasValue());
    }

    @Test
    public void testVerbose_Parent() throws Exception {
        Options parent = new OptionsBuilder().verbosity(VerboseMode.EXTRA).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(VerboseMode.EXTRA, builder.verbosity().get());
    }

    @Test
    public void testVerbose_Merged() throws Exception {
        Options parent = new OptionsBuilder().verbosity(VerboseMode.EXTRA).build();
        Options builder = new OptionsBuilder().parent(parent).verbosity(VerboseMode.SILENT).build();
        Assert.assertEquals(VerboseMode.SILENT, builder.verbosity().get());
    }

    @Test
    public void testWarmupForks_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getWarmupForkCount().hasValue());
    }

    @Test
    public void testWarmupForks_Parent() throws Exception {
        Options parent = new OptionsBuilder().warmupForks(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getWarmupForkCount().get());
    }

    @Test
    public void testWarmupForks_Merge() throws Exception {
        Options parent = new OptionsBuilder().warmupForks(42).build();
        Options builder = new OptionsBuilder().parent(parent).warmupForks(84).build();
        Assert.assertEquals(Integer.valueOf(84), builder.getWarmupForkCount().get());
    }

    @Test
    public void testWarmupIters_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getWarmupIterations().hasValue());
    }

    @Test
    public void testWarmupIters_Parent() throws Exception {
        Options parent = new OptionsBuilder().warmupIterations(42).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Integer.valueOf(42), builder.getWarmupIterations().get());
    }

    @Test
    public void testWarmupIters_Merged() throws Exception {
        Options parent = new OptionsBuilder().warmupIterations(42).build();
        Options builder = new OptionsBuilder().parent(parent).warmupIterations(84).build();
        Assert.assertEquals(Integer.valueOf(84), builder.getWarmupIterations().get());
    }

    @Test
    public void testWarmupTime_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getWarmupTime().hasValue());
    }

    @Test
    public void testWarmupTime_Parent() throws Exception {
        Options parent = new OptionsBuilder().warmupTime(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(TimeValue.hours(42), builder.getWarmupTime().get());
    }

    @Test
    public void testWarmupTime_Merged() throws Exception {
        Options parent = new OptionsBuilder().warmupTime(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).warmupTime(TimeValue.days(42)).build();
        Assert.assertEquals(TimeValue.days(42), builder.getWarmupTime().get());
    }

    @Test
    public void testWarmupMicros_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertTrue(builder.getWarmupIncludes().isEmpty());
    }

    @Test
    public void testWarmupMicros_Parent() throws Exception {
        Options parent = new OptionsBuilder().includeWarmup(".*").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(Collections.singletonList(".*"), builder.getWarmupIncludes());
    }

    @Test
    public void testWarmupMicros_Merged() throws Exception {
        Options parent = new OptionsBuilder().includeWarmup(".*").build();
        Options builder = new OptionsBuilder().parent(parent).includeWarmup(".*test.*").build();
        Assert.assertEquals(Arrays.asList(".*test.*", ".*"), builder.getWarmupIncludes());
    }

    @Test
    public void testParameters_Empty() {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getParameter("x").hasValue());
    }

    @Test
    public void testParameters_Parent() {
        Options parent = new OptionsBuilder().param("x", "1", "2", "3").build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Collection<String> bp = builder.getParameter("x").get();
        Assert.assertEquals(3, bp.size());
        for (String b : new String[] {"1", "2", "3"}) {
            Assert.assertTrue("BP does not contain: " + b, bp.contains(b));
        }
    }

    @Test
    public void testParameters_Merged() {
        Options parent = new OptionsBuilder().param("x", "1", "2", "3").build();
        Options builder = new OptionsBuilder().parent(parent).param("x", "4", "5", "6").build();
        Collection<String> bp = builder.getParameter("x").get();
        Assert.assertEquals(3, bp.size());
        for (String b : new String[] {"4", "5", "6"}) {
            Assert.assertTrue("BP does not contain: " + b, bp.contains(b));
        }
    }

    @Test
    public void testTimeout_Empty() throws Exception {
        Options parent = new OptionsBuilder().build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertFalse(builder.getTimeout().hasValue());
    }

    @Test
    public void testTimeout_Parent() throws Exception {
        Options parent = new OptionsBuilder().timeout(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).build();
        Assert.assertEquals(TimeValue.hours(42), builder.getTimeout().get());
    }

    @Test
    public void testTimeout_Merged() throws Exception {
        Options parent = new OptionsBuilder().timeout(TimeValue.hours(42)).build();
        Options builder = new OptionsBuilder().parent(parent).timeout(TimeValue.days(42)).build();
        Assert.assertEquals(TimeValue.days(42), builder.getTimeout().get());
    }

}
