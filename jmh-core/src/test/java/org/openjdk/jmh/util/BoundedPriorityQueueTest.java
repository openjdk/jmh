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

import java.util.Collections;
import java.util.Queue;

public class BoundedPriorityQueueTest {
    @Test
    public void top3Smallest() {
        Queue<Integer> queue = new BoundedPriorityQueue<>(3);
        queue.add(50);
        queue.add(40);
        queue.add(10);
        queue.add(20);
        queue.add(30);
        queue.add(30);
        queue.add(80);

        Assert.assertEquals(3, queue.size());
        Assert.assertEquals((Integer) 30, queue.poll());
        Assert.assertEquals((Integer) 20, queue.poll());
        Assert.assertEquals((Integer) 10, queue.poll());
    }

    @Test
    public void top3Largest() {
        Queue<Integer> queue = new BoundedPriorityQueue<>(3, Collections.reverseOrder());
        queue.add(50);
        queue.add(40);
        queue.add(10);
        queue.add(20);
        queue.add(30);
        queue.add(30);
        queue.add(80);

        Assert.assertEquals(3, queue.size());
        Assert.assertEquals((Integer) 40, queue.poll());
        Assert.assertEquals((Integer) 50, queue.poll());
        Assert.assertEquals((Integer) 80, queue.poll());
    }

}
