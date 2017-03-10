/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.runner;

import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestBenchmarkListEncoding {

    private static BenchmarkListEntry stub(String userClassQName, String generatedClassQName, String method, Mode mode) {
        BenchmarkListEntry br = new BenchmarkListEntry(
                userClassQName,
                generatedClassQName,
                method,
                mode,
                Optional.<Integer>none(),
                new int[]{1},
                Optional.<Collection<String>>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<String>none(),
                Optional.<Collection<String>>none(),
                Optional.<Collection<String>>none(),
                Optional.<Collection<String>>none(),
                Optional.<Map<String, String[]>>none(),
                Optional.<TimeUnit>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none()
        );
        return br;
    }

    @Test
    public void test_ASCII_UTF8() throws Exception {
        testWith("ASCII", "UTF-8");
    }

    @Test
    public void test_UTF8_ASCII() throws Exception {
        testWith("UTF-8", "ASCII");
    }

    @Test
    public void test_UTF8_UTF8() throws Exception {
        testWith("UTF-8", "UTF-8");
    }

    @Test
    public void test_ASCII_ASCII() throws Exception {
        testWith("ASCII", "ASCII");
    }

    public void testWith(String src, String dst) throws IOException {
        BenchmarkListEntry br = stub("something.Test",
                "something.generated.Test",
                "testКонкаррентХэшмап",
                Mode.AverageTime);

        resetCharset();
        System.setProperty("file.encoding", src);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BenchmarkList.writeBenchmarkList(bos, Collections.singleton(br));

        resetCharset();
        System.setProperty("file.encoding", dst);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Collection<BenchmarkListEntry> read = BenchmarkList.readBenchmarkList(bis);
        BenchmarkListEntry first = read.iterator().next();
        assertEquals("something.Test.testКонкаррентХэшмап", first.getUsername());
    }

    private void resetCharset() {
        try {
            Field f = Charset.class.getDeclaredField("defaultCharset");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            // okay then.
        }
    }
}
