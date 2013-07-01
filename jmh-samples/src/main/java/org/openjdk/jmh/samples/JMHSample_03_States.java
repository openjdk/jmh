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

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class JMHSample_03_States {

    /*
     * Most of the time, you need to maintain some of the state while
     * the benchmark is running. Since JMH is heavily used to build
     * concurrent benchmarks, we opted to the explicit notion
     * of state-bearing objects.
     *
     * Below are two state objects. Their class names are not essential,
     * it matters they are marked with @State. These objects will be
     * instantiated on demand, and reused during the entire benchmark trial.
     *
     * The important property is that state is always instantiated by
     * one of those benchmark threads which will then have the access
     * to that state. That means you can initialize the fields as if you do
     * that in worker threads (ThreadLocals are yours, etc).
     */

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        double x = Math.PI;
    }

    @State(Scope.Thread)
    public static class ThreadState {
        double x = Math.PI;
    }

    /*
     * Benchmark methods can reference the states, and JMH will inject
     * the appropriate states while calling these methods. You can have
     * no states at all, or have only one state, or have multiple states
     * referenced. This makes building multi-threaded benchmark a breeze.
     *
     * For this exercise, we have two methods.
     */

    @GenerateMicroBenchmark
    public void measureUnshared(ThreadState state) {
        // All benchmark threads will call in this method.
        //
        // However, since ThreadState is the Scope.Thread, each thread
        // will have it's own copy of the state, and this benchmark
        // will measure unshared case.
        state.x++;
    }

    @GenerateMicroBenchmark
    public void measureShared(BenchmarkState state) {
        // All benchmark threads will call in this method.
        //
        // Since BenchmarkState is the Scope.Benchmark, all threads
        // will share the state instance, and we will end up measuring
        // shared case.
        state.x++;
    }

    /*
     * HOW TO RUN THIS TEST:
     *
     * You can run this test with:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_03.*" -i 5 -r 1s -t 4
     *    (we requested 5 iterations, 1 sec each, with 4 threads)
     *
     * You are expected to see the drastic difference in shared and unshared cases,
     * because you either contend for single memory location, or not. This effect
     * is more articulated on large machines.
     */

}
