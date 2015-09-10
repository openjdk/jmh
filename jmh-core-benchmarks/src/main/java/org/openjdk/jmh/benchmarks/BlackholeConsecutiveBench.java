/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class BlackholeConsecutiveBench {

    private int x = 4242;
    private int y = 1414;

    @Benchmark
    public void test_boolean_1(Blackhole bh) {
        bh.consume((x / y + 1331) > 0);
    }

    @Benchmark
    public void test_boolean_4(Blackhole bh) {
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
    }

    @Benchmark
    public void test_boolean_8(Blackhole bh) {
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);

        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
        bh.consume((x / y + 1331) > 0);
    }

    @Benchmark
    public void test_byte_1(Blackhole bh) {
        bh.consume((byte) (x / y + 1331));
    }

    @Benchmark
    public void test_byte_4(Blackhole bh) {
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
    }

    @Benchmark
    public void test_byte_8(Blackhole bh) {
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));

        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
        bh.consume((byte) (x / y + 1331));
    }

    @Benchmark
    public void test_short_1(Blackhole bh) {
        bh.consume((short) (x / y + 1331));
    }

    @Benchmark
    public void test_short_4(Blackhole bh) {
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
    }

    @Benchmark
    public void test_short_8(Blackhole bh) {
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));

        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
        bh.consume((short) (x / y + 1331));
    }

    @Benchmark
    public void test_char_1(Blackhole bh) {
        bh.consume((char) (x / y + 1331));
    }

    @Benchmark
    public void test_char_4(Blackhole bh) {
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
    }

    @Benchmark
    public void test_char_8(Blackhole bh) {
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));

        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
        bh.consume((char) (x / y + 1331));
    }

    @Benchmark
    public void test_int_1(Blackhole bh) {
        bh.consume((int) (x / y + 1331));
    }

    @Benchmark
    public void test_int_4(Blackhole bh) {
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
    }

    @Benchmark
    public void test_int_8(Blackhole bh) {
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));

        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
        bh.consume((int) (x / y + 1331));
    }

    @Benchmark
    public void test_float_1(Blackhole bh) {
        bh.consume((float) (x / y + 1331));
    }

    @Benchmark
    public void test_float_4(Blackhole bh) {
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
    }

    @Benchmark
    public void test_float_8(Blackhole bh) {
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));

        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
        bh.consume((float) (x / y + 1331));
    }

    @Benchmark
    public void test_long_1(Blackhole bh) {
        bh.consume((long) (x / y + 1331));
    }

    @Benchmark
    public void test_long_4(Blackhole bh) {
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
    }

    @Benchmark
    public void test_long_8(Blackhole bh) {
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));

        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
        bh.consume((long) (x / y + 1331));
    }

    @Benchmark
    public void test_double_1(Blackhole bh) {
        bh.consume((double) (x / y + 1331));
    }

    @Benchmark
    public void test_double_4(Blackhole bh) {
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
    }

    @Benchmark
    public void test_double_8(Blackhole bh) {
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));

        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
        bh.consume((double) (x / y + 1331));
    }

    @Benchmark
    public void test_Object_1(Blackhole bh) {
        bh.consume(Double.valueOf(x / y + 1331));
    }

    @Benchmark
    public void test_Object_4(Blackhole bh) {
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
    }

    @Benchmark
    public void test_Object_8(Blackhole bh) {
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));

        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
        bh.consume(Double.valueOf(x / y + 1331));
    }

    @Benchmark
    public void test_Array_1(Blackhole bh) {
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
    }

    @Benchmark
    public void test_Array_4(Blackhole bh) {
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
    }

    @Benchmark
    public void test_Array_8(Blackhole bh) {
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});

        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
        bh.consume(new Double[]{Double.valueOf(x / y + 1331)});
    }

}
