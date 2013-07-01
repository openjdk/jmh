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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class GCProfiler implements Profiler {

    private long startTime = -1;
    private long startGCCount = -1;
    private long startGCTime = -1;
    private final String name;
    private final boolean verbose;

    public GCProfiler(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    @Override
    public void startProfile() {
        this.startTime = System.currentTimeMillis();
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
    public ProfilerResult endProfile() {
        long endTime = System.currentTimeMillis();
        long gcTime = -startGCTime;
        long gcCount = -startGCCount;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        if (verbose || (gcTime > 0 || gcCount > 0)) {
            return new GCProfilerResult(name, endTime - startTime, gcCount, gcTime);
        } else {
            return new EmptyResult();
        }
    }

    public static boolean isSupported() {
        return true;
    }

    static class GCProfilerResult implements ProfilerResult {

        private final String name;
        private final long profileIntervalInMillis;
        private final long gcCount;
        private final long gcTime;

        public GCProfilerResult(String name, long profileIntervalInMillis, long gcCount, long gcTime) {
            this.name = name;
            this.profileIntervalInMillis = profileIntervalInMillis;
            this.gcCount = gcCount;
            this.gcTime = gcTime;
        }

        @Override
        public String getProfilerName() {
            return name;
        }

        @Override
        public boolean hasData() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("wall time = %.3f secs,  GC time = %.3f secs, GC%% = %.2f%%, GC count = %+d",
                    profileIntervalInMillis / 1000.0, gcTime / 1000.0, ((double) gcTime / (double) profileIntervalInMillis * 100.0), gcCount);
        }
    }
}
