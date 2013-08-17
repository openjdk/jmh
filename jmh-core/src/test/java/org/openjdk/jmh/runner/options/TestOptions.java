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

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParametersFactory;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author anders.astrand@oracle.com
 */
public class TestOptions {

    public static final String ORACLE_FORKED_MAIN_CLASS = "org.openjdk.jmh.ForkedMain";

    private static String getForked(String[] argv) throws Exception{
        HarnessOptions options =  HarnessOptions.newInstance();
        options.parseArguments(argv);
        StringBuilder sb = new StringBuilder();
        for (String l : options.toCommandLine()) {
            sb.append(l).append(' ');
        }
        // with sweet love from Sweden: remove final delimiter
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @Test
    public void testGetForkArguments() throws Exception {
        String[] argv = {"-e", ".*Apa.*", ".*SPECjbb2005.*", "-f", "-v"};
        assertEquals("-v true", getForked(argv));


        String[] argv2 = {".*Hash.*", "-e", ".*Apa.*", "-f", "-e", ".*a.*", "-v", "-i", "10", "-r", "100", "-t", "2"};
        assertEquals("-i 10 -r 100s -t 2 -v true", getForked(argv2));


        String[] argv3 = {"--jvmargs", "\"-Xmx31337g -Xms31337g\"", "--jvm", "/opt/java/konrad/bin/java", "-l", "-e", ".*Apa.*", "-f", "-e", ".*a.*", "-v", "-i", "10", "-r", "100", "-t", "2", ".*Hash.*"};
        assertEquals("-i 10 -r 100s -t 2 -v true", getForked(argv3));


        String[] argv4 = {"-f"};
        assertEquals("", getForked(argv4));


        String[] argv5 = {"-e", ".*Apa.*", ".*SPECjbb2005.*", "-f", "-v", "-o", "SPECjbb2005"};
        assertEquals("-v true", getForked(argv5));


        String[] argv6 = {".*Apa.*", ".*SPECjbb2006.*", "-sc", "-f", "-v", "-o", "SPECjbb2006"};
        assertEquals("-sc true -v true", getForked(argv6));


        String[] argv7 = {".*Apa.*", ".*SPECjbb2007.*", "-w", "100", "-sc", "-f", "-v", "-o", "SPECjbb2007"};
        assertEquals("-sc true -v true -w 100s", getForked(argv7));


        String[] argv8 = {".*Apa.*", ".*SPECjbb2008.*", "-w", "100", "-sc", "-l", "-f", "-v", "-o", "SPECjbb2008"};
        assertEquals("-sc true -v true -w 100s", getForked(argv8));


        String[] argv9 = {"-i", "10", "-r", "10", "-w", "10", "-t", "10", "-sc", "true", "-tc", "1,2,3,4", "-si", "-l", "-v", "-o", "SPECjbb2005", "-of", "csv"};
        assertEquals("-i 10 -r 10s -sc true -si true -t 10 -tc 1,2,3,4 -v true -w 10s", getForked(argv9));
    }


    private static HarnessOptions getOptions(String[] argv) throws Exception {
        HarnessOptions opts = HarnessOptions.newInstance();
        opts.parseArguments(argv);
        return opts;
    }

    @Test
    public void testGetBenchmarkSpecificInstance() throws Exception {
        String[] argv = {".*Hash.*", "-e", ".*Apa.*", "-f", "-e", ".*a.*", "-v", "-i", "11", "-r", "100s", "-t", "2", "-sc", "false"};
        HarnessOptions options = getOptions(argv);

        BenchmarkRecord br = new BenchmarkRecord(this.getClass().getName() + "dummyMethod2", this.getClass().getName() + "dummyMethod2", Mode.Throughput);
        MicroBenchmarkParameters mbp = MicroBenchmarkParametersFactory.makeParams(options, br, this.getClass().getMethod("dummyMethod"));

        assertEquals(options.getIterations(), mbp.getIteration().getCount());
        assertFalse(options.shouldScale());
        assertFalse(mbp.shouldScale());
        assertEquals(options.getThreads(), mbp.getMaxThreads());

        String[] argv2 = {".*Hash.*", "-e", ".*Apa.*", "-f", "-e", ".*a.*", "-v"};
        options = getOptions(argv2);

        mbp = MicroBenchmarkParametersFactory.makeParams(options, br, this.getClass().getMethod("dummyMethod"));

        assertTrue(options.getIterations() != mbp.getIteration().getCount());
        assertTrue(options.getThreads() != mbp.getMaxThreads());

    }

    @Test
    public void testAutoNumberOfThreads() throws Exception {
        String[] argv = {".*Hash.*", "-e", ".*Apa.*", "-f", "-e", ".*a.*", "-v", "-i", "11", "-r", "100s", "-sc"};
        HarnessOptions options = getOptions(argv);

        BenchmarkRecord br = new BenchmarkRecord(this.getClass().getName() + "dummyMethod2", this.getClass().getName() + "dummyMethod2", Mode.Throughput);
        MicroBenchmarkParameters mbp = MicroBenchmarkParametersFactory.makeParams(options, br, this.getClass().getMethod("dummyMethod2"));

        assertEquals(Runtime.getRuntime().availableProcessors(), mbp.getMaxThreads());
    }

    @Test
    public void testThreadsMax() throws Exception {
        String[] argv = {"-t", "max"};
        HarnessOptions options = getOptions(argv);
        assertEquals(0, options.getThreads());
    }

    @Test
    public void testThreadCounts() throws Exception {
        String[] argv = {"-tc", "1,2,3,4,5"};
        HarnessOptions options = getOptions(argv);
        List<Integer> expected = Arrays.asList(1,2,3,4,5);
        assertTrue(expected.equals(options.getThreadCounts()));
    }

    @Test
    public void testThreadsWrong() throws Exception {
        String parameter = "maxx";
        String[] argv = {"-t", parameter};
        try {
            HarnessOptions options = getOptions(argv);
        } catch (CmdLineException e) {
            assertTrue("Exception message did not contain parameter value", e.getMessage().contains(parameter));
            return;
        }

        fail("Did not get the expected CmdLineException when given a faulty -t parameter");
    }

    @Test
    public void testThreadCountssWrong() throws Exception {
        String parameter = "lol";
        String[] argv = {"-tc", parameter};
        try {
            HarnessOptions options = getOptions(argv);
        } catch (CmdLineException e) {
            assertTrue("Exception message did not contain parameter value", e.getMessage().contains(parameter));
            return;
        }

        fail("Did not get the expected CmdLineException when given a faulty -tc parameter");
    }

    @Threads(100)
    @Measurement(iterations = 100, time = 3)
    @Fork(jvmArgs = "apa")
    public void dummyMethod() {
    }

    @Measurement(iterations = 100, time = 3)
    @Fork(jvmArgs = "apa")
    public void dummyMethod2() {
    }

    @Test
    public void testWarmupWithArg() throws Exception {
        String[] argv = {"-w", "100"};
        HarnessOptions options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.SECONDS), options.getWarmupTime());

        argv = new String[]{"-w", "100ms"};
        options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.MILLISECONDS), options.getWarmupTime());

        argv = new String[]{"-w", "100us"};
        options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.MICROSECONDS), options.getWarmupTime());

        try {
            argv = new String[]{"-w", "100ps"};
            options = getOptions(argv);
            fail("CmdLineException Expected");
        } catch (CmdLineException e) {
            // PASS
        }
    }

    @Test
    public void testForceReWarmup() throws Exception {
        String[] argv = {"-frw", ".*Hash.*"};
        HarnessOptions options = getOptions(argv);

        assertTrue(options.shouldForceReWarmup());

        String[] argv2 = {".*Hash.*"};
        options = getOptions(argv2);

        assertFalse(options.shouldForceReWarmup());
    }
}
