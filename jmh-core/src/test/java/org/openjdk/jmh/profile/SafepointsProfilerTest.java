/*
 * Copyright (c) 2016, 2026, Red Hat Inc.
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
package org.openjdk.jmh.profile;

import org.junit.Assert;
import org.junit.Test;

public class SafepointsProfilerTest {

    @Test
    public void parseJDK7u77_Point() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "1.095: Total time for which application threads were stopped: 0.0014010 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(1_095_000_000L, data.timestamp);
        Assert.assertEquals(    1_401_000L, data.stopTime);
        Assert.assertEquals(Long.MIN_VALUE, data.ttspTime);
    }

    @Test
    public void parseJDK7u77_Comma() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "1,095: Total time for which application threads were stopped: 0,0014010 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(1_095_000_000L, data.timestamp);
        Assert.assertEquals(    1_401_000L, data.stopTime);
        Assert.assertEquals(Long.MIN_VALUE, data.ttspTime);
    }

    @Test
    public void parseJDK8u101_Dot() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "5.042: Total time for which application threads were stopped: 0.0028944 seconds, Stopping threads took: 0.0028351 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(5_042_000_000L, data.timestamp);
        Assert.assertEquals(    2_894_400L, data.stopTime);
        Assert.assertEquals(    2_835_100L, data.ttspTime);
    }

    @Test
    public void parseJDK8u101_Comma() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "5,042: Total time for which application threads were stopped: 0,0028944 seconds, Stopping threads took: 0,0028351 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(5_042_000_000L, data.timestamp);
        Assert.assertEquals(    2_894_400L, data.stopTime);
        Assert.assertEquals(    2_835_100L, data.ttspTime);
    }

    @Test
    public void parseJDK9b140_Dot() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[71.633s][info][safepoint] Total time for which application threads were stopped: 0.0359611 seconds, Stopping threads took: 0.0000516 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(71_633_000_000L, data.timestamp);
        Assert.assertEquals(    35_961_100L, data.stopTime);
        Assert.assertEquals(        51_600L, data.ttspTime);
    }

    @Test
    public void parseJDK9b140_Comma() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[71,633s][info][safepoint] Total time for which application threads were stopped: 0,0359611 seconds, Stopping threads took: 0,0000516 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(71_633_000_000L, data.timestamp);
        Assert.assertEquals(    35_961_100L, data.stopTime);
        Assert.assertEquals(        51_600L, data.ttspTime);
    }

    @Test
    public void parseJDK9b140_Whitespace() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[71,633s][info][safepoint    ] Total time for which application threads were stopped: 0,0359611 seconds, Stopping threads took: 0.0000516 seconds");
        Assert.assertNotNull(data);
        Assert.assertEquals(71_633_000_000L, data.timestamp);
        Assert.assertEquals(    35_961_100L, data.stopTime);
        Assert.assertEquals(        51_600L, data.ttspTime);
    }

    @Test
    public void parseJDK17() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[2.153s][info][safepoint] Safepoint \"ICBufferFull\", Time since last: 202293883 ns, Reaching safepoint: 2070 ns, Cleanup: 88040 ns, At safepoint: 1110 ns, Total: 91220 ns");
        Assert.assertNotNull(data);
        Assert.assertEquals(2_153_000_000L, data.timestamp);
        Assert.assertEquals(        91_220L, data.stopTime);
        Assert.assertEquals(         2_070L, data.ttspTime);
    }

    @Test
    public void parseJDK21() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[2.336s][info][safepoint] Safepoint \"ICBufferFull\", Time since last: 197008615 ns, Reaching safepoint: 3740 ns, Cleanup: 103391 ns, At safepoint: 360 ns, Leaving safepoint: 1650 ns, Total: 109141 ns");
        Assert.assertNotNull(data);
        Assert.assertEquals( 2_336_000_000L, data.timestamp);
        Assert.assertEquals(        109_141L, data.stopTime);
        Assert.assertEquals(          3_740L, data.ttspTime);

    }

    @Test
    public void parseJDK25() {
        SafepointsProfiler.ParsedData data = SafepointsProfiler.parse(
                "[2.756s][info][safepoint] Safepoint \"G1CollectForAllocation\", Time since last: 195656816 ns, Reaching safepoint: 3690 ns, At safepoint: 3314127 ns, Leaving safepoint: 4800 ns, Total: 3322617 ns, Threads: 0 runnable, 27 total");
        Assert.assertNotNull(data);
        Assert.assertEquals(2_756_000_000L, data.timestamp);
        Assert.assertEquals(     3_322_617L, data.stopTime);
        Assert.assertEquals(         3_690L, data.ttspTime);
    }

}
