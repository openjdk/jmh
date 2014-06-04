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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests if harness honors batch size annotation settings.
 */
@State(Scope.Thread)
public class SingleShotBatchApi15Test {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASUREMENT_ITERATIONS = 1;
    private static final int WARMUP_BATCH = 1;
    private static final int MEASUREMENT_BATCH = 5;

    private final AtomicInteger iterationCount = new AtomicInteger();
    private final AtomicInteger batchCount = new AtomicInteger();

    @Setup(Level.Iteration)
    public void setup() {
        iterationCount.incrementAndGet();
        batchCount.set(0);
    }

    private boolean isWarmup() {
        return iterationCount.get() <= WARMUP_ITERATIONS;
    }

    @TearDown(Level.Iteration)
    public void tearDownIter() {
        if(isWarmup()) {
            Assert.assertEquals(WARMUP_BATCH + " batch size expected", WARMUP_BATCH, batchCount.get());
        } else {
            Assert.assertEquals(MEASUREMENT_BATCH + " batch size expected", MEASUREMENT_BATCH, batchCount.get());
        }
    }

    @TearDown
    public void tearDownTrial() {
        Assert.assertEquals((MEASUREMENT_ITERATIONS+WARMUP_ITERATIONS)+" iterations expected", (MEASUREMENT_ITERATIONS+WARMUP_ITERATIONS), iterationCount.get());
    }

    @Benchmark
    @Fork(1)
    public void test() {
        Fixtures.work();
        batchCount.incrementAndGet();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .warmupIterations(WARMUP_ITERATIONS)
                .warmupBatchSize(WARMUP_BATCH)
                .measurementIterations(MEASUREMENT_ITERATIONS)
                .measurementBatchSize(MEASUREMENT_BATCH)
                .mode(Mode.SingleShotTime)
                .build();
        new Runner(opt).run();
    }

}
