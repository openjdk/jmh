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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

class ClassloaderProfiler implements Profiler {

    private long startTime = -1;
    private long loadedClasses = -1;
    private long unloadedClasses = -1;
    private final String name;
    private final boolean verbose;

    public ClassloaderProfiler(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    @Override
    public void startProfile() {
        this.startTime = System.currentTimeMillis();
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        try {
            loadedClasses = cl.getLoadedClassCount();
        } catch (UnsupportedOperationException e) {
            loadedClasses = -1;
        }
        try {
            unloadedClasses = cl.getUnloadedClassCount();
        } catch (UnsupportedOperationException e) {
            unloadedClasses = -1;
        }
    }

    @Override
    public ProfilerResult endProfile() {
        long endTime = System.currentTimeMillis();

        long loaded;
        long unloaded;
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        try {
            loaded = cl.getLoadedClassCount() - loadedClasses;
        } catch (UnsupportedOperationException e) {
            loaded = -1;
        }
        try {
            unloaded = cl.getUnloadedClassCount() - unloadedClasses;
        } catch (UnsupportedOperationException e) {
            unloaded = -1;
        }

        if (verbose || (loaded > 0 || unloaded > 0)) {
            return new ClassloaderProfilerResult(name, endTime - startTime, loaded, unloaded);
        } else {
            return new EmptyResult();
        }
    }

    public static boolean isSupported() {
        return true; // assume always available
    }

    public static class ClassloaderProfilerResult implements ProfilerResult {

        private final String name;
        private final long profileIntervalMsec;
        private final long loaded;
        private final long unloaded;

        public ClassloaderProfilerResult(String name, long profileIntervalMsec, long loaded, long unloaded) {
            this.name = name;
            this.profileIntervalMsec = profileIntervalMsec;
            this.loaded = loaded;
            this.unloaded = unloaded;
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
            return String.format("wall time = %.3f secs, loaded = %+d, unloaded = %+d",
                    profileIntervalMsec / 1000.0, loaded, unloaded
            );
        }
    }

}
