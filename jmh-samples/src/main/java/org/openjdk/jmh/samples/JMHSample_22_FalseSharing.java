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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
public class JMHSample_22_FalseSharing {

    /*
     * One of the unusual thing that can bite you back is false sharing.
     * If two threads access (and possibly modify) the adjacent values
     * in memory, chances are, they are modifying the values on the same
     * cache line. This can yield significant (artificial) slowdowns.
     *
     * JMH helps you to alleviate this: @States are automatically padded.
     * This padding does not extend to the State internals though,
     * as we will see in this example. You have to take care of this on
     * your own.
     */

    /*
     * Suppose we have two threads:
     *   a) innocuous reader which blindly reads its own field
     *   b) furious writer which updates its own field
     */

    /*
     * BASELINE EXPERIMENT:
     * Because of the false sharing, both reader and writer will experience
     * penalties.
     */

    @State(Scope.Group)
    public static class StateBaseline {
        int readOnly;
        int writeOnly;
    }

    @Benchmark
    @Group("baseline")
    public int reader(StateBaseline s) {
        return s.readOnly;
    }

    @Benchmark
    @Group("baseline")
    public void writer(StateBaseline s) {
        s.writeOnly++;
    }

    /*
     * APPROACH 1: PADDING
     *
     * We can try to alleviate some of the effects with padding.
     * This is not versatile because JVMs can freely rearrange the
     * field order, even of the same type.
     */

    @State(Scope.Group)
    public static class StatePadded {
        int readOnly;
        int p01, p02, p03, p04, p05, p06, p07, p08;
        int p11, p12, p13, p14, p15, p16, p17, p18;
        int writeOnly;
        int q01, q02, q03, q04, q05, q06, q07, q08;
        int q11, q12, q13, q14, q15, q16, q17, q18;
    }

    @Benchmark
    @Group("padded")
    public int reader(StatePadded s) {
        return s.readOnly;
    }

    @Benchmark
    @Group("padded")
    public void writer(StatePadded s) {
        s.writeOnly++;
    }

    /*
     * APPROACH 2: CLASS HIERARCHY TRICK
     *
     * We can alleviate false sharing with this convoluted hierarchy trick,
     * using the fact that superclass fields are usually laid out first.
     * In this construction, the protected field will be squashed between
     * paddings.

     * It is important to use the smallest data type, so that layouter would
     * not generate any gaps that can be taken by later protected subclasses
     * fields. Depending on the actual field layout of classes that bear the
     * protected fields, we might need more padding to account for "lost"
     * padding fields pulled into in their superclass gaps.
     */

    public static class StateHierarchy_1 {
        int readOnly;
    }

    public static class StateHierarchy_2 extends StateHierarchy_1 {
        byte p01, p02, p03, p04, p05, p06, p07, p08;
        byte p11, p12, p13, p14, p15, p16, p17, p18;
        byte p21, p22, p23, p24, p25, p26, p27, p28;
        byte p31, p32, p33, p34, p35, p36, p37, p38;
        byte p41, p42, p43, p44, p45, p46, p47, p48;
        byte p51, p52, p53, p54, p55, p56, p57, p58;
        byte p61, p62, p63, p64, p65, p66, p67, p68;
        byte p71, p72, p73, p74, p75, p76, p77, p78;
    }

    public static class StateHierarchy_3 extends StateHierarchy_2 {
        int writeOnly;
    }

    public static class StateHierarchy_4 extends StateHierarchy_3 {
        byte q01, q02, q03, q04, q05, q06, q07, q08;
        byte q11, q12, q13, q14, q15, q16, q17, q18;
        byte q21, q22, q23, q24, q25, q26, q27, q28;
        byte q31, q32, q33, q34, q35, q36, q37, q38;
        byte q41, q42, q43, q44, q45, q46, q47, q48;
        byte q51, q52, q53, q54, q55, q56, q57, q58;
        byte q61, q62, q63, q64, q65, q66, q67, q68;
        byte q71, q72, q73, q74, q75, q76, q77, q78;
    }

    @State(Scope.Group)
    public static class StateHierarchy extends StateHierarchy_4 {
    }

    @Benchmark
    @Group("hierarchy")
    public int reader(StateHierarchy s) {
        return s.readOnly;
    }

    @Benchmark
    @Group("hierarchy")
    public void writer(StateHierarchy s) {
        s.writeOnly++;
    }

    /*
     * APPROACH 3: ARRAY TRICK
     *
     * This trick relies on the contiguous allocation of an array.
     * Instead of placing the fields in the class, we mangle them
     * into the array at very sparse offsets.
     */

    @State(Scope.Group)
    public static class StateArray {
        int[] arr = new int[128];
    }

    @Benchmark
    @Group("sparse")
    public int reader(StateArray s) {
        return s.arr[0];
    }

    @Benchmark
    @Group("sparse")
    public void writer(StateArray s) {
        s.arr[64]++;
    }

    /*
     * APPROACH 4:
     *
     * @Contended (since JDK 8):
     *  Uncomment the annotation if building with JDK 8.
     *  Remember to flip -XX:-RestrictContended to enable.
     */

    @State(Scope.Group)
    public static class StateContended {
        int readOnly;

//        @sun.misc.Contended
        int writeOnly;
    }

    @Benchmark
    @Group("contended")
    public int reader(StateContended s) {
        return s.readOnly;
    }

    @Benchmark
    @Group("contended")
    public void writer(StateContended s) {
        s.writeOnly++;
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * Note the slowdowns.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_22 -t $CPU
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_22_FalseSharing.class.getSimpleName())
                .threads(Runtime.getRuntime().availableProcessors())
                .build();

        new Runner(opt).run();
    }

}
