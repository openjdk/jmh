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
package org.openjdk.jmh.it.errors;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
public class ForkedErrorsTest {

    @Benchmark
    public void test00_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    @Benchmark
    public void test01_exceptional() {
        throw new IllegalStateException();
    }

    @Benchmark
    public void test02_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    @Benchmark
    public void test03_exit() throws InterruptedException {
        System.exit(1);
    }

    @Benchmark
    public void test04_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    @Benchmark
    public void test05_crash() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        // SIGSEGV in JVM
        getUnsafe().getInt(0);
    }

    @Benchmark
    public void test06_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    @Benchmark
    public void test07_runtimeExit() throws InterruptedException {
        Runtime.getRuntime().exit(1);
    }

    @Benchmark
    public void test08_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    @Benchmark
    public void test09_runtimeHalt() throws InterruptedException {
        Runtime.getRuntime().halt(1);
    }

    @Benchmark
    public void test10_normal() throws InterruptedException {
        Thread.sleep(1);
    }

    @Test
    public void test_FOE_false() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .forks(1)
                .shouldFailOnError(false)
                .build();
        Collection<RunResult> results = new Runner(opt).run();

        Assert.assertEquals(6, results.size());
    }

    @Test
    public void test_FOE_true() throws RunnerException {
        try {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .forks(1)
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();

            Assert.fail("Should have thrown the exception");
        } catch (RunnerException e) {
            // expected
        }
    }

}
