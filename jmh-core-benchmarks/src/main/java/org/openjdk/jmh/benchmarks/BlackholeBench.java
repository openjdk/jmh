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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class BlackholeBench {

    private List<String> strs;

    @Setup(Level.Iteration)
    public void makeGarbage() {
        // make some garbage to evict blackhole from the TLAB/eden
        strs = new ArrayList<String>();
        for (int i = 0; i < 100000; i++) {
            strs.add("str" + i);
        }
    }

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

    public boolean trueBoolean = true;
    public boolean falseBoolean = false;

    @GenerateMicroBenchmark
    public void baseline() {
        // do nothing
    }

    @GenerateMicroBenchmark
    public byte implicit_testByte() {
        return b;
    }

    @GenerateMicroBenchmark
    public boolean implicit_testBoolean() {
        return bool;
    }

    @GenerateMicroBenchmark
    public boolean implicit_testBoolean_true() {
        return true;
    }

    @GenerateMicroBenchmark
    public boolean implicit_testBoolean_false() {
        return false;
    }

    @GenerateMicroBenchmark
    public boolean implicit_testBoolean_trueF() {
        return trueBoolean;
    }

    @GenerateMicroBenchmark
    public boolean implicit_testBoolean_falseF() {
        return falseBoolean;
    }

    @GenerateMicroBenchmark
    public char implicit_testChar() {
        return c;
    }

    @GenerateMicroBenchmark
    public short implicit_testShort() {
        return s;
    }

    @GenerateMicroBenchmark
    public int implicit_testInt() {
        return i;
    }

    @GenerateMicroBenchmark
    public long implicit_testLong() {
        return l;
    }

    @GenerateMicroBenchmark
    public float implicit_testFloat() {
        return f;
    }

    @GenerateMicroBenchmark
    public double implicit_testDouble() {
        return d;
    }

    @GenerateMicroBenchmark
    public Object implicit_testObject() {
        return o;
    }

    @GenerateMicroBenchmark
    public Object[] implicit_testArray() {
        return os;
    }

    @GenerateMicroBenchmark
    public void explicit_testByte(BlackHole bh) {
        bh.consume(b);
    }

    @GenerateMicroBenchmark
    public void explicit_testBoolean(BlackHole bh) {
        bh.consume(bool);
    }

    @GenerateMicroBenchmark
    public void explicit_testChar(BlackHole bh) {
        bh.consume(c);
    }

    @GenerateMicroBenchmark
    public void explicit_testShort(BlackHole bh) {
        bh.consume(s);
    }

    @GenerateMicroBenchmark
    public void explicit_testInt(BlackHole bh) {
        bh.consume(i);
    }

    @GenerateMicroBenchmark
    public void explicit_testLong(BlackHole bh) {
        bh.consume(l);
    }

    @GenerateMicroBenchmark
    public void explicit_testFloat(BlackHole bh) {
        bh.consume(f);
    }

    @GenerateMicroBenchmark
    public void explicit_testDouble(BlackHole bh) {
        bh.consume(d);
    }

    @GenerateMicroBenchmark
    public void explicit_testObject(BlackHole bh) {
        bh.consume(o);
    }

    @GenerateMicroBenchmark
    public void explicit_testArray(BlackHole bh) {
        bh.consume(os);
    }

}
