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
package org.openjdk.jmh.logic;

import org.junit.Test;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Loop
 *
 * @author anders.astrand@oracle.com, staffan.friberg@oracle.com
 *
 */
public class TestLoop {

    @Test
    public void testDone() {
        long time = 1000;

        Loop loop = new Loop(time);
        loop.start();
        while (!loop.done()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        loop.end();

        // getTime() is in ns
        // Allow 50ms diff since sleep is inaccurate depending on system used
        assertEquals("Actual loop time differ from specified time", time, loop.getTime() / (1000000.0), 80);
    }

    @Test
    public void testPauseResume() {
        long time = 1000;
        boolean pause = false;
        Loop loop = new Loop(time);
        loop.start();
        while (!loop.done()) {
            try {
                if (pause) {
                    loop.pauseMeasurement();
                    Thread.sleep(50);
                    loop.resumeMeasurement();
                } else {
                    Thread.sleep(50);
                }
                pause = !pause;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        loop.end();

        // getTime() is in ns
        // Allow 50ms diff since sleep is inaccurate depending on system used
        assertEquals("Actual loop time differ from specified time", time / 2, loop.getTime() / (1000000.0), 80);
    }

    @Test
    public void testTimeUnitConvert() {
        long time = 100000;

        Loop loop = new Loop(new TimeValue(time, TimeUnit.MICROSECONDS));
        loop.start();
        while (!loop.done()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        loop.end();

        // getTime() is in ns
        // Allow 50ms diff since sleep is inaccurate depending on system used
        assertEquals("Actual loop time differ from specified time", time, loop.getTime() / (1000.0), 80000);
    }

    @Test
    public void testDuration() {
        assertEquals(0, new Loop(new TimeValue(1, TimeUnit.MICROSECONDS)).getDuration());
        assertEquals(TimeUnit.NANOSECONDS.convert(1,TimeUnit.MICROSECONDS), new Loop(new TimeValue(1, TimeUnit.MICROSECONDS)).getDuration(TimeUnit.NANOSECONDS));
        assertEquals(TimeUnit.MILLISECONDS.convert(1,TimeUnit.SECONDS), new Loop(new TimeValue(1, TimeUnit.SECONDS)).getDuration());
        assertEquals(17, new Loop(new TimeValue(17, TimeUnit.NANOSECONDS)).getDuration(TimeUnit.NANOSECONDS));
        assertEquals(42, new Loop(42).getDuration());
        assertEquals(42000000, new Loop(42).getDuration(TimeUnit.NANOSECONDS));
    }
}
