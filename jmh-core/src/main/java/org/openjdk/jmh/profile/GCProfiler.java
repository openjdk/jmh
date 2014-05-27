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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.util.internal.Optional;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class GCProfiler implements Profiler {
    private long startGCCount;
    private long startGCTime;
    private long startTime;

    @Override
    public InjectionPoint point() {
        return InjectionPoint.FORKED_VM_CONTROL;
    }

    @Override
    public Optional<List<String>> addJVMOptions() {
        return Optional.none();
    }

    @Override
    public void beforeTrial() {
        // do nothing
    }

    @Override
    public void beforeIteration() {
        this.startTime = System.nanoTime();
        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        this.startGCCount = gcCount;
        this.startGCTime = gcTime;
    }

    @Override
    public Collection<? extends Result> afterIteration() {
        long endTime = System.nanoTime();
        long gcTime = -startGCTime;
        long gcCount = -startGCCount;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        return Arrays.asList(
                new ProfilerResult("@gc.count", gcCount, "counts"),
                new ProfilerResult("@gc.time", 100.0 * gcTime / TimeUnit.NANOSECONDS.toMillis(endTime - startTime), "%")
        );
    }

    @Override
    public Collection<? extends Result> afterTrial() {
        return Collections.emptyList();
    }

    public static boolean isSupported() {
        return true; // always supported
    }
}
