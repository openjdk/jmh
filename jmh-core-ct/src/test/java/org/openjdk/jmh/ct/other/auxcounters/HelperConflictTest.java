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
package org.openjdk.jmh.ct.other.auxcounters;

import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.ct.CompileTest;

public class HelperConflictTest {

    @AuxCounters
    @State(Scope.Thread)
    public static class S {
        @Setup(Level.Trial)
        public void setupTrial() {}

        @Setup(Level.Iteration)
        public void setupIteration() {}

        @Setup(Level.Invocation)
        public void setupInvocation() {}

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {}

        @TearDown(Level.Iteration)
        public void tearDownIteration() {}

        @TearDown(Level.Trial)
        public void tearDownTrial() {}
    }

    @Benchmark
    public void benchmark(S s) {
        // intentionally left blank
    }

    @Test
    public void compileTest() {
        CompileTest.assertOK(this.getClass());
    }

}
