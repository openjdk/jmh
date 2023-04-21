/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PerfAsmAddressTest {

    static final HashMap<String, List<Long>> TESTS = new HashMap<>();

    static long addr(String s) {
        return Long.parseLong(s, 16);
    }

    static {
        TESTS.put("0x00007f815c65724e:   test   %eax,(%r8)",
                Arrays.asList(addr("7f815c65724e")));

        TESTS.put("0x00007f815c657239:   je     0x00007f815c657290",
                Arrays.asList(addr("7f815c657239"), addr("7f815c657290")));

        TESTS.put("0x00007f815c657256:   movabs $0x7f8171798570,%r10",
                Arrays.asList(addr("7f815c657256"), addr("7f8171798570")));

        TESTS.put("0x0000ffff685c7d2c:   b   0x0000ffff685c7cf0",
                Arrays.asList(addr("ffff685c7d2c"), addr("ffff685c7cf0")));

        TESTS.put("0x0000ffff685c7d1c:   b.ne        0x0000ffff685c7cb4  // b.any",
                Arrays.asList(addr("ffff685c7d1c"), addr("ffff685c7cb4")));

        TESTS.put("0x0000ffff685c7d1c:   b.ne        0x0000ffff685c7cb4// b.any",
                Arrays.asList(addr("ffff685c7d1c"), addr("ffff685c7cb4")));

        TESTS.put("0x0000ffff685c7d1c:   b.ne        0x0000ffff685c7cb4;comment",
                Arrays.asList(addr("ffff685c7d1c"), addr("ffff685c7cb4")));

        TESTS.put("0x0000ffff685c7d1c:b.ne        0x0000ffff685c7cb4",
                Arrays.asList(addr("ffff685c7d1c"), addr("ffff685c7cb4")));

        TESTS.put("0x0000ffff685c7d1c: b.ne\t0x0000ffff685c7cb4",
                Arrays.asList(addr("ffff685c7d1c"), addr("ffff685c7cb4")));
    }

    @Test
    public void testNoPrefix() {
        for (String line : TESTS.keySet()) {
            List<Long> actual = AbstractPerfAsmProfiler.parseAddresses(line, true);
            List<Long> expected = TESTS.get(line);
            Assert.assertEquals(line, expected, actual);
        }
    }

    @Test
    public void testPrefix() {
        for (String line : TESTS.keySet()) {
            String testLine = "something " + line;
            Assert.assertEquals(new ArrayList<>(), AbstractPerfAsmProfiler.parseAddresses(testLine, true));

            List<Long> actual = AbstractPerfAsmProfiler.parseAddresses(testLine, false);
            List<Long> expected = TESTS.get(line);
            Assert.assertEquals(line, expected, actual);
        }
    }

}
