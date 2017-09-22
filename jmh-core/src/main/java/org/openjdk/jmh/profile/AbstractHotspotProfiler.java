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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

abstract class AbstractHotspotProfiler implements InternalProfiler {

    private final Method getListMethod;
    private final Object bean;

    private Map<String, Long> prevs;

    public AbstractHotspotProfiler(String beanName) throws ProfilerException {
        try {
            Class<?> helper = Class.forName("sun.management.ManagementFactoryHelper");
            bean = helper.getMethod("get" + beanName).invoke(null);
            getListMethod = bean.getClass().getMethod("getInternalRuntimeCounters");
            getListMethod.setAccessible(true);
            getListMethod.invoke(bean); // try
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ProfilerException("Problem initializing profiler (" + e.getMessage() + "), are you running HotSpot VM?");
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
        for (HotspotCounter counter : getCounters()) {
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
        for (HotspotCounter counter : getCounters()) {
            Long prev = prevs.get(counter.getName());
            if (prev != null) {
                long diff = convert(counter.getValue()) - prev;
                difference.put(counter.getName(), diff);
                current.put(counter.getName(), convert(counter.getValue()));
            }
        }

        return new HotspotInternalResult(current, difference);
    }

    public List<HotspotCounter> getCounters() {
        try {
            List<HotspotCounter> counters = new ArrayList<>();
            for (Object c : (List) getListMethod.invoke(bean)) {
                try {
                    counters.add(new HotspotCounter(c));
                } catch (UnsupportedOperationException e) {
                    // ignore this counter
                }
            }
            return counters;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Should not be here", e);
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

    /**
     * Reflective proxy for Hotspot counters to dodge compatibility problems.
     */
    private static class HotspotCounter {
        private static final Method GET_VALUE;
        private static final Method GET_NAME;

        static {
            Method name = null;
            Method value = null;
            try {
                Class<?> cntClass = Class.forName("sun.management.counter.Counter");
                if (cntClass != null) {
                    try {
                        name = cntClass.getMethod("getName");
                    } catch (NoSuchMethodException e) {
                        // do nothing
                    }
                    try {
                        value = cntClass.getMethod("getValue");
                    } catch (NoSuchMethodException e) {
                        // do nothing
                    }
                }
            } catch (ClassNotFoundException e) {
                // no nothing
            }

            GET_NAME = name;
            GET_VALUE = value;
        }

        private final Object proxy;

        public HotspotCounter(Object proxy) throws UnsupportedOperationException {
            this.proxy = proxy;

            // Try these right now
            if (GET_NAME == null || GET_VALUE == null)  {
                throw new UnsupportedOperationException();
            }
            try {
                String k = (String) GET_NAME.invoke(proxy);
                Object v = GET_VALUE.invoke(proxy);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        public String getName() {
            try {
                return (String) GET_NAME.invoke(proxy);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot be here");
            }
        }

        public Object getValue() {
            try {
                return GET_VALUE.invoke(proxy);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot be here");
            }
        }
    }

}
