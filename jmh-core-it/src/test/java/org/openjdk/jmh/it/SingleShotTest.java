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
package org.openjdk.jmh.it;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests if harness had indeed executed different tests in different JVMs.
 */
@BenchmarkMode(Mode.SingleShotTime)
public class SingleShotTest implements RunnerFactory {

    private static final AtomicInteger test1executed = new AtomicInteger();
    private static final AtomicInteger test2executed = new AtomicInteger();

    @Benchmark
    @Fork(1)
    public void test1() {
        Fixtures.work();
        Assert.assertEquals(1, test1executed.incrementAndGet());
        Assert.assertEquals(0, test2executed.get());
    }

    @Benchmark
    @Fork(1)
    public void test2() {
        Fixtures.work();
        Assert.assertEquals(1, test2executed.incrementAndGet());
        Assert.assertEquals(0, test1executed.get());
    }

    @Test
    public void invokeAPI() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .forks(1)
                .build();
        createRunner(opt).run();
    }

    @Test
    public void invokeAPI_1() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .forks(2)
                .build();
        createRunner(opt).run();
    }

}
