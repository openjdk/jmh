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
package org.openjdk.jmh.samples;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class JMHSample_29_StatesDAG {

    /**
     * WARNING:
     * THIS IS AN EXPERIMENTAL FEATURE, BE READY FOR IT BECOME REMOVED WITHOUT NOTICE!
     */

    /**
     * There are weird cases when the benchmark state is more cleanly described
     * by the set of @States, and those @States reference each other. JMH allows
     * linking @States in directed acyclic graphs (DAGs) by referencing @States
     * in helper method signatures. (Note that {@link org.openjdk.jmh.samples.JMHSample_28_BlackholeHelpers}
     * is just a special case of that.
     *
     * Following the interface for @GenerateMicroBenchmark calls, all @Setups for
     * referenced @State-s are fired before it becomes accessible to current @State.
     * Similarly, no @TearDown methods are fired for referenced @State before
     * current @State is done with it.
     */

    /*
     * This is a model case, and it might not be a good benchmark.
     * // TODO: Replace it with the benchmark which does something useful.
     */

    public static class Counter {
        int x;

        public int inc() {
            return x++;
        }

        public void dispose() {
            // pretend this is something really useful
        }
    }

    /*
     * Shared state maintains the set of Counters, and worker threads should
     * poll their own instances of Counter to work with. However, it should only
     * be done once, and therefore, Local state caches it after requesting the
     * counter from Shared state.
     */

    @State(Scope.Benchmark)
    public static class Shared {
        List<Counter> all;
        Queue<Counter> available;

        @Setup
        public synchronized void setup() {
            all = new ArrayList<Counter>();
            for (int c = 0; c < 10; c++) {
                all.add(new Counter());
            }

            available = new LinkedList<Counter>();
            available.addAll(all);
        }

        @TearDown
        public synchronized void tearDown() {
            for (Counter c : all) {
                c.dispose();
            }
        }

        public synchronized Counter getMine() {
            return available.poll();
        }
    }

    @State(Scope.Thread)
    public static class Local {
        Counter cnt;

        @Setup
        public void setup(Shared shared) {
            cnt = shared.getMine();
        }
    }

    @GenerateMicroBenchmark
    public int test(Local local) {
        return local.cnt.inc();
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_29.*"
     *
     * b) Via the Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_29_StatesDAG.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }


}
