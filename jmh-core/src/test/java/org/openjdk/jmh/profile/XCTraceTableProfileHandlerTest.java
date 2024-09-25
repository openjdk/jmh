/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class XCTraceTableProfileHandlerTest extends XCTraceTestBase {
    @Test
    public void sanityTest() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        XCTraceTableProfileHandler handler = new XCTraceTableProfileHandler(XCTraceTableHandler.ProfilingTableType.CPU_PROFILE, sample -> {
            count.incrementAndGet();
        });

        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertEquals(584, count.get());
    }

    @Test
    public void parseCpuProfile() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.CPU_PROFILE, "cpu-profile", true);
    }

    @Test
    public void parseCpuProfileXcode14_0_1() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.CPU_PROFILE, "cpu-profile.xcode14.0.1", false);
    }

    @Test
    public void unsupportedSchema() throws Exception {
        XCTraceTableProfileHandler handler = new XCTraceTableProfileHandler(XCTraceTableHandler.ProfilingTableType.CPU_PROFILE, sample -> fail("Expected no samples"));
        assertThrows(IllegalStateException.class, () ->
                factory.newSAXParser().parse(openResource("counters-profile.xml"), handler));
    }

    @Test
    public void parseCountersProfile() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE, "counters-profile", true);
    }

    @Test
    public void parseCountersTimeProfile() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE, "counters-time-profile", true);
    }

    @Test
    public void parseTimeProfile() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.TIME_PROFILE, "time-profile", true);
    }

    @Test
    public void parseTimeProfileXcode12_5() throws Exception {
        verifyProfile(XCTraceTableHandler.ProfilingTableType.TIME_PROFILE, "time-profile.xcode12.5", false);
    }

    private void verifyProfile(XCTraceTableHandler.ProfilingTableType tableType, String profileName,
                               boolean fixAddress) throws Exception {
        List<XCTraceTableProfileHandler.XCTraceSample> samples = new ArrayList<>();
        XCTraceTableProfileHandler handler = new XCTraceTableProfileHandler(tableType, samples::add);
        factory.newSAXParser().parse(openResource(profileName + ".xml"), handler);

        List<Object[]> expectedRows = readExpectedData(profileName + ".csv");
        assertEquals(expectedRows.size(), samples.size());
        for (int idx = 0; idx < expectedRows.size(); idx++) {
            assertRowEquals(idx, expectedRows.get(idx), samples.get(idx), fixAddress);
        }
    }

    private void assertRowEquals(int rowIndex, Object[] expectedRow, XCTraceTableProfileHandler.XCTraceSample actualRow,
                                 boolean fixAddress) {
        assertEquals("Timestamp for row " + rowIndex,
                ((Long) expectedRow[0]).longValue(), actualRow.getTimeFromStartNs());
        assertEquals("Weight for row " + rowIndex,
                ((Long) expectedRow[1]).longValue(), actualRow.getWeight());
        long expectedAddress = (Long) expectedRow[2];
        // latest xctrace versions contain well-formatted backtraces,
        // but addresses there are always one byte past an actual IP
        if (expectedAddress > 0 && fixAddress) expectedAddress--;
        assertEquals("Address for row " + rowIndex,
                expectedAddress, actualRow.getAddress());
        assertEquals("Symbol for row " + rowIndex,
                expectedRow[3], actualRow.getSymbol());
        assertEquals("Library for row " + rowIndex,
                expectedRow[4], actualRow.getBinary());
        assertArrayEquals("PMC values for row " + rowIndex,
                (long[]) expectedRow[5], actualRow.getPmcValues());
    }
}
