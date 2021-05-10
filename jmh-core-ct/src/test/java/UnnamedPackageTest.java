/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.ct.CompileTest;
import org.openjdk.jmh.ct.InMemoryGeneratorDestination;

import java.util.concurrent.atomic.AtomicInteger;

public class UnnamedPackageTest {

    @State(Scope.Benchmark)
    public static class S {
        @Setup(Level.Trial)
        public void setup(AtomicInteger v) {}
    }

    @Benchmark
    public void test(S s) {

    }

    @Test
    public void compileTest() {
        InMemoryGeneratorDestination destination = new InMemoryGeneratorDestination();
        boolean success = CompileTest.doTest(getClass(), destination);
        if (success) {
            Assert.fail("Should have failed.");
        }
        String error = destination.getErrors().get(0);
        Assert.assertTrue(error, error.contains("Benchmark class should have package other than default."));
    }
}
