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
package org.openjdk.jmh.util;

import java.util.*;

public class Multisets {

    public static <T> List<T> countHighest(Multiset<T> set, int top) {
        Queue<Map.Entry<T, Long>> q = new BoundedPriorityQueue<>(top, Map.Entry.<T, Long>comparingByValue().reversed());

        q.addAll(set.entrySet());

        List<T> result = new ArrayList<>(q.size());

        Map.Entry<T, Long> pair;
        while ((pair = q.poll()) != null) {
            result.add(pair.getKey());
        }

        // BoundedPriorityQueue returns "smallest to largest", so we reverse the result
        Collections.reverse(result);

        return result;
    }

    public static <T> List<T> sortedDesc(final Multiset<T> set) {
        List<T> sorted = new ArrayList<>(set.keys());
        sorted.sort((o1, o2) -> Long.compare(set.count(o2), set.count(o1)));
        return sorted;
    }

}
