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
package org.openjdk.jmh.it.sharing;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class GroupDefaultStateSharingTest {

    @State(Scope.Group)
    public static class MyState {
        Set<Thread> visitors = Collections.synchronizedSet(new HashSet<Thread>());

        @TearDown(Level.Trial)
        public void tearDown() {
            Assert.assertEquals("All the threads have visited this state", 2, visitors.size());
        }
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(BenchmarkType.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 2, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Threads(2)
    public void test(MyState s) {
        Fixtures.work();
        s.visitors.add(Thread.currentThread());
    }

    @Test
    public void invoke() {
        Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe");
    }

}
