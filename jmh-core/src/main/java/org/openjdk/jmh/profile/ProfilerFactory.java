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

import org.openjdk.jmh.runner.options.ProfilerConfig;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ProfilerFactory {

    public static Profiler getProfilerOrException(ProfilerConfig cfg) throws ProfilerException {
        try {
            return getProfiler(cfg);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ProfilerException) {
                throw (ProfilerException) e.getCause();
            }
            throw new ProfilerException(e);
        } catch (ProfilerException e) {
            throw e;
        } catch (Exception e) {
            throw new ProfilerException(e);
        }
    }

    private static Profiler getProfilerOrNull(ProfilerConfig cfg) {
        try {
            return getProfiler(cfg);
        } catch (Exception e) {
            return null;
        }
    }

    private static Profiler getProfiler(ProfilerConfig cfg) throws Exception {
        String desc = cfg.getKlass();

        // Try built-in profilers first
        Class<? extends Profiler> builtIn = BUILT_IN.get(desc);
        if (builtIn != null) {
            return instantiate(cfg, builtIn);
        }

        // Try discovered profilers then
        Collection<Class<? extends Profiler>> profilers = getDiscoveredProfilers();
        for (Class<? extends Profiler> p : profilers) {
            if (p.getCanonicalName().equals(desc)) {
                return instantiate(cfg, p);
            }
        }

        // Try the direct hit
        Class<? extends Profiler> klass = (Class<? extends Profiler>) Class.forName(desc);
        return instantiate(cfg, klass);
    }

    private static Profiler instantiate(ProfilerConfig cfg, Class<? extends Profiler> p) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            return p.getConstructor(String.class).newInstance(cfg.getOpts());
        } catch (NoSuchMethodException e) {
            // fallthrough
        }

        try {
            return p.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // fallthrough
        }

        throw new IllegalStateException("Cannot instantiate profiler");
    }

    public static List<ExternalProfiler> getSupportedExternal(Collection<ProfilerConfig> cfg) {
        List<ExternalProfiler> profilers = new ArrayList<>();
        for (ProfilerConfig p : cfg) {
            Profiler prof = ProfilerFactory.getProfilerOrNull(p);
            if (prof instanceof ExternalProfiler) {
                profilers.add((ExternalProfiler) prof);
            }
        }
        return profilers;
    }

    public static List<InternalProfiler> getSupportedInternal(Collection<ProfilerConfig> cfg) {
        List<InternalProfiler> profilers = new ArrayList<>();
        for (ProfilerConfig p : cfg) {
            Profiler prof = ProfilerFactory.getProfilerOrNull(p);
            if (prof instanceof InternalProfiler) {
                profilers.add((InternalProfiler) prof);
            }
        }
        return profilers;
    }

    public static void listProfilers(PrintStream out) {
        int maxLen = 0;
        for (String s : BUILT_IN.keySet()) {
            maxLen = Math.max(maxLen, s.length());
        }
        for (Class<? extends Profiler> s : ProfilerFactory.getDiscoveredProfilers()) {
            maxLen = Math.max(maxLen, s.getCanonicalName().length());
        }
        maxLen += 2;

        StringBuilder supported = new StringBuilder();
        StringBuilder unsupported = new StringBuilder();

        for (String s : BUILT_IN.keySet()) {
            try {
                Profiler prof = getProfilerOrException(new ProfilerConfig(s, ""));
                supported.append(String.format("%" + maxLen + "s: %s %s\n", s, prof.getDescription(), ""));
            } catch (ProfilerException e) {
                unsupported.append(String.format("%" + maxLen + "s: %s %s\n", s, "<none>", ""));
                unsupported.append(e.getMessage());
                unsupported.append("\n");
            }
        }

        for (Class<? extends Profiler> s : ProfilerFactory.getDiscoveredProfilers()) {
            try {
                Profiler prof = getProfilerOrException(new ProfilerConfig(s.getCanonicalName(), ""));
                supported.append(String.format("%" + maxLen + "s: %s %s\n", s.getCanonicalName(), prof.getDescription(), "(discovered)"));
            } catch (ProfilerException e) {
                unsupported.append(String.format("%" + maxLen + "s: %s %s\n", s, s.getCanonicalName(), ""));
                unsupported.append(e.getMessage());
                unsupported.append("\n");
            }
        }

        if (!supported.toString().isEmpty()) {
            out.println("Supported profilers:\n" + supported.toString());
        }

        if (!unsupported.toString().isEmpty()) {
            out.println("Unsupported profilers:\n" + unsupported.toString());
        }
    }


    private static final Map<String, Class<? extends Profiler>> BUILT_IN;

    static {
        BUILT_IN = new TreeMap<>();
        BUILT_IN.put("async",    AsyncProfiler.class);
        BUILT_IN.put("cl",       ClassloaderProfiler.class);
        BUILT_IN.put("comp",     CompilerProfiler.class);
        BUILT_IN.put("gc",       GCProfiler.class);
        BUILT_IN.put("hs_cl",    HotspotClassloadingProfiler.class);
        BUILT_IN.put("hs_comp",  HotspotCompilationProfiler.class);
        BUILT_IN.put("hs_gc",    HotspotMemoryProfiler.class);
        BUILT_IN.put("hs_rt",    HotspotRuntimeProfiler.class);
        BUILT_IN.put("hs_thr",   HotspotThreadProfiler.class);
        BUILT_IN.put("stack",    StackProfiler.class);
        BUILT_IN.put("perf",     LinuxPerfProfiler.class);
        BUILT_IN.put("perfnorm", LinuxPerfNormProfiler.class);
        BUILT_IN.put("perfasm",  LinuxPerfAsmProfiler.class);
        BUILT_IN.put("xperfasm", WinPerfAsmProfiler.class);
        BUILT_IN.put("dtraceasm", DTraceAsmProfiler.class);
        BUILT_IN.put("pauses",   PausesProfiler.class);
        BUILT_IN.put("safepoints", SafepointsProfiler.class);
    }

    private static List<Class<? extends Profiler>> getDiscoveredProfilers() {
        List<Class<? extends Profiler>> profs = new ArrayList<>();
        for (Profiler s : ServiceLoader.load(Profiler.class)) {
            profs.add(s.getClass());
        }
        return profs;
    }

}
