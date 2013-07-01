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
package org.openjdk.jmh.it.races;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;

import java.util.concurrent.TimeUnit;

/**
 * Baseline test:
 * Checks if assertions are propagated back to integration tests.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class RaceBenchmarkStateIterationTest {

    @State(Scope.Benchmark)
    public static class MyState {
        public int value = 2;

        @Setup(Level.Iteration)
        public void setup() {
            Assert.assertEquals("Setup", 2, value);
            value = 1;
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            Assert.assertEquals("TearDown", 1, value);
            value = 2;
        }
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 50, time = 10, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(4)
    public void test(MyState state) {
        Assert.assertEquals("Run", 1, state.value);
        Fixtures.work();
    }

    @Test
    public void invoke() {
        Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe -f false");
    }

}
