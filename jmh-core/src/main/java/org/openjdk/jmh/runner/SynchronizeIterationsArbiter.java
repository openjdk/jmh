/**
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
package org.openjdk.jmh.runner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizeIterationsArbiter implements Arbiter {

    private final int numThreads;

    private volatile int version;

    private volatile boolean shouldWait;

    private final boolean[] ready;

    private final AtomicInteger indexCounter = new AtomicInteger();

    private final Map<Thread, Integer> indexes = new ConcurrentHashMap<Thread, Integer>();

    public SynchronizeIterationsArbiter(int numThreads) {
        this.numThreads = numThreads;
        this.ready = new boolean[numThreads];
        this.shouldWait = true;
    }

    @Override
    public void announceReady() {
        Thread thread = Thread.currentThread();
        Integer index = indexes.get(thread);
        if (index == null) {
            index = indexCounter.getAndIncrement();
            if (index >= numThreads) {
                throw new IllegalStateException("More threads than expected");
            }
            indexes.put(thread, index);
        }

        if (!ready[index]) {
            ready[index] = true;
            version++; // publish
        }

        if (version > 0) {
            boolean allSet = true;
            for (boolean b : ready) {
                allSet &= b;
            }
            if (allSet) {
                shouldWait = false;
            }
        }
    }

    @Override
    public boolean shouldWait() {
        return shouldWait;
    }

}
