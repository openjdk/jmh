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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class BlackholeConsumeCPUBench {

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1)
    public void consume_00000() {
        Blackhole.consumeCPU(0);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1)
    public void consume_00001() {
        Blackhole.consumeCPU(1);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(2)
    public void consume_00002() {
        Blackhole.consumeCPU(2);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(4)
    public void consume_00004() {
        Blackhole.consumeCPU(4);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(8)
    public void consume_00008() {
        Blackhole.consumeCPU(8);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(16)
    public void consume_00016() {
        Blackhole.consumeCPU(16);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(32)
    public void consume_00032() {
        Blackhole.consumeCPU(32);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(64)
    public void consume_00064() {
        Blackhole.consumeCPU(64);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(128)
    public void consume_00128() {
        Blackhole.consumeCPU(128);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(256)
    public void consume_00256() {
        Blackhole.consumeCPU(256);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(512)
    public void consume_00512() {
        Blackhole.consumeCPU(512);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(1024)
    public void consume_01024() {
        Blackhole.consumeCPU(1024);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(2048)
    public void consume_02048() {
        Blackhole.consumeCPU(2048);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(4096)
    public void consume_04096() {
        Blackhole.consumeCPU(4096);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(8192)
    public void consume_08192() {
        Blackhole.consumeCPU(8192);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(16384)
    public void consume_16384() {
        Blackhole.consumeCPU(16384);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(32768)
    public void consume_32768() {
        Blackhole.consumeCPU(32768);
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(65536)
    public void consume_65536() {
        Blackhole.consumeCPU(65536);
    }
}
