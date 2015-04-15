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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassloaderProfiler implements InternalProfiler {

    private long loadedClasses;
    private long unloadedClasses;

    @Override
    public String getDescription() {
        return "Classloader profiling via standard MBeans";
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        return true;
    }

    @Override
    public String label() {
        return "cl";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
        try {
            loadedClasses = cl.getTotalLoadedClassCount();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
        try {
            unloadedClasses = cl.getUnloadedClassCount();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        List<Result> results = new ArrayList<Result>();

        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

        try {
            long loadedClassCount = cl.getTotalLoadedClassCount();
            long loaded = loadedClassCount - loadedClasses;
            results.add(new ProfilerResult(Defaults.PREFIX + "classload.loaded.profiled", loaded, "classes", AggregationPolicy.SUM));
            results.add(new ProfilerResult(Defaults.PREFIX + "classload.loaded.total", loadedClassCount, "classes", AggregationPolicy.MAX));
        } catch (UnsupportedOperationException e) {
            // do nothing
        }

        try {
            long unloadedClassCount = cl.getUnloadedClassCount();
            long unloaded = unloadedClassCount - unloadedClasses;
            results.add(new ProfilerResult(Defaults.PREFIX + "classload.unloaded.profiled", unloaded, "classes", AggregationPolicy.SUM));
            results.add(new ProfilerResult(Defaults.PREFIX + "classload.unloaded.total", unloadedClassCount, "classes", AggregationPolicy.MAX));

        } catch (UnsupportedOperationException e) {
            // do nothing
        }

        return results;
    }

}
