/*
 * Copyright (c) 2016, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.it.times.fail;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Group)
public class GroupIterationFailureTimesTest {

    private static final AtomicInteger count = new AtomicInteger();

    @Setup(Level.Iteration)
    public void setup() {
        System.err.println("Enter " + Thread.currentThread());
        count.incrementAndGet();
        throw new IllegalStateException("Failing");
    }

    @Benchmark
    @Group
    @GroupThreads(4) // should match the total number of threads
    public void test() {
        Fixtures.work();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (int c = 0; c < Fixtures.repetitionCount(); c++) {
            for (Mode mode : Mode.values()) {
                if (mode == Mode.All) continue;

                count.set(0);

                Options opt = new OptionsBuilder()
                        .include(Fixtures.getTestMask(this.getClass()))
                        .mode(mode)
                        .threads(4)
                        .forks(0)
                        .build();
                new Runner(opt).run();

                Assert.assertEquals(1, count.get());
            }
        }
    }


}
