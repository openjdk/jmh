/*
 * Copyright (c) 2022, Red Hat, Inc. All rights reserved.
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
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class GCProfilerAllocRateTest {

    @Benchmark
    public Object allocate() {
        return new byte[1000];
    }

    @Test
    public void testDefault() throws RunnerException {
        testWith("");
    }

    @Test
    public void testAlloc() throws RunnerException {
        testWith("alloc=true");
    }

    @Test
    public void testAll() throws RunnerException {
        testWith("alloc=true;churn=true");
    }

    private void testWith(String initLine) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(GCProfiler.class, initLine)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        double opsPerSec = rr.getPrimaryResult().getScore();

        Map<String, Result> sr = rr.getSecondaryResults();
        double allocRateMB = sr.get("·gc.alloc.rate").getScore();
        double allocRateNormB = sr.get("·gc.alloc.rate.norm").getScore();
        double allocRatePrimaryMB = opsPerSec * allocRateNormB / 1024 / 1024;

        // Allow 20% slack
        if (Math.abs(1 - allocRatePrimaryMB / allocRateMB) > 0.2) {
            Assert.fail("Allocation rates disagree. " +
                    "Reported by profiler: " + allocRateMB +
                    ", computed from primary score: " + allocRatePrimaryMB);
        }
    }
}
