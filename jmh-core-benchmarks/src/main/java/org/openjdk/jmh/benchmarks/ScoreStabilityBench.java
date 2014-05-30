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
package org.openjdk.jmh.benchmarks;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SingleShotTime)
@Warmup(iterations = 50)
@Measurement(iterations = 50)
@Threads(4)
public class ScoreStabilityBench {

    @State(Scope.Thread)
    public static class Sleep_X_State {
        @Setup(Level.Iteration)
        public void sleep() throws InterruptedException {
            // don't
        }
    }

    @State(Scope.Thread)
    public static class Sleep_X1_State {
        @Setup(Level.Iteration)
        public void sleep() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(1);
        }
    }

    @State(Scope.Thread)
    public static class Sleep_X10_State {
        @Setup(Level.Iteration)
        public void sleep() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(10);
        }
    }

    @State(Scope.Thread)
    public static class Sleep_X100_State {
        @Setup(Level.Iteration)
        public void sleep() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    @State(Scope.Thread)
    public static class Sleep_X500_State {
        @Setup(Level.Iteration)
        public void sleep() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    @GenerateMicroBenchmark
    public void test_X(Sleep_X_State s) {
        Blackhole.consumeCPU(10000);
    }

    @GenerateMicroBenchmark
    public void test_X1(Sleep_X1_State s) {
        Blackhole.consumeCPU(10000);
    }

    @GenerateMicroBenchmark
    public void test_X10(Sleep_X10_State x) {
        Blackhole.consumeCPU(10000);
    }

    @GenerateMicroBenchmark
    public void test_X100(Sleep_X100_State x) {
        Blackhole.consumeCPU(10000);
    }

    @GenerateMicroBenchmark
    public void test_X500(Sleep_X500_State x) {
        Blackhole.consumeCPU(10000);
    }

}
