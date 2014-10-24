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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HashsetMultimap<K, V> implements Multimap<K, V>, Serializable {
    private static final long serialVersionUID = -4236100656731956836L;

    private final Map<K, Collection<V>> map;

    public HashsetMultimap() {
        map = new HashMap<K, Collection<V>>();
    }

    @Override
    public void put(K key, V value) {
        Collection<V> vs = map.get(key);
        if (vs == null) {
            vs = new HashSet<V>();
            map.put(key, vs);
        }
        vs.add(value);
    }

    @Override
    public void putAll(K key, Collection<V> values) {
        Collection<V> vs = map.get(key);
        if (vs == null) {
            vs = new HashSet<V>();
            map.put(key, vs);
        }
        vs.addAll(values);
    }

    @Override
    public Collection<V> get(K key) {
        Collection<V> vs = map.get(key);
        return (vs == null) ? Collections.<V>emptyList() : Collections.unmodifiableCollection(vs);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Collection<K> keys() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = new ArrayList<V>();
        for (Collection<V> vs : map.values()) {
            result.addAll(vs);
        }
        return result;
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public void merge(Multimap<K, V> other) {
        for (K k : other.keys()) {
            putAll(k, other.get(k));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HashsetMultimap that = (HashsetMultimap) o;

        if (!map.equals(that.map)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
