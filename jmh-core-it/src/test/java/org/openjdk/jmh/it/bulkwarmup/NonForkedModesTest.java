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
package org.openjdk.jmh.it.bulkwarmup;


import org.junit.Test;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.logic.Control;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.WarmupMode;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static junit.framework.Assert.assertEquals;

/**
 * @author Aleksey Shipilev
 */
@State(Scope.Thread)
public class NonForkedModesTest {

    private static Queue<String> testSequence = new ConcurrentLinkedQueue<String>();

    boolean recorded;

    @Setup(Level.Iteration)
    public void oneShot() {
        recorded = false;
    }

    @GenerateMicroBenchmark
    public void testBig(Control cnt) {
        if (!recorded) {
            recorded = true;
            if (cnt.iterationTime == 100) { // warmup
                testSequence.add("W");
            } else if (cnt.iterationTime == 200) {  // iteration
                testSequence.add("I");
            }
        }
        Fixtures.work();
    }

    @GenerateMicroBenchmark
    public void testSmall(Control cnt) {
        if (!recorded) {
            recorded = true;
            if (cnt.iterationTime == 100) { // warmup
                testSequence.add("w");
            } else if (cnt.iterationTime == 200) {  // iteration
                testSequence.add("i");
            }
        }
        Fixtures.work();
    }

    private static String getSequence() {
        StringBuilder sb = new StringBuilder();
        for (String s : testSequence) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    public void invokeINDI() throws RunnerException {
        testWith(WarmupMode.INDI, "WIwi");
    }

    @Test
    public void invokeBULK() throws RunnerException {
        testWith(WarmupMode.BULK, "WwIi");
    }

    @Test
    public void invokeBULK_INDI() throws RunnerException {
        testWith(WarmupMode.BULK_INDI, "WwWIwi");
    }

    public void testWith(WarmupMode mode, String runSeq) throws RunnerException {
        testSequence.clear();

        Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .warmupIterations(1)
                .warmupTime(TimeValue.milliseconds(100))
                .measurementIterations(1)
                .measurementTime(TimeValue.milliseconds(200))
                .threads(1)
                .forks(0)
                .syncIterations(false)
                .warmupMode(mode)
                .build();
        new Runner(opt).run();

        assertEquals(runSeq, getSequence());
    }

}
