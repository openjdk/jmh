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
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.MemPoolProfiler;
import org.openjdk.jmh.profile.SafepointsProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class SafepointsProfilerTest {

    @Benchmark
    public int[] allocate() {
        return new int[1_000_000];
    }

    @Test
    public void test() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(SafepointsProfiler.class)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();

        double interval = ProfilerTestUtils.checkedGet(sr, "safepoints.interval").getScore();
        double pauseTotal = ProfilerTestUtils.checkedGet(sr, "safepoints.pause").getScore();
        double ttspTotal = ProfilerTestUtils.checkedGet(sr, "safepoints.ttsp").getScore();

        double pauseCount = ProfilerTestUtils.checkedGet(sr, "safepoints.pause.count").getScore();
        double ttspCount = ProfilerTestUtils.checkedGet(sr, "safepoints.ttsp.count").getScore();

        Assert.assertNotEquals(0D, pauseTotal, 0D);
        Assert.assertNotEquals(0D, ttspTotal, 0D);

        Assert.assertNotEquals(0D, pauseCount, 0D);
        Assert.assertNotEquals(0D, ttspCount, 0D);
        Assert.assertEquals(ttspCount, pauseCount, 0D);

        if (interval < 3000) {
            throw new IllegalStateException("Interval time is too low. " +
                    " Interval: " + interval);
        }

        if (ttspTotal > interval) {
            throw new IllegalStateException("TTSP time is larger than interval time. " +
                    "TTSP: " + ttspTotal + ", Interval: " + interval);
        }

        if (pauseTotal > interval) {
            throw new IllegalStateException("Pause time is larger than interval time. " +
                    "Pause: " + pauseTotal + ", Interval: " + interval);
        }

        if (ttspTotal > pauseTotal) {
            throw new IllegalStateException("TTSP time is larger than pause time. " +
                    "TTSP: " + ttspTotal + ", Pause: " + pauseTotal);
        }

        double lastPause = 0;
        double lastTTSP = 0;
        for (String suff : new String[] {"0.00", "0.50", "0.90", "0.95", "0.99", "0.999", "0.9999", "1.00"}) {
            double curPause = ProfilerTestUtils.checkedGet(sr, "safepoints.pause.p" + suff).getScore();
            double curTTSP = ProfilerTestUtils.checkedGet(sr, "safepoints.ttsp.p" + suff).getScore();
            if (curPause < lastPause) {
                throw new IllegalStateException("pause.p" + suff + " is not monotonic");
            }
            if (curTTSP < lastTTSP) {
                throw new IllegalStateException("ttsp.p" + suff + " is not monotonic");
            }
            lastPause = curPause;
            lastTTSP = curTTSP;
        }

    }

}
