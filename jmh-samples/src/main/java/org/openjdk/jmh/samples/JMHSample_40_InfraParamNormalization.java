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
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
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
public class JMHSample_40_InfraParamNormalization {

    /*
     * This sample serves a dual purposes:
     *
     * First, it shows how to adjust opsPerSample from within the benchmarked project.
     * A correct opsPerSample normalizes all performance numbers and makes interpretation
     * of the benchmark results much simpler. In many cases this cannot be reasonably set
     * via annotations (not constant / known at compile time) nor by the java API (Runner
     * does not allow to set that on per-benchmark basis).
     *
     * Second, it serves as a warning against using shortish branching patterns that are
     * constant across invocations. As this benchmark demonstrates, modern CPU can memorize
     * surprisingly long branching patterns from one invocation to the next.
     *
     * This works via the branch history table. If our branch is taken half of the time,
     * and the branching pattern repeats every size 1000 times (i.e. it is the same for
     * each invocation and each invocation hits the branch 1000 times), then it is enough
     * to know the last 10 branch outcomes to identify our position in the sequence and
     * therefore almost perfectly predict the next branch outcome. This is the same way
     * that e.g. genomic sequences are reconstructed from short reads.
     *
     * Cf e.g. https://discourse.julialang.org/t/psa-microbenchmarks-remember-branch-history/17436
     */

    byte[] bytes;

    @Param({"100", "1000", "5000", "10000", "100000"})
    int size;

    @Setup
    public void setup(BenchmarkParams params) {
        bytes = new byte[size];
        Random random = new Random(1234);
        random.nextBytes(bytes);
        try {
            java.lang.reflect.Field field = BenchmarkParams.class.getSuperclass() // L4
                    .getSuperclass() // L3
                    .getSuperclass() // L2
                    .getDeclaredField("opsPerInvocation");
            field.setAccessible(true);
            field.setInt(params, size);
        } catch (Exception exc) { throw new RuntimeException("Could not set opsPerInvocation", exc);}
    }

    @Benchmark
    public void memorizePattern(Blackhole bh1, Blackhole bh2) {
        for (byte v : bytes) {
            if (v > 0) {
                bh1.consume(v);
            } else {
                bh2.consume(v);
            }
        }
    }


    /*
        There is a substantial difference in performance for these benchmarks!

        We see that the i9-9900K has a 20 cycle branch-miss penalty, can almost perfectly
        memorize patterns of length 5k, and cannot memorize patterns of length 100k.

        Benchmark                                                                     (size)  Mode  Cnt   Score    Error  Units
        JMHSample_40_InfraParamNormalization.memorizePattern                             100  avgt   15   0.279 ±  0.001  ns/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branch-misses:u             100  avgt    3  ≈ 10⁻⁴            #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branches:u                  100  avgt    3   1.476 ±  0.065   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:cycles:u                    100  avgt    3   1.328 ±  0.080   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern                            1000  avgt   15   0.249 ±  0.001  ns/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branch-misses:u            1000  avgt    3  ≈ 10⁻³            #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branches:u                 1000  avgt    3   1.371 ±  0.126   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:cycles:u                   1000  avgt    3   1.179 ±  0.034   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern                            5000  avgt   15   0.297 ±  0.028  ns/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branch-misses:u            5000  avgt    3   0.011 ±  0.140   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branches:u                 5000  avgt    3   1.370 ±  0.024   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:cycles:u                   5000  avgt    3   1.407 ±  2.656   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern                           10000  avgt   15   1.147 ±  0.127  ns/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branch-misses:u           10000  avgt    3   0.180 ±  0.587   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branches:u                10000  avgt    3   1.370 ±  0.073   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:cycles:u                  10000  avgt    3   5.441 ± 11.936   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern                          100000  avgt   15   2.589 ±  0.011  ns/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branch-misses:u          100000  avgt    3   0.491 ±  0.021   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:branches:u               100000  avgt    3   1.371 ±  0.024   #/op
        JMHSample_40_InfraParamNormalization.memorizePattern:cycles:u                 100000  avgt    3  12.239 ±  0.450   #/op
     */


    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar ./target/benchmarks.jar JMHSample_40_InfraParamNormalization -prof perfnorm -f 3
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_40_InfraParamNormalization.class.getSimpleName() + ".*")
                .addProfiler(LinuxPerfNormProfiler.class)
                .build();

        new Runner(opt).run();
    }

}
