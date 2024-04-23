/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.profilers;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.profile.XCTraceAsmProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class XCTraceAsmProfilerTest extends AbstractAsmProfilerTest {
    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private static boolean xctraceExists() {
        Collection<String> out = Utils.tryWith("xcode-select", "-p");
        if (!out.isEmpty()) {
            return false;
        }
        Optional<String> path = Utils.runWith("xcode-select", "-p").stream()
                .flatMap(line -> Arrays.stream(line.split("\n")))
                .findFirst();
        return path.map(p -> new File(p, "usr/bin/xctrace").exists()).orElse(false);
    }

    private static void skipIfProfilerNotSupport() {
        Assume.assumeTrue(xctraceExists());
    }

    private void checkProfiling(int numForks) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceAsmProfiler.class)
                .forks(numForks)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();
        String out = ProfilerTestUtils.checkedGet(sr, "asm").extendedInfo();
        if (!checkDisassembly(out)) {
            throw new IllegalStateException("Profile does not contain the required frame: " + out);
        }
    }

    @Test
    public void testProfiling() throws RunnerException {
        skipIfProfilerNotSupport();

        checkProfiling(1);
    }
    @Test
    public void testProfilingWithMultipleForks() throws RunnerException {
        skipIfProfilerNotSupport();

        checkProfiling(2);
    }

    @Test
    public void testTemplateOption() throws RunnerException {
        skipIfProfilerNotSupport();

        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceAsmProfiler.class, "template=Time Profiler")
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();
        String out = ProfilerTestUtils.checkedGet(sr, "asm").extendedInfo();
        if (!checkDisassembly(out)) {
            throw new IllegalStateException("Profile does not contain the required frame: " + out);
        }
    }

    @Test
    public void testSavePerfBinHandling() throws RunnerException {
        skipIfProfilerNotSupport();
        File expectedFile = new File(TMP_DIR, "results.trace");
        try {
            Options opts = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .addProfiler(XCTraceAsmProfiler.class,
                            "savePerfBin=true;savePerfBinToFile=" + expectedFile.getAbsolutePath())
                    .build();
            new Runner(opts).runSingle();
            Assert.assertTrue("Results were not copied", expectedFile.isDirectory());
        } finally {
            expectedFile.delete();
        }
    }

    @Test
    public void testConstructorThrowsWhenXCTraceDoesNotExist() {
        Assume.assumeFalse(xctraceExists());
        Assert.assertThrows(ProfilerException.class, () -> new XCTraceAsmProfiler(""));
    }
}
