/**
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
package org.openjdk.jmh.it.compile.blackhole;

import org.openjdk.jmh.annotations.BenchmarkType;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

@State(Scope.Thread)
public class BlackholeTypesTest {

    public byte b;
    public boolean bool;
    public char c;
    public short s;
    public int i;
    public long l;
    public float f;
    public double d;
    public Object o;
    public Object[] os;

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void baseline() {
        // do nothing
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public byte implicit_testByte() {
        return b;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public boolean implicit_testBoolean() {
        return bool;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public char implicit_testChar() {
        return c;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public int implicit_testInt() {
        return i;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public long implicit_testLong() {
        return l;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public float implicit_testFloat() {
        return f;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public double implicit_testDouble() {
        return d;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public Object implicit_testObject() {
        return o;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public Object[] implicit_testArray() {
        return os;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testByte(BlackHole bh) {
        bh.consume(b);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testBoolean(BlackHole bh) {
        bh.consume(bool);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testChar(BlackHole bh) {
        bh.consume(c);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testInt(BlackHole bh) {
        bh.consume(i);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testLong(BlackHole bh) {
        bh.consume(l);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testFloat(BlackHole bh) {
        bh.consume(f);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testDouble(BlackHole bh) {
        bh.consume(d);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testObject(BlackHole bh) {
        bh.consume(o);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void explicit_testArray(BlackHole bh) {
        bh.consume(os);
    }

}
