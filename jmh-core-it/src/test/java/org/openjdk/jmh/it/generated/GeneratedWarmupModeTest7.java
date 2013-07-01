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
package org.openjdk.jmh.it.generated;


import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.MicroBenchmark;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.logic.Loop;
import org.openjdk.jmh.logic.results.AverageTimePerOp;
import org.openjdk.jmh.logic.results.Result;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 * Tests if harness honors warmup command line settings like:
 * -wmb
 * -wm
 * -frw
 * ....
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
public class GeneratedWarmupModeTest7 {

    private static Queue<String> testSequence = new ConcurrentLinkedQueue<String>();

    @MicroBenchmark
    public Result testBig(Loop loop) {
        if (loop.getDuration() == 1000) { // warmup
            testSequence.add("W");
        } else if (loop.getDuration() == 2000) {  // iteration
            testSequence.add("I");
        }
        loop.start();
        while (!loop.done()) {
            Fixtures.work();
        }

        return new AverageTimePerOp("test", 1, 42, TimeUnit.NANOSECONDS);
    }


    @MicroBenchmark
    public Result testSmall(Loop loop) {
        if (loop.getDuration() == 1000) { // warmup
            testSequence.add("w");
        } else if (loop.getDuration() == 2000) {  // iteration
            testSequence.add("i");
        }
        loop.start();
        while (!loop.done()) {
            Fixtures.work();
        }

        return new AverageTimePerOp("test", 1, 42, TimeUnit.NANOSECONDS);
    }


    private static String getSequence() {
        StringBuilder sb = new StringBuilder();
        for (String s : testSequence) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    public void invoke7() {
        testSequence.clear();
        Main.testMain(Fixtures.getTestMask(this.getClass()) + ".testBig.* -foe -w 1 -r 2 -tc 1,2,3 -i 1 -wi 2 -f false");
        assertEquals("WWIIIIII", getSequence());
    }

}
