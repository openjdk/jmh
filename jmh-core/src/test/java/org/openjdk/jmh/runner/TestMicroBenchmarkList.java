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
import org.openjdk.jmh.output.OutputFormatFactory;
import org.openjdk.jmh.output.format.OutputFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for MicroBenchmarkList
 *
 * @author anders.astrand@oracle.com
 *
 */
public class TestMicroBenchmarkList {

    private static MicroBenchmarkList list;
    private static List<String> excludes;
    private static OutputFormat out;

    @BeforeClass
    public static void setUpClass() throws Exception {
        list = MicroBenchmarkList.fromResource("/org/openjdk/jmh/runner/MicroBenchmarks");
        excludes = new ArrayList<String>();
        out = OutputFormatFactory.createFormatInstance(false);
    }

    @Test
    public void testListGetNothing() throws Exception {
        // make sure we get nothing
        excludes.clear();
        excludes.add(".*");
        Set<BenchmarkRecord> micros = list.getAll(out, excludes);
        assertEquals(0, micros.size());
    }

    @Test
    public void testListGetAll() throws Exception {
        excludes.clear();
        // make sure we get em all
        Set<BenchmarkRecord> micros = list.getAll(out, excludes);
        assertEquals(20, micros.size());
    }

    @Test
    public void testListFindSingle() throws Exception {
        // check find without excldues
        excludes.clear();
        Set<BenchmarkRecord> micros = list.find(out, ".*Hash.*", excludes);
        assertEquals(7, micros.size());
    }

    @Test
    public void testListFindSingleWithExcludes() throws Exception {
        // check find with excludes
        excludes.clear();
        excludes.add(".*Int.*");
        Set<BenchmarkRecord> micros = list.find(out, ".*Hash.*", excludes);
        assertEquals(2, micros.size());
    }

    @Test
    public void testListIsSorted() throws Exception {
        // micros should be sorted
        excludes.clear();
        excludes.add(".*Int.*");
        Set<BenchmarkRecord> micros = list.find(out, ".*Hash.*", excludes);
        BenchmarkRecord first = micros.iterator().next();
        assertTrue("oracle.micro.benchmarks.api.java.util.concurrent.GeneratedMaps.testConcurrentHashMap".equals(first.getUsername()));
    }

    @Test
    public void testListGetWithIncludesAndExcludes() throws Exception {
        excludes.clear();
        excludes.add(".*Int.*");
        Set<BenchmarkRecord> micros = list.find(out, ".*Concurrent.*", excludes);
        assertEquals(2, micros.size());
    }
}
