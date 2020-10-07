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
package org.openjdk.jmh.it.batchsize;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class OpsPerInvSanityTest {

    private static final int SLEEP_TIME_MS = 5;

    @AuxCounters(AuxCounters.Type.EVENTS)
    @State(Scope.Thread)
    public static class RawCounter {
        public long time;
        public int ops;
    }

    @Benchmark
    public void test(RawCounter cnt) throws InterruptedException {
        long start = System.nanoTime();
        TimeUnit.MILLISECONDS.sleep(SLEEP_TIME_MS);
        long stop = System.nanoTime();
        cnt.ops++;
        cnt.time += (stop - start);
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (int opsPerInv : new int[] {1, 10, 100}) {
            for (Mode m : Mode.values()) {
                if (m == Mode.All) continue;
                doWith(m, opsPerInv);
            }
        }
    }

    private void doWith(Mode mode, int opsPerInv) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass()))
            .shouldFailOnError(true)
            .warmupIterations(0)
            .measurementTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .forks(1)
            .timeUnit(TimeUnit.MILLISECONDS)
            .operationsPerInvocation(opsPerInv)
            .mode(mode)
            .build();
        RunResult run = new Runner(opt).runSingle();

        final double TOLERANCE = 0.30;

        Map<String, Result> srs = run.getSecondaryResults();

        Assert.assertNotNull("Ops counter is available for " + mode, srs.get("ops"));
        Assert.assertNotNull("Time counter is available for " + mode, srs.get("time"));

        double realOps = srs.get("ops").getScore();
        double realTime = srs.get("time").getScore() / 1_000_000;

        double actualScore = run.getPrimaryResult().getStatistics().getMean();
        double expectedScore;

        switch (mode) {
            case Throughput:
                expectedScore = 1.0 * (realOps * opsPerInv) / realTime;
                break;
            case AverageTime:
            case SampleTime:
                expectedScore = 1.0 * realTime / (realOps * opsPerInv);
                break;
            case SingleShotTime:
                expectedScore = 1.0 * realTime / realOps;
                break;
            default:
                expectedScore = Double.NaN;
                actualScore   = Double.NaN;
                Assert.fail("Unhandled mode: " + mode);
        }

        Assert.assertTrue(mode + ", " + opsPerInv + ": " + expectedScore + " vs " + actualScore,
                Math.abs(1 - actualScore / expectedScore) < TOLERANCE);
    }

}
