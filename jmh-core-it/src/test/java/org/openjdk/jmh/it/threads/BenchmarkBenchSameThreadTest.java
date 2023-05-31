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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Tests if harness executes setup, run, and tearDown in the same workers.
 * Also tests if all workers are platform or virtual threads.
 */
@State(Scope.Benchmark)
public class BenchmarkBenchSameThreadTest {

    public enum ExecutorType {
        CACHED_TPE, FIXED_TPE, VIRTUAL_TPE, FJP, CUSTOM

    }

    @Param("FIXED_TPE")
    ExecutorType benchmarkExecutorType;

    private final Set<Thread> setupRunThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> setupIterationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> setupInvocationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> teardownRunThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> teardownIterationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> teardownInvocationThread = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> testInvocationThread = Collections.synchronizedSet(new HashSet<>());

    @Setup(Level.Trial)
    public void setupRun() {
        setupRunThread.add(Thread.currentThread());
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        setupIterationThread.add(Thread.currentThread());
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        setupInvocationThread.add(Thread.currentThread());
    }

    @TearDown(Level.Trial)
    public void tearDownRun() {
        teardownRunThread.add(Thread.currentThread());
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        teardownIterationThread.add(Thread.currentThread());
    }

    @TearDown(Level.Invocation)
    public void tearDownInvocation() {
        teardownInvocationThread.add(Thread.currentThread());
    }

    @TearDown(Level.Trial)
    public void teardownZZZ() { // should perform last
        Assert.assertFalse("Test sanity", testInvocationThread.isEmpty());
        if (benchmarkExecutorType == ExecutorType.FIXED_TPE ||
                benchmarkExecutorType == ExecutorType.VIRTUAL_TPE) { // only fixed and virtual guarantee same thread rule
            Assert.assertTrue("test <: setupRun", testInvocationThread.containsAll(setupRunThread));
            Assert.assertTrue("test <: setupIteration", testInvocationThread.containsAll(setupIterationThread));
            Assert.assertTrue("test <: setupInvocation", testInvocationThread.containsAll(setupInvocationThread));
            Assert.assertTrue("test <: teardownRun", testInvocationThread.containsAll(teardownRunThread));
            Assert.assertTrue("test <: teardownIteration", testInvocationThread.containsAll(teardownIterationThread));
            Assert.assertTrue("test <: teardownInvocation", testInvocationThread.containsAll(teardownInvocationThread));
        }
        if (benchmarkExecutorType == ExecutorType.VIRTUAL_TPE) {
            Assert.assertTrue("setupRun thread kind", setupRunThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("setupIteration thread kind", setupIterationThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("setupInvocation thread kind", setupInvocationThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownRun thread kind", teardownRunThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownIteration thread kind", teardownIterationThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownInvocation thread kind", teardownInvocationThread.stream().allMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("testInvocation thread kind", testInvocationThread.stream().allMatch(VirtualAPI::isVirtual));
        } else {
            Assert.assertTrue("setupRun thread kind", setupRunThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("setupIteration thread kind", setupIterationThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("setupInvocation thread kind", setupInvocationThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownRun thread kind", teardownRunThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownIteration thread kind", teardownIterationThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("teardownInvocation thread kind", teardownInvocationThread.stream().noneMatch(VirtualAPI::isVirtual));
            Assert.assertTrue("testInvocation thread kind", testInvocationThread.stream().noneMatch(VirtualAPI::isVirtual));
        }
        if (benchmarkExecutorType == ExecutorType.FJP) {
            Assert.assertTrue("setupRun thread kind", setupRunThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("setupIteration thread kind", setupIterationThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("setupInvocation thread kind", setupInvocationThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("teardownRun thread kind", teardownRunThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("teardownIteration thread kind", teardownIterationThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("teardownInvocation thread kind", teardownInvocationThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
            Assert.assertTrue("testInvocation thread kind", testInvocationThread.stream().allMatch(t -> t instanceof ForkJoinWorkerThread));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    @Threads(4)
    public void test() {
        testInvocationThread.add(Thread.currentThread());
        Fixtures.work();
    }

    @Test
    public void invokeAPI_default() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
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
                    .param("benchmarkExecutorType", "FIXED_TPE")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_cached() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .jvmArgsAppend("-Djmh.executor=CACHED_TPE")
                    .param("benchmarkExecutorType", "CACHED_TPE")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_fjp() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .jvmArgsAppend("-Djmh.executor=FJP")
                    .param("benchmarkExecutorType", "FJP")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_fjp_common() throws RunnerException {
        int cores = Runtime.getRuntime().availableProcessors();
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .jvmArgsAppend("-Djmh.executor=FJP_COMMON")
                    .param("benchmarkExecutorType", "FJP")
                    .threads(cores)
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_custom() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            Options opt = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .jvmArgsAppend("-Djmh.executor=CUSTOM")
                    .jvmArgsAppend("-Djmh.executor.class=" + CustomExecutor.class.getName())
                    .param("benchmarkExecutorType", "CUSTOM")
                    .shouldFailOnError(true)
                    .build();
            new Runner(opt).run();
        }
    }

    @Test
    public void invokeAPI_virtual() throws RunnerException {
        if(VirtualAPI.hasVirtualThreads()) {
            for (int c = 0; c < Fixtures.repetitionCount(); c++) {
                Options opt = new OptionsBuilder()
                        .include(Fixtures.getTestMask(this.getClass()))
                        .jvmArgsAppend("-Djmh.executor=VIRTUAL_TPE")
                        .param("benchmarkExecutorType", "VIRTUAL_TPE")
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

    static class CustomExecutor implements ExecutorService {
        private final ExecutorService e;

        public CustomExecutor(int maxThreads, String prefix) {
            e = Executors.newFixedThreadPool(maxThreads, new CustomThreadFactory(prefix));
        }

        public void execute(Runnable command) {
            e.execute(command);
        }

        public void shutdown() {
            e.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return e.shutdownNow();
        }

        public boolean isShutdown() {
            return e.isShutdown();
        }

        public boolean isTerminated() {
            return e.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }

        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    static class CustomThreadFactory implements ThreadFactory {

        private final AtomicInteger counter;
        private final String prefix;

        public CustomThreadFactory(String prefix) {
            this.counter = new AtomicInteger();
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            CustomThread t = new CustomThread(r, prefix + "-jmh-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }

    static class CustomThread extends Thread {
        public CustomThread(Runnable r, String name) {
            super(r, name);
        }
    }

}
