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
package org.openjdk.jmh.it.dagorder;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests global setup -> run -> tearDown sequence.
 */
public class ThreadIterationDagOrderTest {

    public static final AtomicInteger TICKER = new AtomicInteger();

    private static volatile int s1setup, s1teardown;
    private static volatile int s2setup, s2teardown;
    private static volatile int s3setup, s3teardown;
    private static volatile int run;

    @State(Scope.Thread)
    public static class S3 {

        @Setup(Level.Iteration)
        public void setupInstance() {
            s3setup = TICKER.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDownInstance() {
            s3teardown = TICKER.incrementAndGet();
        }
    }

    @State(Scope.Thread)
    public static class S2 {

        @Setup(Level.Iteration)
        public void setupInstance(S3 s3) {
            s2setup = TICKER.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDownInstance() {
            s2teardown = TICKER.incrementAndGet();
        }
    }

    @State(Scope.Thread)
    public static class S1 {

        @Setup(Level.Iteration)
        public void setupInstance(S2 s2) {
            s1setup = TICKER.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDownInstance() {
            s1teardown = TICKER.incrementAndGet();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(0)
    @Threads(1)
    public void test(S1 state) {
        run = TICKER.incrementAndGet();
        Fixtures.work();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .shouldFailOnError(true)
                    .syncIterations(false)
                    .build();
            new Runner(opt).run();


            Assert.assertTrue(s3setup + " < " + s2setup, s3setup < s2setup);
            Assert.assertTrue(s2setup + " < " + s1setup, s2setup < s1setup);
            Assert.assertTrue(s1setup + " < " + run, s1setup < run);

            Assert.assertTrue(run + " < " + s1teardown, run < s1teardown);
            Assert.assertTrue(s1teardown + " < " + s2teardown, s1teardown < s2teardown);
            Assert.assertTrue(s2teardown + " < " + s3teardown, s2teardown < s3teardown);
        }
    }
}
