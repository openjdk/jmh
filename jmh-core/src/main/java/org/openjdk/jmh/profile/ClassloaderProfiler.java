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

import org.openjdk.jmh.logic.results.AggregationPolicy;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.util.internal.Optional;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassloaderProfiler implements Profiler {

    private long loadedClasses = -1;
    private long unloadedClasses = -1;

    @Override
    public String getDescription() {
        return "Classloader profiling via standard MBeans";
    }

    @Override
    public InjectionPoint point() {
        return InjectionPoint.FORKED_VM_CONTROL;
    }

    @Override
    public Collection<String> checkSupport() {
        return Collections.emptyList();
    }

    @Override
    public String label() {
        return "cl";
    }

    @Override
    public Optional<List<String>> addJVMOptions() {
        return Optional.none();
    }

    @Override
    public void beforeTrial() {

    }

    @Override
    public void beforeIteration() {
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
    public Collection<? extends Result> afterIteration() {
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

        return Arrays.asList(
                new ProfilerResult("@classload.loaded", loaded, "classes", AggregationPolicy.AVG),
                new ProfilerResult("@classload.unloaded", unloaded, "classes", AggregationPolicy.AVG)
        );
    }

    @Override
    public Collection<? extends Result> afterTrial() {
        return Collections.emptyList();
    }

}
