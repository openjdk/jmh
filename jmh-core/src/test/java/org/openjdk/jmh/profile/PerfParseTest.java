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

public class PerfParseTest {

    @Test
    public void parsePerf_4_4() {
        String[] lines = new String[] {
                "328650.667569: instructions:      7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions:      7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/with spaces/libjvm.so)"
        };

        for (String line : lines) {
            LinuxPerfAsmProfiler.PerfLine perfLine = LinuxPerfAsmProfiler.parsePerfLine(line);
            Assert.assertEquals(328650.667569D, perfLine.time());
            Assert.assertEquals("instructions", perfLine.eventName());
            Assert.assertEquals(0x7f82b6a8beb4L, perfLine.addr());
            Assert.assertEquals("ConstantPoolCache::allocate", perfLine.symbol());
            Assert.assertEquals("libjvm.so", perfLine.lib());
        }
    }

    @Test
    public void parseRaggedSymbols() {
        String[] lines = new String[] {
                "328650.667569: instructions:      7f82b6a8beb4 ConstantPoolCache::allocate(Thread* thr) (/somewhere/on/my/filesystem/libjvm.so)",
        };

        for (String line : lines) {
            LinuxPerfAsmProfiler.PerfLine perfLine = LinuxPerfAsmProfiler.parsePerfLine(line);
            Assert.assertEquals(328650.667569D, perfLine.time());
            Assert.assertEquals("instructions", perfLine.eventName());
            Assert.assertEquals(0x7f82b6a8beb4L, perfLine.addr());
            Assert.assertEquals("ConstantPoolCache::allocate(Thread* thr)", perfLine.symbol());
            Assert.assertEquals("libjvm.so", perfLine.lib());
        }
    }

    @Test
    public void parseOptionalTag() {
        String[] lines = new String[] {
                "328650.667569: instructions:u:      7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions:uk:     7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions:k:      7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions:HG:     7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions:H:      7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)",
                "328650.667569: instructions::       7f82b6a8beb4 ConstantPoolCache::allocate (/somewhere/on/my/filesystem/libjvm.so)"
        };
        for (String line : lines) {
            LinuxPerfAsmProfiler.PerfLine perfLine = LinuxPerfAsmProfiler.parsePerfLine(line);

            Assert.assertEquals(328650.667569D, perfLine.time());
            Assert.assertEquals("instructions", perfLine.eventName());
            Assert.assertEquals(0x7f82b6a8beb4L, perfLine.addr());
            Assert.assertEquals("ConstantPoolCache::allocate", perfLine.symbol());
            Assert.assertEquals("libjvm.so", perfLine.lib());
        }
    }


}
