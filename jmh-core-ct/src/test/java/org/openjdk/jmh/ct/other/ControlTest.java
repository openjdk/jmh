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
package org.openjdk.jmh.ct.other;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.ct.CompileTest;
import org.openjdk.jmh.infra.Control;

public class ControlTest {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

    }

    @State(Scope.Thread)
    public static class ThreadState {

    }

    @State(Scope.Group)
    public static class GroupState {

    }

    @Benchmark
    @Group("plain")
    public void plain_test1(Control cnt) {

    }

    @Benchmark
    @Group("plain")
    public void plain_test2(Control cnt) {

    }

    @Benchmark
    @Group("plain")
    public void plain_test3() {

    }

    @Benchmark
    @Group("bench")
    public void bench_test1(BenchmarkState s, Control cnt) {

    }

    @Benchmark
    @Group("bench")
    public void bench_test2(BenchmarkState s, Control cnt) {

    }

    @Benchmark
    @Group("bench")
    public void bench_test3(BenchmarkState s) {

    }

    @Benchmark
    @Group("thread")
    public void thread_test1(ThreadState s, Control cnt) {

    }

    @Benchmark
    @Group("thread")
    public void thread_test2(ThreadState s, Control cnt) {

    }

    @Benchmark
    @Group("thread")
    public void thread_test3(ThreadState s) {

    }

    @Benchmark
    @Group("group")
    public void group_test1(GroupState s, Control cnt) {

    }

    @Benchmark
    @Group("group")
    public void group_test2(GroupState s, Control cnt) {

    }

    @Benchmark
    @Group("group")
    public void group_test3(GroupState s) {

    }

    @Test
    public void compileTest() {
        CompileTest.assertOK(this.getClass());
    }

}
