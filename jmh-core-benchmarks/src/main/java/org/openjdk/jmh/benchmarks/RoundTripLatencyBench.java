/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmh.benchmarks;

import org.openjdk.jmh.validation.SpinWaitSupport;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.validation.AffinitySupport;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class RoundTripLatencyBench {

    static final boolean SPINWAIT = Boolean.getBoolean("spinWait");

    @Param("-1")
    int p;

    @Param("-1")
    int c;

    Thread t;

    volatile boolean ping;

    @Setup
    public void setup() {
        if (c != -1) AffinitySupport.bind(c);
        t = new Thread(() -> {
            if (p != -1) AffinitySupport.bind(p);
            Thread t = Thread.currentThread();
            while (!t.isInterrupted()) {
                while (!ping) {
                    if (SPINWAIT) SpinWaitSupport.onSpinWait();
                }
                ping = false;
            }
        });
        t.start();
    }

    @TearDown
    public void tearDown() throws InterruptedException {
        t.interrupt();
        ping = true;
        t.join();
    }

    @Benchmark
    public void test() {
        ping = true;
        while (ping) {
            if (SPINWAIT) SpinWaitSupport.onSpinWait();
        }
    }
}
