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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Group)
public class JMHSample_30_Interrupts {

    /*
     * JMH can also detect when threads are stuck in the benchmarks, and try
     * to forcefully interrupt the benchmark thread. JMH tries to do that
     * when it is arguably sure it would not affect the measurement.
     */

    /*
     * In this example, we want to measure the simple performance characteristics
     * of the ArrayBlockingQueue. Unfortunately, doing that without a harness
     * support will deadlock one of the threads, because the executions of
     * take/put are not paired perfectly. Fortunately for us, both methods react
     * to interrupts well, and therefore we can rely on JMH to terminate the
     * measurement for us. JMH will notify users about the interrupt actions
     * nevertheless, so users can see if those interrupts affected the measurement.
     *
     * This is a variant of org.openjdk.jmh.samples.JMHSample_18_Control, but without
     * the explicit control objects. This example is suitable for the methods which
     * react to interrupts gracefully.
     */

    private BlockingQueue<Integer> q;

    @Setup
    public void setup() {
        q = new ArrayBlockingQueue<Integer>(1);
    }

    @Group("Q")
    @Benchmark
    public Integer take() throws InterruptedException {
        return q.take();
    }

    @Group("Q")
    @Benchmark
    public void put() throws InterruptedException {
        q.put(42);
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar ".*JMHSample_30.*" -wi 5 -i 5 -t 2 -f 5
     *    (we requested 5 warmup iterations, 5 iterations, 2 threads, and 5 forks)
     *
     * b) Via the Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_30_Interrupts.class.getSimpleName() + ".*")
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(2)
                .forks(5)
                .build();

        new Runner(opt).run();
    }

}
