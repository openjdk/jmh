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
package org.openjdk.jmh.annotations;

/** Level enumeration for Setup and TearDown */
public enum Level {

    /**
     * Trial level.
     *
     * To be executed each run of the benchmark
     */
    Trial,

    /**
     * Iteration level.
     *
     * To be executued for each iteration in a benchmark execution
     */
    Iteration,

    /**
     * Invocation level.
     *
     * To be executed for each benchmark method execution.
     *
     * WARNING: HERE BE DRAGONS!
     * MAKE SURE YOU UNDERSTAND THE REASONING AND THE IMPLICATIONS
     * OF THE WARNINGS BELOW BEFORE EVEN CONSIDERING USING THIS LEVEL.
     *
     * It is almost never a good idea to use this in nano- and micro-benchmarks.
     *
     * <p>WARNING #1: In order to subtract the helper time from the
     * benchmark itself, we will have to have at least two timestamps
     * per *each* benchmark invocation. If the benchmarked method is
     * small, then we saturate the system with timestamp requests, which
     * *both* make timestamp requests the critical part of the benchmark
     * time, and inhibit workload scalability, introducing the artificial
     * scalability bottleneck.
     *
     * Also, the hiccups in measurement can be hidden from these individual
     * timing measurement, which can introduce inconsistent results. The largest
     * caveat is measuring oversaturated system where the descheduling events
     * will be missed, and the benchmark will perceive the fictionally large
     * throughput.
     *
     * <p>WARNING #2: In order to maintain the basic interference behavior
     * of other Levels (e.g. the State(Scope.Benchmark) should only fire the
     * helper method once per invocation, regardless of the thread count),
     * we have to arbitrate the access to the state between worker thread,
     * and do that on *critical path*, thus further offsetting the measurement.
     *
     * <p>WARNING #3: Current implementation in JMH allows the helper method
     * execution to overlap with the benchmark method itself in order to simplify
     * arbitrage. (To be redefined in future).
     */
    Invocation,
}
