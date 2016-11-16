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
package org.openjdk.jmh.runner;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Optional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for BenchmarkList
 */
public class TestBenchmarkList {

    private static BenchmarkList list;
    private static OutputFormat out;

    private static void stub(StringBuilder sb, String userClassQName, String generatedClassQName, String method, Mode mode) {
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

        sb.append(br.toLine());
        sb.append(String.format("%n"));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        StringBuilder sb = new StringBuilder();

        stub(sb,
                "oracle.micro.benchmarks.api.java.util.concurrent.NormalMaps",
                "oracle.micro.benchmarks.api.java.util.concurrent.generated.NormalMaps",
                "testConcurrentHashMap",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.jbb05.GeneratedSPECjbb2005HashMap",
                "oracle.micro.benchmarks.app.jbb05.generated.GeneratedSPECjbb2005HashMap",
                "jbb2005HashMapGetIntThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.jbb05.GeneratedSPECjbb2005HashMap",
                "oracle.micro.benchmarks.app.jbb05.generated.GeneratedSPECjbb2005HashMap",
                "jbb2005HashMapGetIntGCThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.jbb05.GeneratedSPECjbb2005HashMap",
                "oracle.micro.benchmarks.app.jbb05.generated.GeneratedSPECjbb2005HashMap",
                "jbb2005HashMapGetIntegerThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.jbb05.GeneratedSPECjbb2005HashMap",
                "oracle.micro.benchmarks.app.jbb05.generated.GeneratedSPECjbb2005HashMap",
                "jbb2005ResizedHashMapGetIntThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.jbb05.GeneratedSPECjbb2005HashMap",
                "oracle.micro.benchmarks.app.jbb05.generated.GeneratedSPECjbb2005HashMap",
                "jbb2005ResizedHashMapGetIntegerThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.sjent.GeneratedPrintBase64",
                "oracle.micro.benchmarks.app.sjent.generated.GeneratedPrintBase64",
                "printBase64Binary_128bytesThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.sjent.GeneratedPrintBase64",
                "oracle.micro.benchmarks.app.sjent.generated.GeneratedPrintBase64",
                "printBase64Binary_32bytesThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.app.sjent.GeneratedPrintBase64",
                "oracle.micro.benchmarks.app.sjent.generated.GeneratedPrintBase64",
                "printBase64Binary_512bytesThroughput",
                Mode.AverageTime);

        stub(sb,
                "oracle.micro.benchmarks.api.java.util.concurrent.GeneratedMaps",
                "oracle.micro.benchmarks.api.java.util.concurrent.generated.GeneratedMaps",
                "testConcurrentHashMap",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "dummy",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "dummyWarmThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "dummyWarmOnlyThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "dummySetupPayloadThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "boom_ExceptionThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "boom_ErrorThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "boom_ThrowableThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestMicro",
                "org.openjdk.jmh.runner.generated.TestMicro",
                "blackholedThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestBrokenMicro",
                "org.openjdk.jmh.runner.generated.TestBrokenMicro",
                "dummyPayloadThroughput",
                Mode.AverageTime);

        stub(sb,
                "org.openjdk.jmh.runner.TestExceptionThrowingMicro",
                "org.openjdk.jmh.runner.generated.TestExceptionThrowingMicro",
                "ouchThroughput",
                Mode.AverageTime);

        list = BenchmarkList.fromString(sb.toString());
        out = OutputFormatFactory.createFormatInstance(System.out, VerboseMode.NORMAL);
    }

    @Test
    public void testListGetNothing() throws Exception {
        // make sure we get nothing
        List<String> excludes = Collections.singletonList(".*");
        Set<BenchmarkListEntry> micros = list.getAll(out, excludes);
        assertEquals(0, micros.size());
    }

    @Test
    public void testListGetAll() throws Exception {
        // make sure we get em all
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.getAll(out, excludes);
        assertEquals(20, micros.size());
    }

    @Test
    public void testListFindSingleByPattern() throws Exception {
        // check find without excludes
        List<String> includes = Collections.singletonList(".*Hash.*");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(7, micros.size());
    }

    @Test
    public void testListFindSingleBySubstring() throws Exception {
        // check find without excludes
        List<String> includes = Collections.singletonList("Hash");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(7, micros.size());
    }

    @Test
    public void testListFindSingleByTypical() throws Exception {
        // check find without excludes
        // this would be a typical partial pattern with . abuse case
        List<String> includes = Collections.singletonList("jbb05.GeneratedSPECjbb2005HashMap");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(5, micros.size());
    }

    @Test
    public void testListFindAnchored() throws Exception {
        // check find without excludes
        // matches only: org.openjdk.jmh.runner.TestMicro.dummy
        List<String> includes = Collections.singletonList("^org\\.openjdk.*\\.dummy$");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(1, micros.size());
    }

    @Test
    public void testListFindSingleWithExcludes() throws Exception {
        // check find with excludes
        List<String> includes = Collections.singletonList(".*Hash.*");
        List<String> excludes = Collections.singletonList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(2, micros.size());
    }

    @Test
    public void testListFindAllWithSubstringExclude() throws Exception {
        // check find with excludes
        List<String> includes = Collections.singletonList("");
        List<String> excludes = Collections.singletonList("oracle");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(10, micros.size());
    }

    @Test
    public void testListFindAllWithEmpty() throws Exception {
        List<String> includes = Collections.emptyList();
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(20, micros.size());
    }

    @Test
    public void testListFindIncludeList() throws Exception {
        // check find with excludes
        List<String> includes = Arrays.asList("^oracle", ".*openjmh.*");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(10, micros.size());
    }

    @Test
    public void testListFindWithIncludesAndExcludes() throws Exception {
        List<String> includes = Collections.singletonList(".*Concurrent.*");
        List<String> excludes = Collections.singletonList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(2, micros.size());
    }

    @Test
    public void testListIsSorted() throws Exception {
        // micros should be sorted
        List<String> includes = Collections.singletonList(".*Hash.*");
        List<String> excludes = Collections.singletonList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        BenchmarkListEntry first = micros.iterator().next();
        assertTrue("oracle.micro.benchmarks.api.java.util.concurrent.GeneratedMaps.testConcurrentHashMap".equals(first.getUsername()));
    }
}
