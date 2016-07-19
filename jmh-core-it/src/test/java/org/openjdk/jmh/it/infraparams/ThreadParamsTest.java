/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.infraparams;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

@State(Scope.Benchmark)
public class ThreadParamsTest {

    Collection<ThreadParams> set;

    @Setup(Level.Iteration)
    public void setup() {
        set = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<ThreadParams, Boolean>()));
    }

    @TearDown(Level.Iteration)
    public void verify() {
        Assert.assertEquals(4, set.size());
    }

    @Benchmark
    @Threads(4)
    public void test(ThreadParams params) {
        set.add(params);
        Fixtures.work();
    }

    @Test
    public void test() throws RunnerException, CommandLineOptionException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .shouldFailOnError(true)
                .build();

        new Runner(opts).run();
    }

}
