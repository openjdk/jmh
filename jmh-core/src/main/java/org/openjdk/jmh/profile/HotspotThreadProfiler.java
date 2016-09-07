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
import sun.management.HotspotThreadMBean;
import sun.management.counter.Counter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HotspotThreadProfiler extends AbstractHotspotProfiler {

    public HotspotThreadProfiler() throws ProfilerException {
    }

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotThreadMBean>getInstance("HotspotThreadMBean").getInternalThreadingCounters();
    }

    @Override
    public String getDescription() {
        return "HotSpot (tm) threading subsystem via implementation-specific MBeans";
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        Map<String, Long> current = counters().getCurrent();
        return Arrays.asList(
                new ScalarResult(Defaults.PREFIX + "threads.alive",
                        current.get("java.threads.live"),
                        "threads", AggregationPolicy.AVG),

                new ScalarResult(Defaults.PREFIX + "threads.daemon",
                        current.get("java.threads.daemon"),
                        "threads", AggregationPolicy.AVG),

                new ScalarResult(Defaults.PREFIX + "threads.started",
                        current.get("java.threads.started"),
                        "threads", AggregationPolicy.MAX)
        );
    }

}
