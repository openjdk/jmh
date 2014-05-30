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
import sun.management.HotspotCompilationMBean;
import sun.management.counter.Counter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HotspotCompilationProfiler extends AbstractHotspotProfiler {

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotCompilationMBean>getInstance("HotspotCompilationMBean").getInternalCompilerCounters();
    }

    @Override
    public String label() {
        return "hs_comp";
    }

    @Override
    public String getDescription() {
        return "HotSpot (tm) JIT compiler profiling via implementation-specific MBeans";
    }

    @Override
    public Collection<? extends Result> afterIteration() {
        Map<String, Long> current = counters().getCurrent();
        return Arrays.asList(
                new ProfilerResult("@compiler.totalTime",
                        TimeUnit.NANOSECONDS.toMillis(current.get("java.ci.totalTime")),
                        "ms", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.totalCompiles",
                        current.get("sun.ci.totalCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.totalBailouts",
                        current.get("sun.ci.totalBailouts"),
                        "methods", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.totalInvalidates",
                        current.get("sun.ci.totalInvalidates"),
                        "methods", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.nmethodCodeSize",
                        current.get("sun.ci.nmethodCodeSize")/ 1024,
                        "Kb", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.nmethodSize",
                        current.get("sun.ci.nmethodSize") / 1024,
                        "Kb", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.osrCompiles",
                        current.get("sun.ci.osrCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.osrBytes",
                        current.get("sun.ci.osrBytes") / 1024,
                        "Kb", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.osrTime",
                        TimeUnit.NANOSECONDS.toMillis(current.get("sun.ci.osrTime")),
                        "ms", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.standardCompiles",
                        current.get("sun.ci.standardCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.standardBytes",
                        current.get("sun.ci.standardBytes") / 1024,
                        "Kb", AggregationPolicy.MAX),

                new ProfilerResult("@compiler.standardTime",
                        TimeUnit.NANOSECONDS.toMillis(current.get("sun.ci.standardTime")),
                        "ms", AggregationPolicy.MAX)
        );
    }

}

