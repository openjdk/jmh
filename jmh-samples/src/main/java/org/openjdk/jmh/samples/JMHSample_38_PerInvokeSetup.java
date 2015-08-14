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
public class JMHSample_38_PerInvokeSetup {

    /*
     * This example highlights the usual mistake in non-steady-state benchmarks.
     *
     * Suppose we want to test how long it takes to bubble sort an array. Naively,
     * we could make the test that populates an array with random (unsorted) values,
     * and calls sort on it over and over again:
     */

    private void bubbleSort(byte[] b) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int c = 0; c < b.length - 1; c++) {
                if (b[c] > b[c + 1]) {
                    byte t = b[c];
                    b[c] = b[c + 1];
                    b[c + 1] = t;
                    changed = true;
                }
            }
        }
    }

    // Could be an implicit State instead, but we are going to use it
    // as the dependency in one of the tests below
    @State(Scope.Benchmark)
    public static class Data {

        @Param({"1", "16", "256"})
        int count;

        byte[] arr;

        @Setup
        public void setup() {
            arr = new byte[count];
            Random random = new Random(1234);
            random.nextBytes(arr);
        }
    }

    @Benchmark
    public byte[] measureWrong(Data d) {
        bubbleSort(d.arr);
        return d.arr;
    }

    /*
     * The method above is subtly wrong: it sorts the random array on the first invocation
     * only. Every subsequent call will "sort" the already sorted array. With bubble sort,
     * that operation would be significantly faster!
     *
     * This is how we might *try* to measure it right by making a copy in Level.Invocation
     * setup. However, this is susceptible to the problems described in Level.Invocation
     * Javadocs, READ AND UNDERSTAND THOSE DOCS BEFORE USING THIS APPROACH.
     */

    @State(Scope.Thread)
    public static class DataCopy {
        byte[] copy;

        @Setup(Level.Invocation)
        public void setup2(Data d) {
           copy = Arrays.copyOf(d.arr, d.arr.length);
        }
    }

    @Benchmark
    public byte[] measureNeutral(DataCopy d) {
        bubbleSort(d.copy);
        return d.copy;
    }

    /*
     * In an overwhelming majority of cases, the only sensible thing to do is to suck up
     * the per-invocation setup costs into a benchmark itself. This work well in practice,
     * especially when the payload costs dominate the setup costs.
     */

    @Benchmark
    public byte[] measureRight(Data d) {
        byte[] c = Arrays.copyOf(d.arr, d.arr.length);
        bubbleSort(c);
        return c;
    }

    /*
        Benchmark                                   (count)  Mode  Cnt      Score     Error  Units

        JMHSample_38_PerInvokeSetup.measureWrong          1  avgt   25      2.408 ±   0.011  ns/op
        JMHSample_38_PerInvokeSetup.measureWrong         16  avgt   25      8.286 ±   0.023  ns/op
        JMHSample_38_PerInvokeSetup.measureWrong        256  avgt   25     73.405 ±   0.018  ns/op

        JMHSample_38_PerInvokeSetup.measureNeutral        1  avgt   25     15.835 ±   0.470  ns/op
        JMHSample_38_PerInvokeSetup.measureNeutral       16  avgt   25    112.552 ±   0.787  ns/op
        JMHSample_38_PerInvokeSetup.measureNeutral      256  avgt   25  58343.848 ± 991.202  ns/op

        JMHSample_38_PerInvokeSetup.measureRight          1  avgt   25      6.075 ±   0.018  ns/op
        JMHSample_38_PerInvokeSetup.measureRight         16  avgt   25    102.390 ±   0.676  ns/op
        JMHSample_38_PerInvokeSetup.measureRight        256  avgt   25  58812.411 ± 997.951  ns/op

        We can clearly see that "measureWrong" provides a very weird result: it "sorts" way too fast.
        "measureNeutral" is neither good or bad: while it prepares the data for each invocation correctly,
        the timing overheads are clearly visible. These overheads can be overwhelming, depending on
        the thread count and/or OS flavor.
     */

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_38
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_38_PerInvokeSetup.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

}
