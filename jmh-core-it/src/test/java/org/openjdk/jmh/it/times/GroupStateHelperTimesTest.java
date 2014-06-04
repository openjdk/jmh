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
package org.openjdk.jmh.it.times;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
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

public class GroupStateHelperTimesTest {

    @State(Scope.Group)
    public static class MyState {

        private final AtomicInteger countInvocations = new AtomicInteger();
        private final AtomicInteger countSetupRun1 = new AtomicInteger();
        private final AtomicInteger countSetupRun2 = new AtomicInteger();
        private final AtomicInteger countSetupIteration1 = new AtomicInteger();
        private final AtomicInteger countSetupIteration2 = new AtomicInteger();
        private final AtomicInteger countSetupInvocation1 = new AtomicInteger();
        private final AtomicInteger countSetupInvocation2 = new AtomicInteger();
        private final AtomicInteger countTearDownRun1 = new AtomicInteger();
        private final AtomicInteger countTearDownRun2 = new AtomicInteger();
        private final AtomicInteger countTearDownIteration1 = new AtomicInteger();
        private final AtomicInteger countTearDownIteration2 = new AtomicInteger();
        private final AtomicInteger countTearDownInvocation1 = new AtomicInteger();
        private final AtomicInteger countTearDownInvocation2 = new AtomicInteger();

        @Setup(Level.Trial)
        public void setup1() {
            countSetupRun1.incrementAndGet();
        }

        @Setup(Level.Trial)
        public void setup2() {
            countSetupRun2.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void setup3() {
            countSetupIteration1.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void setup4() {
            countSetupIteration2.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void setup5() {
            countSetupInvocation1.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void setup6() {
            countSetupInvocation2.incrementAndGet();
        }

        @TearDown(Level.Trial)
        public void tearDown1() {
            countTearDownRun1.incrementAndGet();
        }

        @TearDown(Level.Trial)
        public void tearDown2() {
            countTearDownRun2.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDown3() {
            countTearDownIteration1.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDown4() {
            countTearDownIteration2.incrementAndGet();
        }

        @TearDown(Level.Invocation)
        public void tearDown5() {
            countTearDownInvocation1.incrementAndGet();
        }

        @TearDown(Level.Invocation)
        public void tearDown6() {
            countTearDownInvocation2.incrementAndGet();
        }

        @TearDown(Level.Trial)
        public void tearDownLATEST() { // this name ensures this is the latest teardown to run
            Assert.assertEquals("Setup1 called once", 1, countSetupRun1.get());
            Assert.assertEquals("Setup2 called once", 1, countSetupRun2.get());
            Assert.assertEquals("Setup3 called twice", 2, countSetupIteration1.get());
            Assert.assertEquals("Setup4 called twice", 2, countSetupIteration2.get());

            // These asserts make no sense for Benchmark tests
//            Assert.assertEquals("Setup5 = invocation count", countInvocations.get(), countSetupInvocation1.get());
//            Assert.assertEquals("Setup6 = invocation count", countInvocations.get(), countSetupInvocation2.get());

            Assert.assertEquals("TearDown1 called once", 1, countTearDownRun1.get());
            Assert.assertEquals("TearDown2 called once", 1, countTearDownRun2.get());
            Assert.assertEquals("TearDown3 called twice", 2, countTearDownIteration1.get());
            Assert.assertEquals("TearDown4 called twice", 2, countTearDownIteration2.get());

            // These two asserts make no sense for Benchmark tests.
//            Assert.assertEquals("TearDown5 = invocation count", countInvocations.get(), countTearDownInvocation1.get());
//            Assert.assertEquals("TearDown6 = invocation count", countInvocations.get(), countTearDownInvocation2.get());
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 2, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    @Group("T")
    @GroupThreads(2)
    public void test(MyState state) {
        Fixtures.work();
        state.countInvocations.incrementAndGet();
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
