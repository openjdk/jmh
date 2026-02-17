/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.openjdk.jmh.it.fork;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests that JMH correctly handles boolean JVM flags originating from
 * .hotspotrc or -XX:Flags=file. These flags appear in
 * ManagementFactory.getRuntimeMXBean().getInputArguments() without the
 * -XX: prefix (e.g. "+UseG1GC" instead of "-XX:+UseG1GC"), and must be
 * normalized before being passed to the forked JVM.
 */
public class ForkedHotspotrcFlagsTest {

    @Benchmark
    public void bench() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        Assert.assertTrue("Expected -XX:+UseG1GC in forked VM args: " + args,
                args.contains("-XX:+UseG1GC"));
        Assert.assertTrue("Expected -XX:+TieredCompilation in forked VM args: " + args,
                args.contains("-XX:+TieredCompilation"));
        Assert.assertTrue("Expected -XX:-AlwaysPreTouch in forked VM args: " + args,
                args.contains("-XX:-AlwaysPreTouch"));
        Assert.assertTrue("Expected -XX:MaxHeapSize=536870912 in forked VM args: " + args,
                args.contains("-XX:MaxHeapSize=536870912"));
        Assert.assertEquals("Expected -DHandles.Weird.Props=yes in forked VM",
                "yes", System.getProperty("Handles.Weird.Props"));
    }

    /**
     * Launches a child JVM with -XX:Flags=file containing a boolean flag,
     * which then runs the benchmark above with forks(1). If the bare
     * flag leaks through to the forked process command line, the fork will
     * fail with an unrecognized option error.
     */
    @Test
    public void testHotspotrcBooleanFlagsNormalized() throws Exception {
        File flagsFile = File.createTempFile("jmh-test-hotspotrc", ".flags");
        flagsFile.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(flagsFile)) {
            // true and false boolean, plus non-boolean
            pw.println("+UseG1GC");
            pw.println("-UseParallelGC");
            pw.println("MaxHeapSize=536870912");
        }

        String classpath = System.getProperty("java.class.path");
        String jvm = Utils.getCurrentJvm();

        List<String> command = new ArrayList<>();
        command.add(jvm);
        command.add("-XX:Flags=" + flagsFile.getAbsolutePath());

        // make sure these flags are not clobbered
        command.add("-XX:+TieredCompilation");
        command.add("-XX:-AlwaysPreTouch");
        command.add("-DHandles.Weird.Props=yes");

        command.add("-cp");
        command.add(classpath);
        command.add(InnerRunner.class.getName());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String output = new String(p.getInputStream().readAllBytes());

        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            Assert.fail("Child JVM timed out. Output:\n" + output);
        }

        Assert.assertEquals(
                "Child JVM running JMH with .hotspotrc-style flags failed. Output:\n" + output,
                0, p.exitValue());
    }

    public static class InnerRunner {
        public static void main(String[] args) throws Exception {
            Options opt =
                    new OptionsBuilder()
                            .include(Fixtures.getTestMask(ForkedHotspotrcFlagsTest.class))
                            .shouldFailOnError(true)
                            .mode(Mode.SingleShotTime)
                            .forks(1)
                            .warmupIterations(0)
                            .measurementIterations(1)
                            .build();
            new Runner(opt).run();
        }
    }
}
