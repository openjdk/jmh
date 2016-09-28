/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.infra.ThreadParams;

import java.util.List;

public class DistributeGroupsTest {

    @Test
    public void test1() {
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(1, new int[]{1});

        Assert.assertEquals(1, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(1, controls.get(0).getThreadCount());
        Assert.assertEquals(0, controls.get(0).getThreadIndex());

        // one group with a single thread
        Assert.assertEquals(1, controls.get(0).getGroupCount());
        Assert.assertEquals(0, controls.get(0).getGroupIndex());

        // the thread is alone in the group
        Assert.assertEquals(1, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());

        // no subgroups in the group
        Assert.assertEquals(1, controls.get(0).getSubgroupCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupIndex());

        // the thread is alone in the subgroup
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupThreadIndex());
    }

    @Test
    public void test2() {
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(2, new int[]{1});

        Assert.assertEquals(2, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(2, controls.get(0).getThreadCount());
        Assert.assertEquals(2, controls.get(1).getThreadCount());
        Assert.assertEquals(0, controls.get(0).getThreadIndex());
        Assert.assertEquals(1, controls.get(1).getThreadIndex());

        // two groups, each for a single thread
        Assert.assertEquals(2, controls.get(0).getGroupCount());
        Assert.assertEquals(2, controls.get(1).getGroupCount());
        Assert.assertEquals(0, controls.get(0).getGroupIndex());
        Assert.assertEquals(1, controls.get(1).getGroupIndex());

        // each thread is alone in the group
        Assert.assertEquals(1, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(1, controls.get(1).getGroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getGroupThreadIndex());

        // no subgroups in the group
        Assert.assertEquals(1, controls.get(0).getSubgroupCount());
        Assert.assertEquals(1, controls.get(1).getSubgroupCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupIndex());
        Assert.assertEquals(0, controls.get(1).getSubgroupIndex());

        // each thread is alone in the subgroup
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(1, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getSubgroupThreadIndex());
    }

    @Test
    public void test3() {
        // First "subgroup" is ignored
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(2, new int[]{0, 1});

        Assert.assertEquals(2, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(2, controls.get(0).getThreadCount());
        Assert.assertEquals(2, controls.get(1).getThreadCount());
        Assert.assertEquals(0, controls.get(0).getThreadIndex());
        Assert.assertEquals(1, controls.get(1).getThreadIndex());

        // two groups, each for a single thread
        Assert.assertEquals(2, controls.get(0).getGroupCount());
        Assert.assertEquals(2, controls.get(1).getGroupCount());
        Assert.assertEquals(0, controls.get(0).getGroupIndex());
        Assert.assertEquals(1, controls.get(1).getGroupIndex());

        // each thread is alone in the group
        Assert.assertEquals(1, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(1, controls.get(1).getGroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getGroupThreadIndex());

        // two subgroups, but first is not populated
        Assert.assertEquals(2, controls.get(0).getSubgroupCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupCount());
        Assert.assertEquals(1, controls.get(0).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(1).getSubgroupIndex());

        // each thread is alone in the subgroup
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(1, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getSubgroupThreadIndex());
    }

    @Test
    public void test4() {
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(2, new int[]{1, 1});

        Assert.assertEquals(2, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(2, controls.get(0).getThreadCount());
        Assert.assertEquals(2, controls.get(1).getThreadCount());
        Assert.assertEquals(0, controls.get(0).getThreadIndex());
        Assert.assertEquals(1, controls.get(1).getThreadIndex());

        // one group only
        Assert.assertEquals(1, controls.get(0).getGroupCount());
        Assert.assertEquals(1, controls.get(1).getGroupCount());
        Assert.assertEquals(0, controls.get(0).getGroupIndex());
        Assert.assertEquals(0, controls.get(1).getGroupIndex());

        // both threads share the group
        Assert.assertEquals(2, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(2, controls.get(1).getGroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());
        Assert.assertEquals(1, controls.get(1).getGroupThreadIndex());

        // two subgroups, threads in different subgroups
        Assert.assertEquals(2, controls.get(0).getSubgroupCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(1).getSubgroupIndex());

        // each subgroup has a single thread
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(1, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getSubgroupThreadIndex());

    }

    @Test
    public void test5() {
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(3, new int[]{1, 2});

        Assert.assertEquals(3, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(3, controls.get(0).getThreadCount());
        Assert.assertEquals(3, controls.get(1).getThreadCount());
        Assert.assertEquals(3, controls.get(2).getThreadCount());
        Assert.assertEquals(0, controls.get(0).getThreadIndex());
        Assert.assertEquals(1, controls.get(1).getThreadIndex());
        Assert.assertEquals(2, controls.get(2).getThreadIndex());

        // one group only
        Assert.assertEquals(1, controls.get(0).getGroupCount());
        Assert.assertEquals(1, controls.get(1).getGroupCount());
        Assert.assertEquals(1, controls.get(2).getGroupCount());
        Assert.assertEquals(0, controls.get(0).getGroupIndex());
        Assert.assertEquals(0, controls.get(1).getGroupIndex());
        Assert.assertEquals(0, controls.get(2).getGroupIndex());

        // all threads share the group
        Assert.assertEquals(3, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(1).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(2).getGroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());
        Assert.assertEquals(1, controls.get(1).getGroupThreadIndex());
        Assert.assertEquals(2, controls.get(2).getGroupThreadIndex());

        // two subgroups, first thread in distinct subgroup
        Assert.assertEquals(2, controls.get(0).getSubgroupCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupCount());
        Assert.assertEquals(2, controls.get(2).getSubgroupCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(1).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(2).getSubgroupIndex());

        // first subgroup has a single thread, second has two threads
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(2).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(0).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(1).getSubgroupThreadIndex());
        Assert.assertEquals(1, controls.get(2).getSubgroupThreadIndex());

    }

    @Test
    public void test6() {
        List<ThreadParams> controls = BenchmarkHandler.distributeThreads(6, new int[]{1, 2});

        Assert.assertEquals(6, controls.size());

        // threads agree on thread count, and enumerated
        Assert.assertEquals(6, controls.get(0).getThreadCount());
        Assert.assertEquals(6, controls.get(1).getThreadCount());
        Assert.assertEquals(6, controls.get(2).getThreadCount());
        Assert.assertEquals(6, controls.get(3).getThreadCount());
        Assert.assertEquals(6, controls.get(4).getThreadCount());
        Assert.assertEquals(6, controls.get(5).getThreadCount());

        Assert.assertEquals(0, controls.get(0).getThreadIndex());
        Assert.assertEquals(1, controls.get(1).getThreadIndex());
        Assert.assertEquals(2, controls.get(2).getThreadIndex());
        Assert.assertEquals(3, controls.get(3).getThreadIndex());
        Assert.assertEquals(4, controls.get(4).getThreadIndex());
        Assert.assertEquals(5, controls.get(5).getThreadIndex());

        // two groups
        Assert.assertEquals(2, controls.get(0).getGroupCount());
        Assert.assertEquals(2, controls.get(1).getGroupCount());
        Assert.assertEquals(2, controls.get(2).getGroupCount());
        Assert.assertEquals(2, controls.get(3).getGroupCount());
        Assert.assertEquals(2, controls.get(4).getGroupCount());
        Assert.assertEquals(2, controls.get(5).getGroupCount());

        Assert.assertEquals(0, controls.get(0).getGroupIndex());
        Assert.assertEquals(0, controls.get(1).getGroupIndex());
        Assert.assertEquals(0, controls.get(2).getGroupIndex());
        Assert.assertEquals(1, controls.get(3).getGroupIndex());
        Assert.assertEquals(1, controls.get(4).getGroupIndex());
        Assert.assertEquals(1, controls.get(5).getGroupIndex());

        // two groups, three threads each
        Assert.assertEquals(3, controls.get(0).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(1).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(2).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(3).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(4).getGroupThreadCount());
        Assert.assertEquals(3, controls.get(5).getGroupThreadCount());

        Assert.assertEquals(0, controls.get(0).getGroupThreadIndex());
        Assert.assertEquals(1, controls.get(1).getGroupThreadIndex());
        Assert.assertEquals(2, controls.get(2).getGroupThreadIndex());
        Assert.assertEquals(0, controls.get(3).getGroupThreadIndex());
        Assert.assertEquals(1, controls.get(4).getGroupThreadIndex());
        Assert.assertEquals(2, controls.get(5).getGroupThreadIndex());

        // two subgroups: first subgroup has a single thread, second has two threads
        Assert.assertEquals(2, controls.get(0).getSubgroupCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupCount());
        Assert.assertEquals(2, controls.get(2).getSubgroupCount());
        Assert.assertEquals(2, controls.get(3).getSubgroupCount());
        Assert.assertEquals(2, controls.get(4).getSubgroupCount());
        Assert.assertEquals(2, controls.get(5).getSubgroupCount());

        Assert.assertEquals(0, controls.get(0).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(1).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(2).getSubgroupIndex());
        Assert.assertEquals(0, controls.get(3).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(4).getSubgroupIndex());
        Assert.assertEquals(1, controls.get(5).getSubgroupIndex());

        // first subgroup has a single thread, second has two threads
        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(2).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(3).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(4).getSubgroupThreadIndex());
        Assert.assertEquals(1, controls.get(5).getSubgroupThreadIndex());

        Assert.assertEquals(1, controls.get(0).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(1).getSubgroupThreadCount());
        Assert.assertEquals(2, controls.get(2).getSubgroupThreadCount());
        Assert.assertEquals(0, controls.get(3).getSubgroupThreadIndex());
        Assert.assertEquals(0, controls.get(4).getSubgroupThreadIndex());
        Assert.assertEquals(1, controls.get(5).getSubgroupThreadIndex());

    }

}
