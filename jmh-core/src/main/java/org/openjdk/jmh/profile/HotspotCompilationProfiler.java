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
import org.openjdk.jmh.results.*;
import sun.management.HotspotCompilationMBean;
import sun.management.counter.Counter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HotspotCompilationProfiler extends AbstractHotspotProfiler {

    public HotspotCompilationProfiler() throws ProfilerException {
    }

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotCompilationMBean>getInstance("HotspotCompilationMBean").getInternalCompilerCounters();
    }

    @Override
    public String getDescription() {
        return "HotSpot (tm) JIT compiler profiling via implementation-specific MBeans";
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        Map<String, Long> current = counters().getCurrent();
        return Arrays.asList(
                new ScalarResult(Defaults.PREFIX + "compiler.totalTime",
                        current.get("java.ci.totalTime") * 1D / TimeUnit.MILLISECONDS.toNanos(1),
                        "ms", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.totalCompiles",
                        current.get("sun.ci.totalCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.totalBailouts",
                        current.get("sun.ci.totalBailouts"),
                        "methods", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.totalInvalidates",
                        current.get("sun.ci.totalInvalidates"),
                        "methods", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.nmethodCodeSize",
                        current.get("sun.ci.nmethodCodeSize") / 1024d,
                        "Kb", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.nmethodSize",
                        current.get("sun.ci.nmethodSize") / 1024d,
                        "Kb", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.osrCompiles",
                        current.get("sun.ci.osrCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.osrBytes",
                        current.get("sun.ci.osrBytes") / 1024d,
                        "Kb", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.osrTime",
                        current.get("sun.ci.osrTime") * 1d / TimeUnit.MILLISECONDS.toNanos(1),
                        "ms", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.standardCompiles",
                        current.get("sun.ci.standardCompiles"),
                        "methods", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.standardBytes",
                        current.get("sun.ci.standardBytes") / 1024d,
                        "Kb", AggregationPolicy.MAX),

                new ScalarResult(Defaults.PREFIX + "compiler.standardTime",
                        current.get("sun.ci.standardTime") * 1d / TimeUnit.MILLISECONDS.toNanos(1),
                        "ms", AggregationPolicy.MAX)
        );
    }

}

