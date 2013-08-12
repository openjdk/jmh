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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for map processing
 *
 * @author sergey.kuksenko@oracle.com
 */
public class CollectionUtils {

    private CollectionUtils(){
        // no instances
    }

    public static <K, V> Map<K, V> createTreeMapIfNull(Map<K, V> map) {
        if (map == null) {
            map = new TreeMap<K, V>();
        }
        return map;
    }

    public static <K, V> Map<K, V> createHashMapIfNull(Map<K, V> map) {
        if (map == null) {
            map = new HashMap<K, V>();
        }
        return map;
    }

    public static <K, V> Map<K, V> putIfAbsent(Map<K, V> map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    public static <K, V> Map<K, V> putAndCreateTreeMapIfAbsent(Map<K, V> map, K key, V value) {
        if (map == null) {
            map = new TreeMap<K, V>();
            map.put(key, value);
        } else if (!map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    public static <K, V> Map<K, V> conditionalPutAndCreateTreeMapIfAbsent(Map<K, V> map, boolean cond, K key, V value) {
        if (cond) {
            if (map == null) {
                map = new TreeMap<K, V>();
                map.put(key, value);
            } else if (!map.containsKey(key)) {
                map.put(key, value);
            }
        }
        return map;
    }

    public static <K> Map<K, String> conditionalPutAndCreateTreeMapIfAbsentAndQuote(Map<K, String> map, boolean cond, K key, String value) {
        if (cond) {
            if (map == null) {
                map = new TreeMap<K, String>();
                map.put(key, "\"" + value + "\"");
            } else if (!map.containsKey(key)) {
                map.put(key, "\"" + value + "\"");
            }
        }
        return map;
    }

    public static <T> List<T> addIfNotNull(List<T> list, T value) {
        if (value != null) {
            list.add(value);
        }
        return list;
    }
}
