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
package org.openjdk.jmh.it.fails.inherit;

import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;

/**
 * Baseline test:
 * Checks if assertions are propagated back to integration tests.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@BenchmarkMode(Mode.All)
public class InheritBenchmarkBenchTearDownTest extends InheritableBenchmarkTearDownState {

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1)
    @Threads(4)
    public void test() {
        Fixtures.work();
    }

    @Test
    public void invoke() {
        boolean failed;
        try {
            Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe");
            failed = false;
        } catch (Throwable t) {
            failed = true;
        }
        junit.framework.Assert.assertTrue("Should have failed", failed);
    }

}
