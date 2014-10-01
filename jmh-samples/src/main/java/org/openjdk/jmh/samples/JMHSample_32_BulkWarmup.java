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
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.WarmupMode;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JMHSample_32_BulkWarmup {

    /*
     * This is an addendum to JMHSample_12_Forking test.
     *
     * Sometimes you want an opposite configuration: instead of separating the profiles
     * for different benchmarks, you want to mix them together to test the worst-case
     * scenario.
     *
     * JMH has a bulk warmup feature for that: it does the warmups for all the tests
     * first, and then measures them. JMH still forks the JVM for each test, but once the
     * new JVM has started, all the warmups are being run there, before running the
     * measurement. This helps to dodge the type profile skews, as each test is still
     * executed in a different JVM, and we only "mix" the warmup code we want.
     */

    /*
     * These test classes are borrowed verbatim from JMHSample_12_Forking.
     */

    public interface Counter {
        int inc();
    }

    public class Counter1 implements Counter {
        private int x;

        @Override
        public int inc() {
            return x++;
        }
    }

    public class Counter2 implements Counter {
        private int x;

        @Override
        public int inc() {
            return x++;
        }
    }

    Counter c1 = new Counter1();
    Counter c2 = new Counter2();

    /*
     * And this is our test payload. Notice we have to break the inlining of the payload,
     * so that in could not be inlined in either measure_c1() or measure_c2() below, and
     * specialized for that only call.
     */

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int measure(Counter c) {
        int s = 0;
        for (int i = 0; i < 10; i++) {
            s += c.inc();
        }
        return s;
    }

    @Benchmark
    public int measure_c1() {
        return measure(c1);
    }

    @Benchmark
    public int measure_c2() {
        return measure(c2);
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * Note how JMH runs the warmups first, and only then a given test. Note how JMH re-warmups
     * the JVM for each test. The scores for C1 and C2 cases are equally bad, compare them to
     * the scores from JMHSample_12_Forking.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar JMHSample_32 -f 1 -wi 5 -i 5 -bm BULK
     *    (we requested a single fork, 5 warmup/measurement iterations, and bulk warmup mode)
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_32_BulkWarmup.class.getSimpleName())
                // .includeWarmup(...) <-- this may include other benchmarks into warmup
                .warmupMode(WarmupMode.BULK) // see other WarmupMode.* as well
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
