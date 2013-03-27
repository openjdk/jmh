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

import sun.management.counter.Counter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Aleksey Shipilev
 */
public abstract class AbstractHotspotProfiler implements Profiler {

    private final String name;
    protected final boolean verbose;
    private Map<String, Long> prevs;
    private long startTime;

    public AbstractHotspotProfiler(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    /**
     * Returns internal counters for specific MXBean
     * @return list of internal counters.
     */
    protected abstract Collection<Counter> getCounters();

    @Override
    public void startProfile() {
        prevs = new HashMap<String, Long>();
        for (Counter counter : getCounters()) {
            prevs.put(counter.getName(), convert(counter.getValue()));
        }
        startTime = System.currentTimeMillis();
    }

    public static Long convert(Object o) {
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    @Override
    public HotspotInternalResult endProfile() {
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Long> difference = new TreeMap<String, Long>();
        Map<String, Long> current = new TreeMap<String, Long>();
        for (Counter counter : getCounters()) {
            Long prev = prevs.get(counter.getName());
            if (prev != null) {
                long diff = convert(counter.getValue()) - prev;
                if (verbose || diff != 0) {
                    difference.put(counter.getName(), diff);
                }
                current.put(counter.getName(), convert(counter.getValue()));
            }
        }

        return new HotspotInternalResult(name, current, difference, duration);
    }

    public static <T> T getInstance(String name) {
        try {
            Object o = Class.forName("sun.management.ManagementFactoryHelper").getMethod("get" + name).invoke(null);
            return (T) o;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Should not be here");
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Should not be here");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Should not be here");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Should not be here");
        }
    }

    /**
     * Checks if this profiler is accessible
     * @return true, if accessible; false otherwise
     */
    public static boolean isSupported() {
        try {
            Class.forName("sun.management.ManagementFactoryHelper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Represents the HotSpot profiling result.
     */
    static class HotspotInternalResult implements ProfilerResult {

        private final String name;
        private final Map<String, Long> current;
        private final Map<String, Long> diff;
        private final long durationMsec;

        public HotspotInternalResult(String name, Map<String, Long> current, Map<String, Long> diff, long durationMsec) {
            this.name = name;
            this.current = current;
            this.diff = diff;
            this.durationMsec = durationMsec;
        }

        public HotspotInternalResult(HotspotInternalResult result) {
            this(result.name, result.current, result.diff, result.durationMsec);
        }

        @Override
        public String getProfilerName() {
            return name;
        }

        @Override
        public boolean hasData() {
            return true;
        }

        public Map<String, Long> getCurrent() {
            return current;
        }

        public Map<String, Long> getDiff() {
            return diff;
        }

        public long getDurationMsec() {
            return durationMsec;
        }

        protected long deNull(Long v) {
            return v == null ? 0 : v;
        }

        @Override
        public String toString() {
            return "difference: " + diff.toString();
        }
    }
}
