/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.JDKVersion;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-Xms1g", "-Xmx1g", "-XX:+UseParallelGC"})
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class GCProfilerSeparateThreadTest {

    static final int SIZE = 1_000_000;

    @Benchmark
    public void separateAlloc(Blackhole bh) throws InterruptedException {
        Thread t = new Thread(() -> bh.consume(new byte[SIZE]));
        t.start();
        t.join();
    }

    @Test
    public void testDefault() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(GCProfiler.class)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();
        double allocRateNormB = ProfilerTestUtils.checkedGet(sr, "·gc.alloc.rate.norm").getScore();

        String msg = "Reported by profiler: " + allocRateNormB + ", target: " + SIZE;

        // Allow 1% slack
        if (accurateProfiler() && (Math.abs(1 - allocRateNormB / SIZE) > 0.01)) {
            Assert.fail("Allocation rate failure. Reported by profiler: " + allocRateNormB + ", target: " + SIZE);
        }

        System.out.println(msg);
    }

    private boolean accurateProfiler() {
        // Change to this version-sensing code after JDK 21 releases:
        // return JDKVersion.parseMajor(System.getProperty("java.version")) >= 21;

        // Try to sense the existence of the accurate method:
        try {
            Class.forName("com.sun.management.ThreadMXBean").getMethod("getTotalThreadAllocatedBytes");
            return true;
        } catch (Exception e) {
            // Fall through
        }

        return false;
    }
}
