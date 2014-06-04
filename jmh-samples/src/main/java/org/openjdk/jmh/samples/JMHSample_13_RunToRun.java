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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class JMHSample_13_RunToRun {

    /*
     * Forking also allows to estimate run-to-run variance.
     *
     * JVMs are complex systems, and the non-determinism is inherent for them.
     * This requires us to always account the run-to-run variance as the one
     * of the effects in our experiments.
     *
     * Luckily, forking aggregates the results across several JVM launches.
     */

    /*
     * In order to introduce readily measurable run-to-run variance, we build
     * the workload which performance differs from run to run. Note that many workloads
     * will have the similar behavior, but we do that artificially to make a point.
     */

    @State(Scope.Thread)
    public static class SleepyState {
        public long sleepTime;

        @Setup
        public void setup() {
            sleepTime = (long) (Math.random() * 1000);
        }
    }

    /*
     * Now, we will run this different number of times.
     */

    @Benchmark
    @Fork(1)
    public void baseline(SleepyState s) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(s.sleepTime);
    }

    @Benchmark
    @Fork(5)
    public void fork_1(SleepyState s) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(s.sleepTime);
    }

    @Benchmark
    @Fork(20)
    public void fork_2(SleepyState s) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(s.sleepTime);
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * Note the baseline is random within [0..1000] msec; and both forked runs
     * are estimating the average 500 msec with some confidence.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar ".*JMHSample_13.*" -wi 0 -i 3
     *    (we requested no warmup, 3 measurement iterations)
     *
     * b) Via the Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_13_RunToRun.class.getSimpleName() + ".*")
                .warmupIterations(0)
                .measurementIterations(5)
                .build();

        new Runner(opt).run();
    }

}
