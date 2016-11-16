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
package org.openjdk.jmh.it.threads;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests if harness executes setup, run, and tearDown in the same workers.
 */
@BenchmarkMode(Mode.All)
@Warmup(iterations = 0)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Group)
public class GroupThreadGroupOrderTest {

    private final Set<Thread> abc = Collections.synchronizedSet(new HashSet<Thread>());
    private final Set<Thread> def = Collections.synchronizedSet(new HashSet<Thread>());
    private final Set<Thread> ghi = Collections.synchronizedSet(new HashSet<Thread>());

    @Setup(Level.Iteration)
    public void prepare() {
        abc.clear();
        def.clear();
        ghi.clear();
    }

    @TearDown(Level.Iteration)
    public void verify() {
        Assert.assertEquals("Test abc", 3, abc.size());
        Assert.assertEquals("Test def", 1, def.size());
        Assert.assertEquals("Test ghi", 2, ghi.size());
    }

    @Benchmark
    @Group("T")
    public void abc() {
        abc.add(Thread.currentThread());
        Fixtures.work();
    }

    @Benchmark
    @Group("T")
    public void ghi() {
        ghi.add(Thread.currentThread());
        Fixtures.work();
    }

    @Benchmark
    @Group("T")
    public void def() {
        def.add(Thread.currentThread());
        Fixtures.work();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .threadGroups(3, 1, 2)
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

}
