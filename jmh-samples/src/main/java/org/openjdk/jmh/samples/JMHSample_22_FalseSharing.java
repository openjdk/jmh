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
package org.openjdk.jmh.samples;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class JMHSample_22_FalseSharing {

    /*
     * One of the unusual thing that can bite you back is false sharing.
     * If two threads access (and possibly modify) the adjacent values
     * in memory, chances are, they are modifying the values on the same
     * cache line. This can yield significant (artificial) slowdowns.
     *
     * JMH helps you to alleviate this: @States are automatically padded.
     * This padding does not extend to any referenced objects though,
     * as we will see in this example. You have to take care of this on
     * your own.
     */

    /*
     * Suppose we have the array of integer counters:
     */

    @State(Scope.Benchmark)
    public static class Shared {
        int[] cs;

        @Setup
        public void prepare() {
            cs = new int[4096]; // should be enough
        }
    }

    /*
     * ...and each thread gets it's own index in the shared array, either
     * dense index, or sparse index:
     */

    @State(Scope.Thread)
    public static class LocalDense {
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private int index = COUNTER.incrementAndGet();
    }

    @State(Scope.Thread)
    public static class LocalSparse {
        private static final int CACHE_LINE_SIZE = 64;
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private int index = COUNTER.addAndGet(CACHE_LINE_SIZE);
    }

    /*
     * Then, running with different amount of threads, we can clearly see
     * the difference between dense and sparse cases:
     */

    @GenerateMicroBenchmark
    @Threads(1)
    public void test_dense_01(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(2)
    public void test_dense_02(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(4)
    public void test_dense_04(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(8)
    public void test_dense_08(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(16)
    public void test_dense_16(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(32)
    public void test_dense_32(Shared s, LocalDense l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(1)
    public void test_sparse_01(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(2)
    public void test_sparse_02(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(4)
    public void test_sparse_04(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(8)
    public void test_sparse_08(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(16)
    public void test_sparse_16(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    @GenerateMicroBenchmark
    @Threads(32)
    public void test_sparse_32(Shared s, LocalSparse l) {
        s.cs[l.index]++;
    }

    /*
     * HOW TO RUN THIS TEST:
     *
     * You can run this test with:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_22.*"
     *
     * Note the slowdowns.
     */

}
