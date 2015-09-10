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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BlackholePipelinePayloadBench {

    @Param("10")
    private int steps;

    private boolean[] booleans;
    private byte[] bytes;
    private short[] shorts;
    private char[] chars;
    private int[] ints;
    private float[] floats;
    private long[] longs;
    private double[] doubles;
    private Double[] objects;
    private Double[][] arrays;

    @Setup
    public void prepare() {
        booleans = new boolean[steps];
        bytes = new byte[steps];
        shorts = new short[steps];
        chars = new char[steps];
        ints = new int[steps];
        floats = new float[steps];
        longs = new long[steps];
        doubles = new double[steps];
        objects = new Double[steps];
        arrays = new Double[steps][];

        for (int c = 0; c < steps; c++) {
            booleans[c] = ((c & 1) == 0);
            bytes[c] = (byte) c;
            shorts[c] = (short) c;
            chars[c] = (char) c;
            ints[c] = c;
            floats[c] = c;
            longs[c] = c;
            doubles[c] = c;
            objects[c] = (double) c;
            arrays[c] = new Double[]{Double.valueOf(c)};
        }
    }

    @Benchmark
    public void test_boolean(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((Math.log(doubles[c]) > 1) ^ booleans[c]);
        }
    }

    @Benchmark
    public void test_byte(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((byte)Math.log(bytes[c]));
        }
    }

    @Benchmark
    public void test_short(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((short)Math.log(shorts[c]));
        }
    }

    @Benchmark
    public void test_char(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((char)Math.log(chars[c]));
        }
    }

    @Benchmark
    public void test_int(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((int)Math.log(ints[c]));
        }
    }

    @Benchmark
    public void test_float(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((float)Math.log(floats[c]));
        }
    }

    @Benchmark
    public void test_long(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((long)Math.log(longs[c]));
        }
    }

    @Benchmark
    public void test_double(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((double)Math.log(doubles[c]));
        }
    }

    @Benchmark
    public void test_Object(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume((Double) Math.log(objects[c]));
        }
    }

    @Benchmark
    public void test_Array(Blackhole bh) {
        for (int c = 0; c < steps; c++) {
            bh.consume(new Double[] {Math.log(arrays[c][0])});
        }
    }

}
