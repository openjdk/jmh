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
package org.openjdk.jmh.benchmarks;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BlackholePipelineBench {

    private Integer[] data;

    @Setup
    public void prepare() {
        data = new Integer[1000];
        for (int c = 0; c < 1000; c++) {
            data[c] = new Integer(c);
        }
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1)
    public void test_Obj_1(BlackHole bh) {
        doTestObj(bh, 1);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(10)
    public void test_Obj_10(BlackHole bh) {
        doTestObj(bh, 10);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(100)
    public void test_Obj_100(BlackHole bh) {
        doTestObj(bh, 100);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1000)
    public void test_Obj_1000(BlackHole bh) {
        doTestObj(bh, 1000);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1)
    public void test_Int_1(BlackHole bh) {
        doTestInt(bh, 1);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(10)
    public void test_Int_10(BlackHole bh) {
        doTestInt(bh, 10);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(100)
    public void test_Int_100(BlackHole bh) {
        doTestInt(bh, 100);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1000)
    public void test_Int_1000(BlackHole bh) {
        doTestInt(bh, 1000);
    }

    public void doTestObj(BlackHole bh, int count) {
        for (int c = 0; c < count; c++) {
            bh.consume(data[c]);
        }
    }

    public void doTestInt(BlackHole bh, int count) {
        for (int c = 0; c < count; c++) {
            bh.consume(data[c].intValue());
        }
    }

}
