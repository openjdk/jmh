/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.ct.blackhole;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.ct.CompileTest;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
@BenchmarkMode(Mode.All)
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

    @Benchmark
    public void baseline() {
        // do nothing
    }

    @Benchmark
    public byte implicit_testByte() {
        return b;
    }

    @Benchmark
    public boolean implicit_testBoolean() {
        return bool;
    }

    @Benchmark
    public char implicit_testChar() {
        return c;
    }

    @Benchmark
    public int implicit_testInt() {
        return i;
    }

    @Benchmark
    public long implicit_testLong() {
        return l;
    }

    @Benchmark
    public float implicit_testFloat() {
        return f;
    }

    @Benchmark
    public double implicit_testDouble() {
        return d;
    }

    @Benchmark
    public Object implicit_testObject() {
        return o;
    }

    @Benchmark
    public Object[] implicit_testArray() {
        return os;
    }

    @Benchmark
    public void explicit_testByte(Blackhole bh) {
        bh.consume(b);
    }

    @Benchmark
    public void explicit_testBoolean(Blackhole bh) {
        bh.consume(bool);
    }

    @Benchmark
    public void explicit_testChar(Blackhole bh) {
        bh.consume(c);
    }

    @Benchmark
    public void explicit_testInt(Blackhole bh) {
        bh.consume(i);
    }

    @Benchmark
    public void explicit_testLong(Blackhole bh) {
        bh.consume(l);
    }

    @Benchmark
    public void explicit_testFloat(Blackhole bh) {
        bh.consume(f);
    }

    @Benchmark
    public void explicit_testDouble(Blackhole bh) {
        bh.consume(d);
    }

    @Benchmark
    public void explicit_testObject(Blackhole bh) {
        bh.consume(o);
    }

    @Benchmark
    public void explicit_testArray(Blackhole bh) {
        bh.consume(os);
    }

    @Test
    public void test() {
        CompileTest.assertOK(this.getClass());
    }

}
