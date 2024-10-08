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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XCTraceTableOfContentsHandlerTest extends XCTraceTestBase {
    private final XCTraceTableOfContentsHandler handler = new XCTraceTableOfContentsHandler();

    @Test
    public void parseDocumentWithoutToc() throws Exception {
        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertTrue(handler.getSupportedTables().isEmpty());
    }

    @Test
    public void parsePmcToc() throws Exception {
        factory.newSAXParser().parse(openResource("pmc-toc.xml"), handler);
        List<XCTraceTableHandler.XCTraceTableDesc> tables = handler.getSupportedTables();
        assertEquals(1693140580479L, handler.getRecordStartMs());
        assertEquals(1, tables.size());

        XCTraceTableHandler.XCTraceTableDesc table = tables.get(0);
        assertEquals(XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE, table.getTableType());

        assertEquals(XCTraceTableHandler.TriggerType.PMI, table.getTriggerType());
        assertEquals(1000000L, table.triggerThreshold());
        assertEquals("MEM_INST_RETIRED.ALL_LOADS", table.triggerEvent());
        assertEquals(Arrays.asList(
                "L1D_CACHE_MISS_LD", "MEM_LOAD_RETIRED.L1_HIT"
        ), table.getPmcEvents());
    }

    @Test
    public void parseTimeToc() throws Exception {
        factory.newSAXParser().parse(openResource("time-toc.xml"), handler);
        assertEquals(1693153606998L, handler.getRecordStartMs());
        List<XCTraceTableHandler.XCTraceTableDesc> tables = handler.getSupportedTables();
        assertEquals(1, tables.size());

        XCTraceTableHandler.XCTraceTableDesc table = tables.get(0);
        assertEquals(XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE, table.getTableType());

        assertEquals(XCTraceTableHandler.TriggerType.TIME, table.getTriggerType());
        assertEquals(1000L, table.triggerThreshold());
        assertEquals("TIME_MICRO_SEC", table.triggerEvent());
        assertEquals(Arrays.asList(
                "INST_ALL", "CORE_ACTIVE_CYCLE", "INST_BRANCH"
        ), table.getPmcEvents());
    }

    @Test
    public void parseMixedToc() throws Exception {
        factory.newSAXParser().parse(openResource("mixed-toc.xml"), handler);
        assertEquals(1693153762702L, handler.getRecordStartMs());
        List<XCTraceTableHandler.XCTraceTableDesc> tables = handler.getSupportedTables();
        assertEquals(2, tables.size());

        assertTrue(tables.stream().anyMatch(t -> t.getTableType() == XCTraceTableHandler.ProfilingTableType.COUNTERS_PROFILE));
        assertTrue(tables.stream().anyMatch(t -> t.getTableType() == XCTraceTableHandler.ProfilingTableType.CPU_PROFILE));
    }

    @Test
    public void parseCpuProfileToc() throws Exception {
        factory.newSAXParser().parse(openResource("cpu-prof-toc.xml"), handler);
        assertEquals(1693158302632L, handler.getRecordStartMs());
        List<XCTraceTableHandler.XCTraceTableDesc> tables = handler.getSupportedTables();
        assertEquals(1, tables.size());
        assertEquals(XCTraceTableHandler.ProfilingTableType.CPU_PROFILE, tables.get(0).getTableType());
    }

    @Test
    public void parseTimeProfileToc() throws Exception {
        factory.newSAXParser().parse(openResource("time-prof-toc.xml"), handler);
        assertEquals(1693158448830L, handler.getRecordStartMs());
        List<XCTraceTableHandler.XCTraceTableDesc> tables = handler.getSupportedTables();
        assertEquals(1, tables.size());
        assertEquals(XCTraceTableHandler.ProfilingTableType.TIME_PROFILE, tables.get(0).getTableType());
    }
}
