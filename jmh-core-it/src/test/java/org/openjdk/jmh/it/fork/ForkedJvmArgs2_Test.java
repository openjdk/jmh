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
package org.openjdk.jmh.it.fork;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.All)
@Fork(jvmArgs = "-DtestUpper")
public class ForkedJvmArgs2_Test {

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgs = "-Dtest1")
    public void test1() {
        Fixtures.work();
        Assert.assertNotNull(System.getProperty("test1"));
        Assert.assertNull(System.getProperty("test2"));
        Assert.assertNull(System.getProperty("testUpper"));
    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgs = "-Dtest2")
    public void test2() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("test1"));
        Assert.assertNotNull(System.getProperty("test2"));
        Assert.assertNull(System.getProperty("testUpper"));
    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork
    public void testUpper() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("test1"));
        Assert.assertNull(System.getProperty("test2"));
        Assert.assertNotNull(System.getProperty("testUpper"));
    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    public void testNone() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("test1"));
        Assert.assertNull(System.getProperty("test2"));
        Assert.assertNotNull(System.getProperty("testUpper"));
    }

    @Test
    public void invokeAPI() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .failOnError(true)
                .build();
        new Runner(opt).run();
    }

}
