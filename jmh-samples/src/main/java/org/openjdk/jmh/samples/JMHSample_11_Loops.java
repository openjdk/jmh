/**
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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTimePerOp)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JMHSample_11_Loops {

    /*
     * It would be tempting for users to do loops within the benchmarked method.
     * (This is the bad thing Caliper taught everyone). This tests explains why
     * this is a bad idea.
     *
     * There is an argument that we would want to minimize the overhead for calling
     * the test method, etc. Don't buy this argument; we had seen that calling this
     * method is uber-fast.
     */

    /*
     * Suppose, we want to measure how much it takes to sum two integers:
     */

    int x = 1;
    int y = 2;

    /*
     * This is what you do with JMH.
     */

    @GenerateMicroBenchmark
    public int measureRight() {
        return (x + y);
    }

    /*
     * These tests emulate the naive looping.
     * This is the Caliper-style benchmark.
     */
    private int reps(int reps) {
        int s = 0;
        for (int i = 0; i < reps; i++) {
            s += (x + y);
        }
        return s;
    }

    /*
     * We would like to measure this with different repetitions count.
     * Special invocation is used in conjunction with AverageTimePerOp
     * to get the individual operation cost.
     */

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1)
    public int measureWrong_1() {
        return reps(1);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(10)
    public int measureWrong_10() {
        return reps(10);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(100)
    public int measureWrong_100() {
        return reps(100);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1000)
    public int measureWrong_1000() {
        return reps(1000);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(10000)
    public int measureWrong_10000() {
        return reps(10000);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(100000)
    public int measureWrong_100000() {
        return reps(100000);
    }

    /*
     * HOW TO RUN THIS TEST:
     *
     * You can run this test with:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_11.*" -i 5 -r 1s
     *    (we requested 5 iterations, 1 sec each)
     *
     * You might notice the larger the repetitions count, the lower the "perceived"
     * cost of the operation being measured. Up to the point we do each addition with 1/20 ns,
     * well beyond what hardware can actually do.
     *
     * This happens because the loop is heavily unrolled/pipelined, and the operation
     * to be measured is hoisted from the loop. Morale: don't overuse loops, rely on JMH
     * to get the measurement right.
     */

}
