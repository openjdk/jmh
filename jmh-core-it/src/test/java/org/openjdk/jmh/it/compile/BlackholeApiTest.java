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
package org.openjdk.jmh.it.compile;

import org.openjdk.jmh.annotations.BenchmarkType;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.logic.BlackHole;

import java.util.concurrent.TimeUnit;

/**
 * Tests basic blackholing API.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class BlackholeApiTest {

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void testNothing() throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public Object testReturnObject() throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
        return null;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public int testReturnInt() throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
        return 0;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public void testBH(BlackHole bh) throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public Object testBH_ReturnObject(BlackHole bh) throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
        return null;
    }

    @GenerateMicroBenchmark(BenchmarkType.All)
    public int testBH_ReturnInt(BlackHole bh) throws InterruptedException {
        // courtesy for parallel-running tests
        TimeUnit.MILLISECONDS.sleep(100);
        return 0;
    }


}
