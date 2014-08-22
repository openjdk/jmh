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
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for BenchmarkList
 */
public class TestBenchmarkList {

    private static BenchmarkList list;
    private static OutputFormat out;

    @BeforeClass
    public static void setUpClass() throws Exception {
        list = BenchmarkList.fromResource("/org/openjdk/jmh/runner/MicroBenchmarks");
        out = OutputFormatFactory.createFormatInstance(System.out, VerboseMode.NORMAL);
    }

    @Test
    public void testListGetNothing() throws Exception {
        // make sure we get nothing
        List<String> excludes = Arrays.asList(".*");
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
        List<String> includes = Arrays.asList(".*Hash.*");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(7, micros.size());
    }

    @Test
    public void testListFindSingleBySubstring() throws Exception {
        // check find without excludes
        List<String> includes = Arrays.asList("Hash");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(7, micros.size());
    }

    @Test
    public void testListFindSingleByTypical() throws Exception {
        // check find without excludes
        // this would be a typical partial pattern with . abuse case
        List<String> includes = Arrays.asList("jbb05.GeneratedSPECjbb2005HashMap");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(5, micros.size());
    }

    @Test
    public void testListFindAnchored() throws Exception {
        // check find without excludes
        // matches only: org.openjdk.jmh.runner.TestMicro.dummy
        List<String> includes = Arrays.asList("^org\\.openjdk.*\\.dummy$");
        List<String> excludes = Collections.emptyList();
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(1, micros.size());
    }

    @Test
    public void testListFindSingleWithExcludes() throws Exception {
        // check find with excludes
        List<String> includes = Arrays.asList(".*Hash.*");
        List<String> excludes = Arrays.asList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(2, micros.size());
    }

    @Test
    public void testListFindAllWithSubstringExclude() throws Exception {
        // check find with excludes
        List<String> includes = Arrays.asList("");
        List<String> excludes = Arrays.asList("oracle");
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
        List<String> includes = Arrays.asList(".*Concurrent.*");
        List<String> excludes = Arrays.asList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        assertEquals(2, micros.size());
    }

    @Test
    public void testListIsSorted() throws Exception {
        // micros should be sorted
        List<String> includes = Arrays.asList(".*Hash.*");
        List<String> excludes = Arrays.asList(".*Int.*");
        Set<BenchmarkListEntry> micros = list.find(out, includes, excludes);
        BenchmarkListEntry first = micros.iterator().next();
        assertTrue("oracle.micro.benchmarks.api.java.util.concurrent.GeneratedMaps.testConcurrentHashMap".equals(first.getUsername()));
    }
}
