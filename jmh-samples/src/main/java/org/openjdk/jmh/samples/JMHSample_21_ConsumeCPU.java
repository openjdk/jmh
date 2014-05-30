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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JMHSample_21_ConsumeCPU {

    /*
     * At times you require the test to burn some of the cycles doing nothing.
     * In many cases, you *do* want to burn the cycles instead of waiting.
     *
     * For these occasions, we have the infrastructure support. Blackholes
     * can not only consume the values, but also the time! Run this test
     * to get familiar with this part of JMH.
     *
     * (Note we use static method because most of the use cases are deep
     * within the testing code, and propagating blackholes is tedious).
     */

    @GenerateMicroBenchmark
    public void consume_0000() {
        Blackhole.consumeCPU(0);
    }

    @GenerateMicroBenchmark
    public void consume_0001() {
        Blackhole.consumeCPU(1);
    }

    @GenerateMicroBenchmark
    public void consume_0002() {
        Blackhole.consumeCPU(2);
    }

    @GenerateMicroBenchmark
    public void consume_0004() {
        Blackhole.consumeCPU(4);
    }

    @GenerateMicroBenchmark
    public void consume_0008() {
        Blackhole.consumeCPU(8);
    }

    @GenerateMicroBenchmark
    public void consume_0016() {
        Blackhole.consumeCPU(16);
    }

    @GenerateMicroBenchmark
    public void consume_0032() {
        Blackhole.consumeCPU(32);
    }

    @GenerateMicroBenchmark
    public void consume_0064() {
        Blackhole.consumeCPU(64);
    }

    @GenerateMicroBenchmark
    public void consume_0128() {
        Blackhole.consumeCPU(128);
    }

    @GenerateMicroBenchmark
    public void consume_0256() {
        Blackhole.consumeCPU(256);
    }

    @GenerateMicroBenchmark
    public void consume_0512() {
        Blackhole.consumeCPU(512);
    }

    @GenerateMicroBenchmark
    public void consume_1024() {
        Blackhole.consumeCPU(1024);
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * Note the single token is just a few cycles, and the more tokens
     * you request, then more work is spent (almost linearly)
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_21.*" -w 1 -i 5 -f 1
     *
     * b) Via the Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_21_ConsumeCPU.class.getSimpleName() + ".*")
                .warmupIterations(1)
                .measurementIterations(5)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
