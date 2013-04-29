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

import java.util.ArrayList;
import java.util.List;

public class ProfilerFactory {

    public enum Profilers {

        GC {
            @Override
            public Profiler createInstance(boolean verbose) {
                return new GCProfiler(label(), verbose);
            }

            @Override
            public boolean isSupported() {
                return GCProfiler.isSupported();
            }

            @Override
            public String id() {
                return "gc";
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
            public Profiler createInstance(boolean verbose) {
                return new CompilerProfiler(label(), verbose);
            }

            @Override
            public boolean isSupported() {
                return CompilerProfiler.isSupported();
            }

            @Override
            public String id() {
                return "comp";
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
            public Profiler createInstance(boolean verbose) {
                return new ClassloaderProfiler(label(), verbose);
            }

            @Override
            public boolean isSupported() {
                return ClassloaderProfiler.isSupported();
            }

            @Override
            public String id() {
                return "cl";
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
        HS_RUNTIME {
            @Override
            public String id() {
                return "hs_rt";
            }

            @Override
            public String label() {
                return "HS(RT)";
            }

            @Override
            public Profiler createInstance(boolean verbose) {
                return new HotspotRuntimeProfiler(label(), verbose);
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
            public Profiler createInstance(boolean verbose) {
                return new HotspotClassloadingProfiler(label(), verbose);
            }

            @Override
            public boolean isSupported() {
                return HotspotClassloadingProfiler.isSupported();
            }

            @Override
            public String id() {
                return "hs_cl";
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
            public String id() {
                return "hs_comp";
            }

            @Override
            public String label() {
                return "HS(JIT)";
            }

            @Override
            public Profiler createInstance(boolean verbose) {
                return new HotspotCompilationProfiler(label(), verbose);
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
            public String id() {
                return "hs_gc";
            }

            @Override
            public String label() {
                return "HS(GC)";
            }

            @Override
            public Profiler createInstance(boolean verbose) {
                return new HotspotMemoryProfiler(label(), verbose);
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
        HS_THREAD {
            @Override
            public String id() {
                return "hs_thr";
            }

            @Override
            public String label() {
                return "HS(THR)";
            }

            @Override
            public Profiler createInstance(boolean verbose) {
                return new HotspotThreadProfiler(label(), verbose);
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
        STACK_PROFILER {
            @Override
            public String id() {
                return "stack";
            }

            @Override
            public String label() {
                return "Stack";
            }

            @Override
            public Profiler createInstance(boolean verbose) {
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

        public abstract Profiler createInstance(boolean verbose);

        public abstract boolean isSupported();

        /**
         * @return id to reference the profiler
         */
        public abstract String id();

        /**
         * @return label to pretty-print profiler name
         */
        public abstract String label();

        /**
         * @return longer description for profiler
         */
        public abstract String description();
    }


    public static Profilers getProfiler(String id) {
        for (Profilers p : Profilers.values()) {
            if (p.id().toLowerCase().equals(id.toLowerCase())) {
                return p;
            }
        }
        return null;
    }

    public static List<String> getAvailableProfilers() {
        List<String> res = new ArrayList<String>();
        for (Profilers p : Profilers.values()) {
            res.add(p.id().toLowerCase());
        }
        return res;
    }

    public static boolean isSupported(String id) {
        Profilers profiler = getProfiler(id);
        return (profiler != null) && profiler.isSupported();
    }

    public static String getDescription(String id) {
        Profilers profiler = getProfiler(id);
        if (profiler != null) {
            return profiler.description();
        }
        return "(Description is not available)";
    }

    public static List<String> getSupportedProfilers() {
        List<String> res = new ArrayList<String>();
        for (Profilers p : Profilers.values()) {
            if (p.isSupported()) {
                res.add(p.id().toLowerCase());
            }
        }
        return res;
    }
}
