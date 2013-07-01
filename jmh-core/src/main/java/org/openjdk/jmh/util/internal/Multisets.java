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
package org.openjdk.jmh.util.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Multisets {

    public static <T> Collection<T> countHighest(Multiset<T> set, int top) {
        // crude and inefficient
        PriorityQueue<Pair<T, Integer>> q = new PriorityQueue<Pair<T, Integer>>(10, new Comparator<Pair<T, Integer>>() {
            @Override
            public int compare(Pair<T, Integer> o1, Pair<T, Integer> o2) {
                return o2.k2.compareTo(o1.k2);
            }
        });

        for (T key : set.keys()) {
            q.add(new Pair<T, Integer>(key, set.count(key)));
        }

        List<T> result = new ArrayList<T>();
        for (int t = 0; (t < top && !q.isEmpty()); t++) {
            result.add(q.poll().k1);
        }

        return result;
    }

    private static class Pair<K1, K2> {
        public final K1 k1;
        public final K2 k2;

        private Pair(K1 k1, K2 k2) {
            this.k1 = k1;
            this.k2 = k2;
        }
    }

}
