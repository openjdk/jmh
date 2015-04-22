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


import java.util.Collection;
import java.util.Map;

/**
 * Basic Multimap.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Multimap<K, V> {

    /**
     * Put the element pair.
     *
     * @param key key
     * @param value value
     */
    void put(K key, V value);

    /**
     * Put multiple pairs.
     * @param k key
     * @param vs values
     */
    void putAll(K k, Collection<V> vs);

    /**
     * Get all values associated with the key
     * @param key key
     * @return collection of values
     */
    Collection<V> get(K key);

    /**
     * Get all associations of the multimap.
     * The method is intended for read-only view.
     * @return entry set of the multimap
     */
    Collection<Map.Entry<K, Collection<V>>> entrySet();

    /**
     * Checks if multimap is empty
     * @return true, if empty
     */
    boolean isEmpty();

    /**
     * Clears the multimap
     */
    void clear();

    /**
     * Keys in the map
     * @return collection of keys
     */
    Collection<K> keys();

    Collection<V> values();

    void remove(K key);

    void merge(Multimap<K, V> other);
}
