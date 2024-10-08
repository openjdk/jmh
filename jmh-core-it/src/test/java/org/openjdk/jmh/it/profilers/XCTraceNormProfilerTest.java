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
import org.openjdk.jmh.profile.XCTraceNormProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.util.*;

public class XCTraceNormProfilerTest extends AbstractAsmProfilerTest {
    private static boolean xctraceExists() {
        Collection<String> out = Utils.tryWith("xcode-select", "-p");
        if (!out.isEmpty()) {
            return false;
        }
        Optional<String> path = Utils.runWith("xcode-select", "-p").stream()
                .flatMap(line -> Arrays.stream(line.split("\n")))
                .findFirst();
        if (!path.isPresent()) {
            return false;
        }
        File xctraceExe = new File(path.get(), "usr/bin/xctrace");
        if (!xctraceExe.exists()) {
            return false;
        }

        Collection<String> versionOut = Utils.runWith(xctraceExe.getAbsolutePath(), "version");
        Optional<String> versionStr = versionOut.stream().flatMap(l -> Arrays.stream(l.split("\n")))
                .filter(l -> l.contains("xctrace version "))
                .findAny();
        if (!versionStr.isPresent()) {
            return false;
        }
        try {
            int version = Integer.parseInt(versionStr.get()
                    .split("version ")[1]
                    .split(" ")[0]
                    .split("\\.")[0]);
            return version >= 13;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isInsideVM() {
        // Alternatively, we can check if CPC subsystem is up and running (kern.cpc.secure)
        String vmmPresent = Utils.runWith("sysctl", "-n", "kern.hv_vmm_present")
                .iterator()
                .next()
                .split("\n")[0];
        // It's either 0 or 1 when sysctl property exists, some error string otherwise
        return vmmPresent.equals("1");
    }

    private static void skipIfProfilerNotSupport() {
        Assume.assumeTrue(xctraceExists());
    }

    private static void skipIfRunningInsideVirtualMachine() {
        Assume.assumeFalse(isInsideVM());
    }

    private void checkProfiling(int forks) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceNormProfiler.class)
                .forks(forks)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();
        double instructions = checkedGetAny(sr, "Instructions",
                "FIXED_INSTRUCTIONS", "INST_ALL", "INST_RETIRED.ANY", "INST_RETIRED.ANY_P");
        double cycles = checkedGetAny(sr, "Cycles", "FIXED_CYCLES",
                "CORE_ACTIVE_CYCLE", "CPU_CLK_UNHALTED.THREAD", "CPU_CLK_UNHALTED.THREAD_P");
        double branches = checkedGetAny(sr, "INST_BRANCH", "BR_INST_RETIRED.ALL_BRANCHES",
                "BR_INST_RETIRED.ALL_BRANCHES_PEBS");
        double missedBranches = checkedGetAny(sr, "BRANCH_MISPRED_NONSPEC", "BR_MISP_RETIRED.ALL_BRANCHES",
                "BR_MISP_RETIRED.ALL_BRANCHES_PS");

        Assert.assertNotEquals(0D, instructions, 0D);
        Assert.assertNotEquals(0D, cycles, 0D);
        Assert.assertNotEquals(0D, branches, 0D);
        Assert.assertNotEquals(0D, missedBranches, 0D);

        double cpi = ProfilerTestUtils.checkedGet(sr, "CPI").getScore();
        double ipc = ProfilerTestUtils.checkedGet(sr, "IPC").getScore();
        double branchMissRatio = ProfilerTestUtils.checkedGet(sr, "Branch miss ratio").getScore();

        Assert.assertNotEquals(0D, ipc, 0D);
        Assert.assertNotEquals(0D, cpi, 0D);
        Assert.assertNotEquals(0D, branchMissRatio, 0D);
    }

    @Test
    public void testDefaultArguments() throws Exception {
        skipIfProfilerNotSupport();
        skipIfRunningInsideVirtualMachine();

        checkProfiling(1);
    }

    @Test
    public void testFailWithNonExistentTemplate() {
        skipIfProfilerNotSupport();

        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceNormProfiler.class, "template=NON_EXISTENT_TEMPLATE")
                .forks(1)
                .build();
        Assert.assertThrows("No results returned", RunnerException.class, () -> new Runner(opts).runSingle());
    }

    @Test
    public void testUnsupportedTemplate() {
        skipIfProfilerNotSupport();
        skipIfRunningInsideVirtualMachine();

        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceNormProfiler.class, "template=CPU Profiler")
                .forks(1)
                .build();
        Assert.assertThrows("Table \"counters-profile\" was not found in the trace results.",
                IllegalStateException.class, () -> new Runner(opts).runSingle());
    }

    @Test
    public void testUseCustomTemplate() throws Exception {
        skipIfProfilerNotSupport();
        skipIfRunningInsideVirtualMachine();

        RunResult result;
        File templateFile = FileUtils.extractFromResource("/default.instruments.template.xml");
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(XCTraceNormProfiler.class, "template=" + templateFile.getAbsolutePath())
                .forks(1)
                .build();
        try {
            result = new Runner(opts).runSingle();
        } finally {
            templateFile.delete();
        }

        Map<String, Result> sr = result.getSecondaryResults();
        double instructions = checkedGetAny(sr, "Instructions",
                "FIXED_INSTRUCTIONS", "INST_ALL", "INST_RETIRED.ANY", "INST_RETIRED.ANY_P");
        Assert.assertNotEquals(0D, instructions, 0D);
    }

    @Test
    public void testMultipleForks() throws Exception {
        skipIfProfilerNotSupport();
        skipIfRunningInsideVirtualMachine();

        checkProfiling(2);
    }

    @Test
    public void testConstructorThrowsWhenXCTraceDoesNotExist() {
        Assume.assumeFalse(xctraceExists());
        Assert.assertThrows(ProfilerException.class, () -> new XCTraceNormProfiler(""));
    }

    private static double checkedGetAny(Map<String, Result> results, String... keys) {
        for (String key : keys) {
            Result value = results.get(key);
            if (value != null) return value.getScore();
        }
        throw new IllegalStateException(
                "Results does not include any of these keys: " + String.join(", ", keys));
    }
}
