/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.util;

import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

public class MultisetsTest {
    @Test
    public void testCountHighest() {
        Multiset<Integer> set = new HashMultiset<>();
        set.add(10);
        set.add(10);
        set.add(10);
        set.add(20);
        set.add(20);
        set.add(70);
        List<Integer> topInts = Multisets.countHighest(set, 2);

        Assert.assertEquals(2, topInts.size());
        Assert.assertEquals((Integer) 10, topInts.get(0));
        Assert.assertEquals((Integer) 20, topInts.get(1));
    }

    @Test
    public void testCountHighest_2() {
        // Regression test for CODETOOLS-7901411

        Multiset<String> set = new HashMultiset<>();
        set.add("Meh", 85);
        set.add("Blah", 17);
        set.add("Choo", 1);

        List<String> top = Multisets.countHighest(set, 3);

        Assert.assertEquals(3, top.size());
        Assert.assertEquals("Meh", top.get(0));
        Assert.assertEquals("Blah", top.get(1));
        Assert.assertEquals("Choo", top.get(2));
    }

    @Test
    public void testSortedDesc() {
        Multiset<Integer> set = new HashMultiset<>();
        set.add(10);
        set.add(10);
        set.add(10);
        set.add(20);
        set.add(20);
        set.add(70);
        List<Integer> topInts = Multisets.sortedDesc(set);

        Assert.assertEquals(3, topInts.size());
        Assert.assertEquals((Integer) 10, topInts.get(0));
        Assert.assertEquals((Integer) 20, topInts.get(1));
        Assert.assertEquals((Integer) 70, topInts.get(2));
    }
}
