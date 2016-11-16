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

import java.io.Serializable;
import java.util.*;

/**
 * Bounded variant of {@link PriorityQueue}.
 * Note: elements are returned in reverse order.
 * For instance, if "top N smallest elements" are required, use {@code new BoundedPriorityQueue(N)},
 * and the elements would be returned in largest to smallest order.
 *
 * @param <E> type of the element
 */
public class BoundedPriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private static final long serialVersionUID = 7159618773497127413L;

    private final int maxSize;
    private final Comparator<? super E> comparator;
    private final Queue<E> queue;

    /**
     * Creates a bounded priority queue with the specified maximum size and default ordering.
     * At most {@code maxSize} smallest elements would be kept in the queue.
     *
     * @param maxSize maximum size of the queue
     */
    public BoundedPriorityQueue(int maxSize) {
        this(maxSize, null);
    }

    /**
     * Creates a bounded priority queue with the specified maximum size.
     * At most {@code maxSize} smallest elements would be kept in the queue.
     *
     * @param maxSize    maximum size of the queue
     * @param comparator comparator that orders the elements
     */
    public BoundedPriorityQueue(int maxSize, Comparator<? super E> comparator) {
        this.maxSize = maxSize;
        this.comparator = reverse(comparator);
        this.queue = new PriorityQueue<>(10, this.comparator);
    }

    /**
     * Internal queue should be in fact in reverse order.
     * By default, the queue aims for "top N smallest elements".
     * So peek() should return the biggest element, so it can be removed when adding smaller element
     *
     * @param comparator comparator that designates ordering of the entries or {@code null} for default ordering
     * @param <E>        type of the element
     * @return reverse comparator
     */
    private static <E> Comparator<? super E> reverse(Comparator<? super E> comparator) {
        if (comparator == null) {
            return Collections.reverseOrder();
        }
        return Collections.reverseOrder(comparator);
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        if (queue.size() >= maxSize) {
            E head = peek();
            if (comparator.compare(e, head) < 1) {
                return false;
            }
            poll();
        }
        return queue.offer(e);
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
