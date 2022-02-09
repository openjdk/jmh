/*
 * Copyright (c) 2014, Oracle America, Inc.
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

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class JMHSample_09_Blackholes {

    /*
     * Should your benchmark require returning multiple results, you have to
     * consider two options (detailed below).
     *
     * NOTE: If you are only producing a single result, it is more readable to
     * use the implicit return, as in JMHSample_08_DeadCode. Do not make your benchmark
     * code less readable with explicit Blackholes!
     */

    double x1 = Math.PI;
    double x2 = Math.PI * 2;

    private double compute(double d) {
        for (int c = 0; c < 10; c++) {
            d = d * d / Math.PI;
        }
        return d;
    }

    /*
     * Baseline measurement: how much a single compute() costs.
     */

    @Benchmark
    public double baseline() {
        return compute(x1);
    }

    /*
     * While the compute(x2) computation is intact, compute(x1)
     * is redundant and optimized out.
     */

    @Benchmark
    public double measureWrong() {
        compute(x1);
        return compute(x2);
    }

    /*
     * This demonstrates Option A:
     *
     * Merge multiple results into one and return it.
     * This is OK when is computation is relatively heavyweight, and merging
     * the results does not offset the results much.
     */

    @Benchmark
    public double measureRight_1() {
        return compute(x1) + compute(x2);
    }

    /*
     * This demonstrates Option B:
     *
     * Use explicit Blackhole objects, and sink the values there.
     * (Background: Blackhole is just another @State object, bundled with JMH).
     */

    @Benchmark
    public void measureRight_2(Blackhole bh) {
        bh.consume(compute(x1));
        bh.consume(compute(x2));
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You will see measureWrong() running on-par with baseline().
     * Both measureRight() are measuring twice the baseline, so the logs are intact.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_09 -f 1
     *    (we requested single fork; there are also other options, see -h)
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_09_Blackholes.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
