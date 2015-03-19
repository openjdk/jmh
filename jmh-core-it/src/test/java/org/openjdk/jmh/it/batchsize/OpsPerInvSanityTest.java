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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Thread)
public class OpsPerInvSanityTest {

    private static final AtomicInteger invCount = new AtomicInteger();
    private static volatile long startTime;
    private static volatile long stopTime;

    @Setup(Level.Iteration)
    public void beforeIter() {
        startTime = System.nanoTime();
    }

    @TearDown(Level.Iteration)
    public void afterIter() {
        stopTime = System.nanoTime();
    }

    @Benchmark
    @Fork(0)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void test() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(1);
        invCount.incrementAndGet();
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
        invCount.set(0);

        Options opt = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass()))
            .shouldFailOnError(true)
            .operationsPerInvocation(opsPerInv)
            .mode(mode)
            .build();
        RunResult run = new Runner(opt).runSingle();

        final double TOLERANCE = 0.30;

        double expectedScore = 0.0;

        double time = stopTime - startTime;
        double calls = invCount.get();

        switch (mode) {
            case Throughput:
                expectedScore = (calls * opsPerInv) / time;
                break;
            case AverageTime:
            case SampleTime:
                expectedScore = time / (calls * opsPerInv);
                break;
            case SingleShotTime:
                expectedScore = time;
                break;
            default:
                Assert.fail("Unhandled mode: " + mode);
        }

        double actualScore = run.getPrimaryResult().getScore();
        Assert.assertTrue(mode + ", " + opsPerInv + ": " + expectedScore + " vs " + actualScore,
                Math.abs(1 - actualScore / expectedScore) < TOLERANCE);
    }

}
