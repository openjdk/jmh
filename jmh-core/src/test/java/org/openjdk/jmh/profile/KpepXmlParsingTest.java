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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class KpepXmlParsingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File copyResourceToFile(String resource) throws IOException {
        InputStream stream = KpepXmlParsingTest.class.getResourceAsStream(resource);
        Assert.assertNotNull("Resource not found: " + resource, stream);

        File tempFile = temporaryFolder.newFile();
        Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private void checkEvent(XCTraceSupport.PerfEvents db, String name, boolean isFixed, long mask,
                            String description, String fallback) {
        XCTraceSupport.PerfEventInfo event = db.getEvent(name);

        Assert.assertNotNull("Event not found: " + name, event);
        Assert.assertEquals("mame not matched", name, event.getName());
        Assert.assertEquals("isFixed not matched for event " + name, isFixed, event.isFixed());
        Assert.assertEquals("counterMask not matched for event " + name, mask, event.getCounterMask());
        Assert.assertEquals("description not matched for event " + name, description, event.getDescription());
        Assert.assertEquals("fallback not matched for event " + name, fallback, event.getFallbackEvent());
    }

    @Test
    public void testIntelDbParsing() throws ProfilerException, IOException {
        XCTraceSupport.PerfEvents db = XCTraceSupport.parseKpepXmlFile(
                copyResourceToFile("/org.openjdk.jmh.profile.xctrace/haswell.xml"));

        Assert.assertEquals("x86_64", db.getArchitecture());
        Assert.assertEquals(7 /* 3 fixed, 4 configurable */, db.getMaxCounters());
        Assert.assertEquals(0x7L, db.getFixedCountersMask());
        Assert.assertEquals(0x78L, db.getConfigurableCountersMask());

        Assert.assertEquals(305, db.getAllEvents().size());

        checkEvent(db, "CPU_CLK_UNHALTED.THREAD", true, 2L,
                "Core cycles when the core is not in halt state.",
                "CPU_CLK_UNHALTED.THREAD_P");
        checkEvent(db, "CPU_CLK_UNHALTED.THREAD_P", false, 120L,
                "Thread cycles when thread is not in halt state", null);
        checkEvent(db, "INST_RETIRED.PREC_DIST", false, 16L,
                "Precise instruction retired event with HW to reduce effect of PEBS shadow in IP distribution",
                null);

        XCTraceSupport.PerfEventInfo instAll = db.getEvent("INST_ALL");
        Assert.assertNotNull(instAll);
        Assert.assertEquals("INST_RETIRED.ANY", instAll.getName());

        XCTraceSupport.PerfEventInfo coreCycles = db.getEvent("CORE_ACTIVE_CYCLE");
        Assert.assertNotNull(coreCycles);
        Assert.assertEquals("CPU_CLK_UNHALTED.THREAD", coreCycles.getName());
    }

    @Test
    public void testAppleSiliconDbParsing() throws ProfilerException, IOException {
        XCTraceSupport.PerfEvents db = XCTraceSupport.parseKpepXmlFile(
                copyResourceToFile("/org.openjdk.jmh.profile.xctrace/a16.xml"));

        Assert.assertEquals("arm64", db.getArchitecture());
        Assert.assertEquals(10 /* 2 fixed, 8 configurable */, db.getMaxCounters());
        Assert.assertEquals(0x3L, db.getFixedCountersMask());
        Assert.assertEquals(0x3FCL, db.getConfigurableCountersMask());

        Assert.assertEquals(63, db.getAllEvents().size());

        checkEvent(db, "FIXED_CYCLES", true, 1L, "No description", null);
        checkEvent(db, "FIXED_INSTRUCTIONS", true, 2L, "No description", "INST_ALL");
        checkEvent(db, "L1D_CACHE_MISS_ST", false, 1020L,
                "Stores that missed the L1 Data Cache", null);
        checkEvent(db, "INST_BRANCH_COND", false, 224L,
                "Retired conditional branch instructions (counts only B.cond)", null);

        XCTraceSupport.PerfEventInfo instr = db.getEvent("Instructions");
        Assert.assertNotNull(instr);
        Assert.assertEquals("FIXED_INSTRUCTIONS", instr.getName());

        XCTraceSupport.PerfEventInfo cycles = db.getEvent("Cycles");
        Assert.assertNotNull(cycles);
        Assert.assertEquals("FIXED_CYCLES", cycles.getName());
    }
}
