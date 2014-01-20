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

import org.openjdk.jmh.runner.options.VerboseMode;

public enum ProfilerType {

    GC {
        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new GCProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return GCProfiler.isSupported();
        }

        @Override
        public String label() {
            return "GC";
        }

        @Override
        public String description() {
            return "GC profiling via standard MBeans";
        }
    },
    COMP {
        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new CompilerProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return CompilerProfiler.isSupported();
        }

        @Override
        public String label() {
            return "JIT";
        }

        @Override
        public String description() {
            return "JIT compiler profiling via standard MBeans";
        }
    },
    CL {
        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new ClassloaderProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return ClassloaderProfiler.isSupported();
        }

        @Override
        public String label() {
            return "Class";
        }

        @Override
        public String description() {
            return "Classloader profiling via standard MBeans";
        }
    },
    HS_RT {

        @Override
        public String label() {
            return "HS(RT)";
        }

        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new HotspotRuntimeProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return HotspotRuntimeProfiler.isSupported();
        }

        @Override
        public String description() {
            return "HotSpot (tm) runtime profiling via implementation-specific MBeans";
        }
    },
    HS_CL {
        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new HotspotClassloadingProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return HotspotClassloadingProfiler.isSupported();
        }

        @Override
        public String label() {
            return "HS(Class)";
        }

        @Override
        public String description() {
            return "HotSpot (tm) classloader profiling via implementation-specific MBeans";
        }
    },
    HS_COMP {
        @Override
        public String label() {
            return "HS(JIT)";
        }

        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new HotspotCompilationProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return HotspotCompilationProfiler.isSupported();
        }

        @Override
        public String description() {
            return "HotSpot (tm) JIT compiler profiling via implementation-specific MBeans";
        }
    },
    HS_GC {
        @Override
        public String label() {
            return "HS(GC)";
        }

        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new HotspotMemoryProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return HotspotMemoryProfiler.isSupported();
        }

        @Override
        public String description() {
            return "HotSpot (tm) memory manager (GC) profiling via implementation-specific MBeans";
        }
    },
    HS_THR {

        @Override
        public String label() {
            return "HS(THR)";
        }

        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new HotspotThreadProfiler(label(), mode.equalsOrHigherThan(VerboseMode.EXTRA));
        }

        @Override
        public boolean isSupported() {
            return HotspotThreadProfiler.isSupported();
        }

        @Override
        public String description() {
            return "HotSpot (tm) threading subsystem via implementation-specific MBeans";
        }
    },
    STACK {
        @Override
        public String label() {
            return "Stack";
        }

        @Override
        public Profiler createInstance(VerboseMode mode) {
            return new StackProfiler(label());
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String description() {
            return "Simple and naive Java stack profiler";
        }
    };

    public abstract Profiler createInstance(VerboseMode mode);

    public abstract boolean isSupported();

    /**
     * @return id to reference the profiler
     */
    public final String id() {
        return this.toString().toLowerCase();
    }

    /**
     * @return label to pretty-print profiler name
     */
    public abstract String label();

    /**
     * @return longer description for profiler
     */
    public abstract String description();
}
