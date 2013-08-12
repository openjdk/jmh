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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JMHSample_10_ConstantFold {

    /*
     * The flip side of dead-code elimination is constant-folding.
     *
     * If JVM realizes the result of the computation is the same no matter what, it
     * can cleverly optimize it. In our case, that means we can move the computation
     * outside of the internal JMH loop.
     *
     * This can be prevented by always reading the inputs from the state, computing
     * the result based on that state, and the follow the rules to prevent DCE.
     */

    // IDEs will say "Oh, you can convert this field to local variable". Don't. Trust. Them.
    private double x = Math.PI;

    @GenerateMicroBenchmark
    public void baseline() {
        // do nothing, this is a baseline
    }

    @GenerateMicroBenchmark
    public double measureWrong() {
        // This is wrong: the result is provably the same, optimized out.
        return Math.log(Math.PI);
    }

    @GenerateMicroBenchmark
    public double measureRight() {
        // This is correct: the result is being used.
        return Math.log(x);
    }

    /*
     * HOW TO RUN THIS TEST:
     *
     * You can run this test with:
     *    $ mvn clean install
     *    $ java -jar target/microbenchmarks.jar ".*JMHSample_10.*" -i 5 -r 1s
     *    (we requested 5 iterations, 1 sec each)
     *
     * You can see the unrealistically fast calculation in with measureWrong(),
     * while realistic measurement with measureRight().
     */

}
