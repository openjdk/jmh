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
import sun.management.counter.Counter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

abstract class AbstractHotspotProfiler implements InternalProfiler {

    private Map<String, Long> prevs;

    /**
     * Returns internal counters for specific MXBean
     * @return list of internal counters.
     */
    protected abstract Collection<Counter> getCounters();

    public AbstractHotspotProfiler() throws ProfilerException {
        try {
            Class.forName("sun.management.ManagementFactoryHelper");
        } catch (ClassNotFoundException e) {
            throw new ProfilerException("Class not found: " + e.getMessage() + ", are you running HotSpot VM?");
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        HotspotInternalResult res = counters();
        Collection<ScalarResult> results = new ArrayList<>();
        for (Map.Entry<String, Long> e : res.getDiff().entrySet()) {
            results.add(new ScalarResult(Defaults.PREFIX + e.getKey(), e.getValue(), "?", AggregationPolicy.AVG));
        }
        return results;
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        prevs = new HashMap<>();
        for (Counter counter : getCounters()) {
            prevs.put(counter.getName(), convert(counter.getValue()));
        }
    }

    public static Long convert(Object o) {
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    protected HotspotInternalResult counters() {
        Map<String, Long> difference = new TreeMap<>();
        Map<String, Long> current = new TreeMap<>();
        for (Counter counter : getCounters()) {
            Long prev = prevs.get(counter.getName());
            if (prev != null) {
                long diff = convert(counter.getValue()) - prev;
                difference.put(counter.getName(), diff);
                current.put(counter.getName(), convert(counter.getValue()));
            }
        }

        return new HotspotInternalResult(current, difference);
    }

    public static <T> T getInstance(String name) {
        try {
            Object o = Class.forName("sun.management.ManagementFactoryHelper").getMethod("get" + name).invoke(null);
            return (T) o;
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Should not be here");
        }
    }

    /**
     * Represents the HotSpot profiling result.
     */
    static class HotspotInternalResult {
        private final Map<String, Long> current;
        private final Map<String, Long> diff;

        public HotspotInternalResult(Map<String, Long> current, Map<String, Long> diff) {
            this.current = current;
            this.diff = diff;
        }

        public Map<String, Long> getCurrent() {
            return current;
        }

        public Map<String, Long> getDiff() {
            return diff;
        }

        @Override
        public String toString() {
            return "difference: " + diff.toString();
        }
    }
}
