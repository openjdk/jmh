/*
 * Copyright (c) 2016, Red Hat Inc.
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

import junit.framework.Assert;
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

}
