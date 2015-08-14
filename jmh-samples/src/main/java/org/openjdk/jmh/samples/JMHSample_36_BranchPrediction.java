/*
 * Copyright (c) 2015, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmh.samples;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class JMHSample_36_BranchPrediction {

    /*
     * This sample serves as a warning against regular data sets.
     *
     * It is very tempting to present a regular data set to benchmark, either due to
     * naive generation strategy, or just from feeling better about regular data sets.
     * Unfortunately, it frequently backfires: the regular datasets are known to be
     * optimized well by software and hardware. This example exploits one of these
     * optimizations: branch prediction.
     *
     * Imagine our benchmark selects the branch based on the array contents, as
     * we are streaming through it:
     */

    private static final int COUNT = 1024 * 1024;

    private byte[] sorted;
    private byte[] unsorted;

    @Setup
    public void setup() {
        sorted = new byte[COUNT];
        unsorted = new byte[COUNT];
        Random random = new Random(1234);
        random.nextBytes(sorted);
        random.nextBytes(unsorted);
        Arrays.sort(sorted);
    }

    @Benchmark
    @OperationsPerInvocation(COUNT)
    public void sorted(Blackhole bh1, Blackhole bh2) {
        for (byte v : sorted) {
            if (v > 0) {
                bh1.consume(v);
            } else {
                bh2.consume(v);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(COUNT)
    public void unsorted(Blackhole bh1, Blackhole bh2) {
        for (byte v : unsorted) {
            if (v > 0) {
                bh1.consume(v);
            } else {
                bh2.consume(v);
            }
        }
    }

    /*
        There is a substantial difference in performance for these benchmarks!

        It is explained by good branch prediction in "sorted" case, and branch mispredicts in "unsorted"
        case. -prof perfnorm conveniently highlights that, with larger "branch-misses", and larger "CPI"
        for "unsorted" case:

        Benchmark                                                       Mode  Cnt   Score    Error  Units
        JMHSample_36_BranchPrediction.sorted                            avgt   25   2.160 ±  0.049  ns/op
        JMHSample_36_BranchPrediction.sorted:·CPI                       avgt    5   0.286 ±  0.025   #/op
        JMHSample_36_BranchPrediction.sorted:·branch-misses             avgt    5  ≈ 10⁻⁴            #/op
        JMHSample_36_BranchPrediction.sorted:·branches                  avgt    5   7.606 ±  1.742   #/op
        JMHSample_36_BranchPrediction.sorted:·cycles                    avgt    5   8.998 ±  1.081   #/op
        JMHSample_36_BranchPrediction.sorted:·instructions              avgt    5  31.442 ±  4.899   #/op

        JMHSample_36_BranchPrediction.unsorted                          avgt   25   5.943 ±  0.018  ns/op
        JMHSample_36_BranchPrediction.unsorted:·CPI                     avgt    5   0.775 ±  0.052   #/op
        JMHSample_36_BranchPrediction.unsorted:·branch-misses           avgt    5   0.529 ±  0.026   #/op  <--- OOPS
        JMHSample_36_BranchPrediction.unsorted:·branches                avgt    5   7.841 ±  0.046   #/op
        JMHSample_36_BranchPrediction.unsorted:·cycles                  avgt    5  24.793 ±  0.434   #/op
        JMHSample_36_BranchPrediction.unsorted:·instructions            avgt    5  31.994 ±  2.342   #/op

        It is an open question if you want to measure only one of these tests. In many cases, you have to measure
        both to get the proper best-case and worst-case estimate!
     */


    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_36
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_36_BranchPrediction.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

}
