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

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
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
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.All)
@State(Scope.Benchmark)
public class ZeroThreadCountTest {

    private Set<Thread> threads = Collections.synchronizedSet(new HashSet<Thread>());

    @TearDown(Level.Iteration)
    public void tearDown() {
        Assert.assertEquals("amount of threads should be Runtime.getRuntime().availableProcessors()", threads.size(), Runtime.getRuntime().availableProcessors());
    }

    @GenerateMicroBenchmark
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Warmup(iterations = 0)
    @Threads(0)
    public void test1() {
        threads.add(Thread.currentThread());
        Fixtures.work();
    }

    @GenerateMicroBenchmark
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Warmup(iterations = 0)
    public void test2() {
        threads.add(Thread.currentThread());
        Fixtures.work();
    }

    @Test
    public void invokeCLI_1() {
        Main.testMain(Fixtures.getTestMask(this.getClass())+".*test1" + " -foe");
    }

    @Test
    public void invokeCLI_2() {
        Main.testMain(Fixtures.getTestMask(this.getClass())+ ".*test2" + " -foe -t 0");
    }

    @Test
    public void invokeAPI_1() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass())+".*test1")
                .failOnError(true)
                .build();
        new Runner(opt).run();
    }

    @Test
    public void invokeAPI_2() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass())+".*test2")
                .failOnError(true)
                .threads(0)
                .build();
        new Runner(opt).run();
    }
}
