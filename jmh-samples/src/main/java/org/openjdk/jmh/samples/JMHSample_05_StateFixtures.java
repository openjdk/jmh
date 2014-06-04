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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
public class JMHSample_05_StateFixtures {

    double x;

    /*
     * Since @State objects are kept around during the lifetime of the benchmark,
     * it helps to have the methods which do state housekeeping. These are usual
     * fixture methods, you are probably familiar with them from JUnit and TestNG.
     *
     * Fixture methods make sense only on @State objects, and JMH will fail to compile
     * the test otherwise.
     *
     * As with the State, fixture methods are only called by those benchmark threads
     * which are using the state. That means, you can operate the thread-local contexts,
     * (don't) use synchronization as if you are executing in the context of benchmark
     * thread.
     *
     * Note: fixture methods can also work with static fields, although the semantics
     * of these operations fall back out of State scope, and obey usual Java rules (i.e.
     * one static field per class).
     */

    /*
     * Ok, let's prepare our benchmark:
     */

    @Setup
    public void prepare() {
        x = Math.PI;
    }

    /*
     * And, check the benchmark went fine afterwards:
     */

    @TearDown
    public void check() {
        assert x > Math.PI : "Nothing changed?";
    }

    /*
     * This method obviously does the right thing, incrementing the field x
     * in the benchmark state. check() will never fail this way, because
     * we are always guaranteed to have at least one benchmark call.
     */

    @Benchmark
    public void measureRight() {
        x++;
    }

    /*
     * This method, however, will fail the check(), because we deliberately
     * have the "typo", and increment only the local variable. This should
     * not pass the check, and JMH will fail the run.
     */

    @Benchmark
    public void measureWrong() {
        double x = 0;
        x++;
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can see measureRight() yields the result, and measureWrong() fires
     * the assert at the end of the run.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -ea -jar target/microbenchmarks.jar ".*JMHSample_05.*" -wi 5 -i 5 -f 1
     *    (we requested 5 warmup/measurement iterations, single fork)
     *
     * b) Via the Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_05_StateFixtures.class.getSimpleName() + ".*")
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .jvmArgs("-ea")
                .build();

        new Runner(opt).run();
    }

}
