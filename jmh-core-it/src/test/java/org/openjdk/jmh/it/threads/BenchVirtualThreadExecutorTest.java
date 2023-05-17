/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests if harness executes setup, run, and tearDown in the virtual or platform threads.
 */
@State(Scope.Benchmark)
public class BenchVirtualThreadExecutorTest {

    @Param("false")
    boolean isVirtual;

    private final Set<Boolean> setupRunThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> setupIterationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> setupInvocationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> teardownRunThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> teardownIterationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> teardownInvocationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Boolean> testInvocationThread = Collections.synchronizedSet(new HashSet<>());

    @Setup(Level.Trial)
    public void setupRun() {
        setupRunThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        setupIterationThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        setupInvocationThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @TearDown(Level.Trial)
    public void tearDownRun() {
        teardownRunThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        teardownIterationThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @TearDown(Level.Invocation)
    public void tearDownInvocation() {
        teardownInvocationThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
    }

    @TearDown(Level.Trial)
    public void teardownZZZ() { // should perform last
        Set<Boolean> expected = new HashSet<>();
        expected.add(this.isVirtual);
        Assert.assertEquals("test <: testInvocationThread", expected, testInvocationThread);
        Assert.assertEquals("test <: setupRun", expected, setupRunThread);
        Assert.assertEquals("test <: setupIterationThread", expected, setupIterationThread);
        Assert.assertEquals("test <: setupInvocationThread", expected, setupInvocationThread);
        Assert.assertEquals("test <: teardownRunThread", expected, teardownRunThread);
        Assert.assertEquals("test <: teardownIterationThread", expected, teardownIterationThread);
        Assert.assertEquals("test <: teardownInvocationThread", expected, teardownInvocationThread);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    @Threads(4)
    public void test() {
        testInvocationThread.add(VirtualAPI.isVirtual(Thread.currentThread()));
        Fixtures.work();
    }

    @Test
    public void invokeAPI_default() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .param("isVirtual", "false")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_fixed() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .jvmArgsAppend("-Djmh.executor=FIXED_TPE")
                    .param("isVirtual", "false")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_virtual() throws RunnerException {
        if (VirtualAPI.hasVirtualThreads()) {
            for (int c = 0; c < Fixtures.repetitionCount(); c++) {
                Options opt = new OptionsBuilder()
                        .include(Fixtures.getTestMask(this.getClass()))
                        .jvmArgsAppend("-Djmh.executor=VIRTUAL_TPE")
                        .param("isVirtual", "true")
                        .shouldFailOnError(true)
                        .build();
                new Runner(opt).run();
            }
        }
    }

    public static class VirtualAPI {
        // provide access to new Threads API via reflection

        private static final Method IS_VIRTUAL = getIsVirtual();

        private static Method getIsVirtual() {
            try {
                Method m = Class.forName("java.lang.Thread").getMethod("isVirtual");
                m.invoke(Thread.currentThread());
                // isVirtual check is not enough, have to check running virtual thread
                Method start = Class.forName("java.lang.Thread").getMethod("startVirtualThread", Runnable.class);
                start.invoke(null, (Runnable) (() -> {}));
                return m;
            } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                     IllegalAccessException e) {
                return null;
            }
        }

        public static boolean hasVirtualThreads() {
            return IS_VIRTUAL != null;
        }

        public static boolean isVirtual(Thread t) {
            if (!hasVirtualThreads()) {
                return false;
            }
            try {
                return (boolean) IS_VIRTUAL.invoke(t);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return false;
            }
        }
    }

}
