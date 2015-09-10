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

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class LevelTrialBench {

    @State(Scope.Benchmark)
    public static class BenchmarkSetup {
        @Setup(Level.Trial)
        public void setup() {}
    }

    @State(Scope.Benchmark)
    public static class BenchmarkTeardown {
        @TearDown(Level.Trial)
        public void tearDown() {}
    }

    @State(Scope.Thread)
    public static class ThreadSetup {
        @Setup(Level.Trial)
        public void setup() {}
    }

    @State(Scope.Thread)
    public static class ThreadTeardown {
        @TearDown(Level.Trial)
        public void tearDown() {}
    }

    @State(Scope.Group)
    public static class GroupSetup {
        @Setup(Level.Trial)
        public void setup() {}
    }

    @State(Scope.Group)
    public static class GroupTeardown {
        @TearDown(Level.Trial)
        public void tearDown() {}
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void raw() {
        // do nothing
    }

    @Benchmark
    public void benchmark_setup(BenchmarkSetup g) {}

    @Benchmark
    public void benchmark_teardown(BenchmarkTeardown g) {}

    @Benchmark
    @Group("group_setup")
    public void group_setup_1(GroupSetup g) {}

    @Benchmark
    @Group("group_setup")
    public void group_setup_2(GroupSetup g) {}

    @Benchmark
    @Group("group_teardown")
    public void group_teardown_1(GroupTeardown g) {}

    @Benchmark
    @Group("group_teardown")
    public void group_teardown_2(GroupTeardown g) {}

    @Benchmark
    public void thread_setup(ThreadSetup g) {}

    @Benchmark
    public void thread_teardown(ThreadTeardown g) {}

}
