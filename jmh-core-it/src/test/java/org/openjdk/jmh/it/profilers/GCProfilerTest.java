/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.profilers;

import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Tests allocation profiler.
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1) // 0 to enable debugging
public class GCProfilerTest {

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 20, time = 10, timeUnit = TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public Object allocateObjectNoWarmup() {
        return new Object();
    }

    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
    @BenchmarkMode(Mode.AverageTime)
    public Object allocateObject() {
        return new Object();
    }

    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 5)
    @BenchmarkMode(Mode.SingleShotTime)
    public Object allocateObjectSingleShot() {
        return new Object();
    }

    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
    @BenchmarkMode(Mode.SampleTime)
    public Object allocateObjectSampleTime() {
        return new Object();
    }

    @Test
    public void testAllocationProfiler() throws RunnerException {
        try {
            new GCProfiler();
        } catch (ProfilerException e) {
            // not supported
            return;
        }
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opts).run();
    }
}
