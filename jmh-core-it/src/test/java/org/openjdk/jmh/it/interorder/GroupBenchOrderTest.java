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
package org.openjdk.jmh.it.interorder;

import junit.framework.Assert;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests global setup -> run -> tearDown sequence.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@State(Scope.Group)
public class GroupBenchOrderTest {

    public static final AtomicInteger TICKER = new AtomicInteger();

    private volatile int tickSetInstance;
    private volatile int tickSetIteration;
    private volatile int tickSetInvocation;
    private volatile int tickTearInstance;
    private volatile int tickTearIteration;
    private volatile int tickTearInvocation;
    private volatile int tickRun;

    @Setup(Level.Trial)
    public void setupInstance() {
        tickSetInstance = TICKER.incrementAndGet();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        tickSetIteration = TICKER.incrementAndGet();
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        tickSetInvocation = TICKER.incrementAndGet();
    }

    @TearDown(Level.Invocation)
    public void tearDownInvocation() {
        tickTearInvocation = TICKER.incrementAndGet();
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        tickTearIteration = TICKER.incrementAndGet();
    }

    @TearDown(Level.Trial)
    public void tearDownInstance() {
        tickTearInstance = TICKER.incrementAndGet();

        Assert.assertTrue("Setup/instance called before setup/iteration", tickSetInstance < tickSetIteration);
        Assert.assertTrue("Setup/iteration called before setup/invocation", tickSetIteration < tickSetInvocation);
        Assert.assertTrue("Setup/invocation called before run", tickSetInvocation < tickRun);
        Assert.assertTrue("Run called before tear/invocation", tickRun < tickTearInvocation);
        Assert.assertTrue("Tear/invocation called before tear/iteration", tickTearInvocation < tickTearIteration);
        Assert.assertTrue("Tear/iteration called before tear/instance", tickTearIteration < tickTearInstance);
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(1)
    public void test() {
        tickRun = TICKER.incrementAndGet();
        Fixtures.work();
    }

    @org.junit.Test
    public void invoke() {
        Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe -v -si false");
    }

}
