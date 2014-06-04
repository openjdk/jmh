/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.intraorder;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests if harness executes setup (instance) methods in lexicographical order.
 */
public class BenchmarkStateSetupOrderTest {

    @State(Scope.Benchmark)
    public static class MyState {

        private final AtomicInteger seq = new AtomicInteger();
        private int run1, run2, run3, runD;
        private int iter1, iter2, iter3, iterD;
        private int invoc1, invoc2, invoc3, invocD;

        @Setup(Level.Trial)
        public void run1() {
            run1 = seq.incrementAndGet();
        }

        @Setup(Level.Trial)
        public void run3() {
            run3 = seq.incrementAndGet();
        }

        @Setup(Level.Trial)
        public void run2() {
            run2 = seq.incrementAndGet();
        }

        @Setup(Level.Trial)
        public void run() {
            runD = seq.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void iteration1() {
            iter1 = seq.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void iteration3() {
            iter3 = seq.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void iteration2() {
            iter2 = seq.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void iteration() {
            iterD = seq.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void invocation1() {
            invoc1 = seq.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void invocation3() {
            invoc3 = seq.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void invocation2() {
            invoc2 = seq.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void invocation() {
            invocD = seq.incrementAndGet();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Assert.assertTrue("Trial(D) < Trial(1)", runD < run1);
            Assert.assertTrue("Trial(1) < Trial(2)", run1 < run2);
            Assert.assertTrue("Trial(2) < Trial(3)", run2 < run3);

            Assert.assertTrue("Iter(D) < Iter(1)", iterD < iter1);
            Assert.assertTrue("Iter(1) < Iter(2)", iter1 < iter2);
            Assert.assertTrue("Iter(2) < Iter(3)", iter2 < iter3);

            Assert.assertTrue("Invoc(D) < Invoc(1)", invocD < invoc1);
            Assert.assertTrue("Invoc(1) < Invoc(2)", invoc1 < invoc2);
            Assert.assertTrue("Invoc(2) < Invoc(3)", invoc2 < invoc3);
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void test(MyState state) {
        Fixtures.work();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }
}
