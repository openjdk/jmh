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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProfilerFactory {

    public static List<Class<? extends Profiler>> getAvailableProfilers() {
        List<Class<? extends Profiler>> profs = new ArrayList<Class<? extends Profiler>>();
        profs.add(ClassloaderProfiler.class);
        profs.add(CompilerProfiler.class);
        profs.add(GCProfiler.class);
        profs.add(HotspotClassloadingProfiler.class);
        profs.add(HotspotCompilationProfiler.class);
        profs.add(HotspotMemoryProfiler.class);
        profs.add(HotspotRuntimeProfiler.class);
        profs.add(HotspotThreadProfiler.class);
        profs.add(StackProfiler.class);
        profs.add(DummyExternalProfiler.class);
        return profs;
    }

    public static Collection<String> checkSupport(Class<? extends Profiler> klass) {
        try {
            Profiler prof = klass.newInstance();
            return prof.checkSupport();
        } catch (InstantiationException e) {
            return Collections.singleton("Unable to instantiate " + klass);
        } catch (IllegalAccessException e) {
            return Collections.singleton("Unable to instantiate " + klass);
        }
    }

    public static String getDescription(Class<? extends Profiler> klass) {
        try {
            Profiler prof = klass.newInstance();
            return prof.getDescription();
        } catch (InstantiationException e) {
            return "(unable to instantiate the profiler)";
        } catch (IllegalAccessException e) {
            return "(unable to instantiate the profiler)";
        }
    }

    public static Class<? extends Profiler> getProfilerByName(String name) {
        try {
            Class<?> klass = Class.forName(name);
            if (Profiler.class.isAssignableFrom(klass)) {
                return (Class<? extends Profiler>) klass;
            }
        } catch (ClassNotFoundException e) {
            // omit
        }

        Collection<Class<? extends Profiler>> profilers = getAvailableProfilers();
        for (Class<? extends Profiler> p : profilers) {
            try {
                Profiler prof = p.newInstance();
                if (prof.label().equalsIgnoreCase(name)) {
                    return p;
                }
            } catch (InstantiationException e) {
                // omit
            } catch (IllegalAccessException e) {
                // omit
            }
        }

        return null;
    }

    public static Profiler prepareProfiler(Class<? extends Profiler> klass, VerboseMode verboseMode) {
        try {
            return klass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getLabel(Class<? extends Profiler> klass) {
        try {
            Profiler prof = klass.newInstance();
            return prof.label();
        } catch (InstantiationException e) {
            return "(unable to instantiate the profiler)";
        } catch (IllegalAccessException e) {
            return "(unable to instantiate the profiler)";
        }
    }

    public static boolean isInternal(Class<? extends Profiler> klass) {
        return InternalProfiler.class.isAssignableFrom(klass);
    }

    public static boolean isExternal(Class<? extends Profiler> klass) {
        return ExternalProfiler.class.isAssignableFrom(klass);
    }
}
