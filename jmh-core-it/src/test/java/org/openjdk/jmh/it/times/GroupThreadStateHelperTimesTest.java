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

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests if harness executes setup, run, and tearDown in the same workers.
 */
@BenchmarkMode(Mode.All)
@Warmup(iterations = 5,  time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class GroupThreadStateHelperTimesTest {

    @State(Scope.Thread)
    public static class State1 {
        static final AtomicInteger setupTimes = new AtomicInteger();
        static final AtomicInteger tearDownTimes = new AtomicInteger();

        @Setup
        public void setup() {
            setupTimes.incrementAndGet();
        }

        @Setup
        public void tearDown() {
            tearDownTimes.incrementAndGet();
        }
    }

    @State(Scope.Thread)
    public static class State2 {
        static final AtomicInteger setupTimes = new AtomicInteger();
        static final AtomicInteger tearDownTimes = new AtomicInteger();

        @Setup
        public void setup() {
            setupTimes.incrementAndGet();
        }

        @Setup
        public void tearDown() {
            tearDownTimes.incrementAndGet();
        }
    }

    @Setup
    public void reset() {
        State1.setupTimes.set(0);
        State1.tearDownTimes.set(0);
        State2.setupTimes.set(0);
        State2.tearDownTimes.set(0);
    }

    @TearDown
    public void verify() {
        Assert.assertEquals(1, State1.setupTimes.get());
        Assert.assertEquals(1, State1.tearDownTimes.get());
        Assert.assertEquals(1, State2.setupTimes.get());
        Assert.assertEquals(1, State2.tearDownTimes.get());
    }

    @Benchmark
    @Group("T")
    public void test1(State1 state1) {
        Fixtures.work();
    }

    @Benchmark
    @Group("T")
    public void test2(State2 state2) {
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
