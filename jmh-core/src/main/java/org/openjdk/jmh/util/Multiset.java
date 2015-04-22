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
 * Basic Multiset.
 *
 * (Transitional interface)
 *
 * @param <T> element type
 */
public interface Multiset<T> {

    /**
     * Add the element to the multiset
     * @param element element to add
     */
    void add(T element);

    /**
     * Add the element to the multiset
     * @param element element to add
     * @param count number of elements to add
     */
    void add(T element, long count);

    /**
     * Count the elements in multiset
     * @param element element
     * @return number of matching elements in the set; zero, if no elements
     */
    long count(T element);

    /**
     * Get all associations of the multiset.
     * Each entry provides a key and a count of that element.
     * @return entry set of the multiset
     */
    Collection<Map.Entry<T, Long>> entrySet();

    /**
     * Answers if Multiset is empty
     * @return true, if set is empty
     */
    boolean isEmpty();

    /**
     * Answers the size of multiset.
     * Equivalent to number of elements, counting duplications.
     *
     * @return number of elements
     */
    long size();

    /**
     * Answers the collection of keys
     * @return the collections of keys
     */
    Collection<T> keys();
}
