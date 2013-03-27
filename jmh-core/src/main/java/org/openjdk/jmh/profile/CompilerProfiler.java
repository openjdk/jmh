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
package org.openjdk.jmh.profile;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

public class CompilerProfiler implements Profiler {

    private long startTime = -1;
    private long startCompTime = -1;
    private final String name;
    private final boolean verbose;

    public CompilerProfiler(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    @Override
    public void startProfile() {
        this.startTime = System.currentTimeMillis();
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        try {
            startCompTime = comp.getTotalCompilationTime();
        } catch (UnsupportedOperationException e) {
            startCompTime = -1;
        }
    }

    @Override
    public ProfilerResult endProfile() {
        long endTime = System.currentTimeMillis();
        long compTime = -startCompTime;
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        try {
            compTime += comp.getTotalCompilationTime();
        } catch (UnsupportedOperationException e) {
            compTime = -1;
        }
        if (verbose || (compTime > 0)) {
            return new CompProfilerResult(name, endTime - startTime, compTime, comp.getName());
        } else {
            return new EmptyResult();
        }
    }

    public static boolean isSupported() {
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        return comp.isCompilationTimeMonitoringSupported();
    }

    static class CompProfilerResult implements ProfilerResult {

        private final long profileIntervalInMillis;
        private final long compilationTime;
        private final String compilerName;
        private final String name;

        public CompProfilerResult(String name, long profileIntervalInMillis, long compilationTime, String compilerName) {
            this.name = name;
            this.profileIntervalInMillis = profileIntervalInMillis;
            this.compilationTime = compilationTime;
            this.compilerName = compilerName;
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
            return String.format("wall time = %.3f secs, JIT time = %.3f secs (%s)",
                    profileIntervalInMillis / 1000.0, compilationTime / 1000.0,
                    compilerName);
        }
    }

}
