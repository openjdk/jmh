/**
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

/**
 * Tests global setup -> run -> tearDown sequence.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class ThreadStateOrderTest {

    @State(Scope.Thread)
    public static class MyState {
        private volatile long tickSetInstance;
        private volatile long tickSetIteration;
        private volatile long tickSetInvocation;
        private volatile long tickTearInstance;
        private volatile long tickTearIteration;
        private volatile long tickTearInvocation;
        private volatile long tickRun;

        @Setup(Level.Trial)
        public void setupInstance() {
            tickSetInstance = System.nanoTime();
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            tickSetIteration = System.nanoTime();
        }

        @Setup(Level.Invocation)
        public void setupInvocation() {
            tickSetInvocation = System.nanoTime();
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {
            tickTearInvocation = System.nanoTime();
        }

        @TearDown(Level.Iteration)
        public void tearDownIteration() {
            tickTearIteration = System.nanoTime();
        }

        @TearDown(Level.Trial)
        public void tearDownInstance() {
            tickTearInstance = System.nanoTime();

            Assert.assertTrue("Setup/instance called before setup/iteration", tickSetInstance < tickSetIteration);
            Assert.assertTrue("Setup/iteration called before setup/invocation", tickSetIteration < tickSetInvocation);
            Assert.assertTrue("Setup/invocation called before run", tickSetInvocation < tickRun);
            Assert.assertTrue("Run called before tear/invocation", tickRun < tickTearInvocation);
            Assert.assertTrue("Tear/invocation called before tear/iteration", tickTearInvocation < tickTearIteration);
            Assert.assertTrue("Tear/iteration called before tear/instance", tickTearIteration < tickTearInstance);
        }
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(1)
    public void test(MyState state) {
        state.tickRun = System.nanoTime();
        Fixtures.work();
    }

    @org.junit.Test
    public void invoke() {
        Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe -v");
    }

}
