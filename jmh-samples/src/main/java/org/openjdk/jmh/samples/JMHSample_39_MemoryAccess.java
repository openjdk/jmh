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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class JMHSample_39_MemoryAccess {
    public static final int N = 20_000_000;

    /*
     * This example highlights the pitfall of accidentally measuring memory access instead of processing time.
     *
     * An int array has got a different memory layout than an ArrayList of boxed ints.
     * This can lead to useless results because the memory access is completely different.
     * Arrays save all their ints in one block on the heap while ArrayLists don't.
     * They save only references to the boxed ints in one block.
     * All the references point to the boxed ints which are usually spread all over the heap.
     * This leads to many cache misses with a big error:
     *
     * Benchmark                                       Mode  Cnt    Score   Error  Units
     * JMHSample_39_MemoryAccess.sumArray              avgt   25    4.887 ± 0.008  ms/op
     * JMHSample_39_MemoryAccess.sumArrayList          avgt   25   35.765 ± 0.112  ms/op
     * JMHSample_39_MemoryAccess.sumArrayListShuffled  avgt   25  169.301 ± 1.064  ms/op
     *
     * The Java Object Layout (JOL) is a tool with which the different memory layouts of arrays and ArrayLists can be
     * examined in more detail.
     */

    private int[] intArray = new int[N];
    private List<Integer> intList = new ArrayList<>(N);
    private List<Integer> shuffledIntList = new ArrayList<>(N);

    @Setup
    public void setup() {
        Random random = new Random(1234);
        for (int i = 0; i < N; i++) {
            intArray[i] = random.nextInt();
            intList.add(intArray[i]);
            shuffledIntList.add(intArray[i]);
        }
        Collections.shuffle(shuffledIntList);
    }

    @Benchmark
    public long sumArray() {
        long sum = 0;
        for (int i = 0; i < N; i++) {
            sum += intArray[i];
        }
        return sum;
    }

    @Benchmark
    public long sumArrayList() {
        long sum = 0;
        for (int i = 0; i < N; i++) {
            sum += intList.get(i);
        }
        return sum;
    }

    @Benchmark
    public long sumArrayListShuffled() {
        long sum = 0;
        for (int i = 0; i < N; i++) {
            sum += shuffledIntList.get(i);
        }
        return sum;
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_39
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_39_MemoryAccess.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }
}
