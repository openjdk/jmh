/*
 * Copyright (c) 2005, 2022, Oracle and/or its affiliates. All rights reserved.
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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.runner.options.IntegerValueConverter;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.Multiset;

import javax.management.ListenerNotFoundException;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GCProfiler implements InternalProfiler {
    private long beforeTime;
    private long beforeGCCount;
    private long beforeGCTime;
    private HotspotAllocationSnapshot beforeAllocated;

    private boolean churnEnabled;
    private boolean allocEnabled;
    private long churnWait;

    @Override
    public String getDescription() {
        return "GC profiling via standard MBeans";
    }

    public GCProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter(PausesProfiler.class.getCanonicalName()));

        OptionSpec<Boolean> optAllocEnable = parser.accepts("alloc", "Enable GC allocation measurement.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(true);

        OptionSpec<Boolean> optChurnEnable = parser.accepts("churn", "Enable GC churn measurement.")
                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);

        OptionSpec<Integer> optChurnWait = parser.accepts("churnWait", "Time to wait for churn notifications to arrive.")
                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("ms").defaultsTo(500);

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try {
            churnWait = set.valueOf(optChurnWait);
            churnEnabled = set.valueOf(optChurnEnable);
            allocEnabled = set.valueOf(optAllocEnable);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }

        if (churnEnabled) {
            if (!VMSupport.tryInitChurn()) {
                churnEnabled = false;
            }
        }

        if (allocEnabled) {
            if (!VMSupport.tryInitAlloc()) {
                allocEnabled = false;
            }
        }
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        if (churnEnabled) {
            VMSupport.startChurnProfile();
        }

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        this.beforeGCCount = gcCount;
        this.beforeGCTime = gcTime;

        if (allocEnabled) {
            this.beforeAllocated = VMSupport.getSnapshot();
        }
        this.beforeTime = System.nanoTime();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iResult) {
        long afterTime = System.nanoTime();

        if (churnEnabled) {
            VMSupport.finishChurnProfile(churnWait);
        }

        List<ScalarResult> results = new ArrayList<>();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        results.add(new ScalarResult(
                "gc.count",
                gcCount - beforeGCCount,
                "counts",
                AggregationPolicy.SUM));

        if (gcCount != beforeGCCount || gcTime != beforeGCTime) {
            results.add(new ScalarResult(
                    "gc.time",
                    gcTime - beforeGCTime,
                    "ms",
                    AggregationPolicy.SUM));
        }

        if (allocEnabled) {
            if (beforeAllocated != null) {
                HotspotAllocationSnapshot newSnapshot = VMSupport.getSnapshot();
                long allocated = newSnapshot.difference(beforeAllocated);
                // When no allocations measured, we still need to report results to avoid user confusion
                results.add(new ScalarResult("gc.alloc.rate",
                        (afterTime != beforeTime) ?
                                1.0 * allocated / 1024 / 1024 * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) :
                                Double.NaN,
                        "MB/sec", AggregationPolicy.AVG));
                if (allocated != 0) {
                    long allOps = iResult.getMetadata().getAllOps();
                    results.add(new ScalarResult("gc.alloc.rate.norm",
                            (allOps != 0) ?
                                    1.0 * allocated / allOps :
                                    Double.NaN,
                            "B/op", AggregationPolicy.AVG));
                }
            } else {
                // When allocation profiling fails, make sure it is distinguishable in report
                results.add(new ScalarResult("gc.alloc.rate",
                        Double.NaN,
                        "MB/sec", AggregationPolicy.AVG));
            }
        }

        if (churnEnabled) {
            Multiset<String> churn = VMSupport.getChurn();
            for (String space : churn.keys()) {
                double churnRate = (afterTime != beforeTime) ?
                        1.0 * churn.count(space) * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) / 1024 / 1024 :
                        Double.NaN;

                double churnNorm = 1.0 * churn.count(space) / iResult.getMetadata().getAllOps();

                String spaceName = space.replaceAll(" ", "_");

                results.add(new ScalarResult(
                        "gc.churn." + spaceName + "",
                        churnRate,
                        "MB/sec",
                        AggregationPolicy.AVG));

                results.add(new ScalarResult(
                        "gc.churn." + spaceName + ".norm",
                        churnNorm,
                        "B/op",
                        AggregationPolicy.AVG));
            }
        }

        return results;
    }

    interface HotspotAllocationSnapshot {
        long difference(HotspotAllocationSnapshot before);
    }

    static class GlobalHotspotAllocationSnapshot implements HotspotAllocationSnapshot {
        private final long allocatedBytes;

        public GlobalHotspotAllocationSnapshot(long allocatedBytes) {
            this.allocatedBytes = allocatedBytes;
        }

        @Override
        public long difference(HotspotAllocationSnapshot before) {
            if (!(before instanceof GlobalHotspotAllocationSnapshot)) {
                throw new IllegalArgumentException();
            }

            GlobalHotspotAllocationSnapshot other = (GlobalHotspotAllocationSnapshot) before;

            long beforeAllocs = other.allocatedBytes;
            if (allocatedBytes >= beforeAllocs) {
                return allocatedBytes - beforeAllocs;
            } else {
                // Do not allow negative values
                return 0;
            }
        }
    }

    static class PerThreadHotspotAllocationSnapshot implements HotspotAllocationSnapshot {
        private final long[] threadIds;
        private final long[] allocatedBytes;

        private PerThreadHotspotAllocationSnapshot(long[] threadIds, long[] allocatedBytes) {
            this.threadIds = threadIds;
            this.allocatedBytes = allocatedBytes;
        }

        /**
         * Estimates allocated bytes based on two snapshots.
         * The problem is threads can come and go while performing the benchmark,
         * thus we would miss allocations made in a thread that was created and died between the snapshots.
         * <p/>
         * <p>Current thread is intentionally excluded since it believed to execute jmh infrastructure code only.
         *
         * @return estimated number of allocated bytes between profiler calls
         */
        public long difference(HotspotAllocationSnapshot before) {
            if (!(before instanceof PerThreadHotspotAllocationSnapshot)) {
                throw new IllegalArgumentException();
            }

            PerThreadHotspotAllocationSnapshot other = (PerThreadHotspotAllocationSnapshot) before;

            HashMap<Long, Integer> prevIndex = new HashMap<>();
            for (int i = 0; i < other.threadIds.length; i++) {
                long id = other.threadIds[i];
                prevIndex.put(id, i);
            }
            long currentThreadId = Thread.currentThread().getId();
            long allocated = 0;
            for (int i = 0; i < threadIds.length; i++) {
                long id = threadIds[i];
                if (id == currentThreadId) {
                    continue;
                }
                allocated += allocatedBytes[i];
                Integer prev = prevIndex.get(id);
                if (prev != null) {
                    allocated -= other.allocatedBytes[prev];
                }
            }
            return allocated;
        }
    }

    /**
     * This class encapsulates any platform-specific functionality. It is supposed to gracefully
     * fail if some functionality is not available. This class resolves most special classes via
     * Reflection to enable building against a standard JDK.
     */
    static class VMSupport {
        private static ThreadMXBean ALLOC_MX_BEAN;
        private static Method ALLOC_MX_BEAN_GETTER_PER_THREAD;
        private static Method ALLOC_MX_BEAN_GETTER_GLOBAL;
        private static NotificationListener LISTENER;
        private static Multiset<String> CHURN;

        private static boolean tryInitAlloc() {
            try {
                Class<?> internalIntf = Class.forName("com.sun.management.ThreadMXBean");
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (!internalIntf.isAssignableFrom(bean.getClass())) {
                    Class<?> pmo = Class.forName("java.lang.management.PlatformManagedObject");
                    Method m = ManagementFactory.class.getMethod("getPlatformMXBean", Class.class, pmo);
                    bean = (ThreadMXBean) m.invoke(null, internalIntf);
                    if (bean == null) {
                        throw new UnsupportedOperationException("No way to access private ThreadMXBean");
                    }
                }

                ALLOC_MX_BEAN = bean;

                // See if global getter is available in this JVM
                try {
                    ALLOC_MX_BEAN_GETTER_GLOBAL = internalIntf.getMethod("getTotalThreadAllocatedBytes");
                    getSnapshot();
                    return true;
                } catch (Exception e) {
                    // Fall through
                }

                // See if per-thread getter is available in this JVM
                ALLOC_MX_BEAN_GETTER_PER_THREAD = internalIntf.getMethod("getThreadAllocatedBytes", long[].class);
                getSnapshot();
                return true;
            } catch (Throwable e) {
                System.out.println("Allocation profiling is not available: " + e.getMessage());
            }
            return false;
        }

        private static boolean tryInitChurn() {
            try {
                for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    if (!(bean instanceof NotificationEmitter)) {
                        throw new UnsupportedOperationException("GarbageCollectorMXBean cannot notify");
                    }
                }
                CHURN = new HashMultiset<>();
                LISTENER = newListener();
                return true;
            } catch (Throwable e) {
                System.out.println("Churn profiling is not available: " + e.getMessage());
            }

            return false;
        }

        private static NotificationListener newListener() {
            try {
                final Class<?> infoKlass = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
                final Field notifNameField = infoKlass.getField("GARBAGE_COLLECTION_NOTIFICATION");
                final Method infoMethod = infoKlass.getMethod("from", CompositeData.class);
                final Method getGcInfo = infoKlass.getMethod("getGcInfo");
                final Method getMemoryUsageBeforeGc = getGcInfo.getReturnType().getMethod("getMemoryUsageBeforeGc");
                final Method getMemoryUsageAfterGc = getGcInfo.getReturnType().getMethod("getMemoryUsageAfterGc");

                return (n, o) -> {
                    try {
                        if (n.getType().equals(notifNameField.get(null))) {
                            Object info = infoMethod.invoke(null, n.getUserData());
                            Object gcInfo = getGcInfo.invoke(info);
                            Map<String, MemoryUsage> mapBefore = (Map<String, MemoryUsage>) getMemoryUsageBeforeGc.invoke(gcInfo);
                            Map<String, MemoryUsage> mapAfter = (Map<String, MemoryUsage>) getMemoryUsageAfterGc.invoke(gcInfo);
                            for (Map.Entry<String, MemoryUsage> entry : mapAfter.entrySet()) {
                                String name = entry.getKey();
                                MemoryUsage after = entry.getValue();
                                MemoryUsage before = mapBefore.get(name);
                                long c = before.getUsed() - after.getUsed();
                                if (c > 0) {
                                    CHURN.add(name, c);
                                }
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        // Do nothing, counters would not get populated
                    }
                };
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        public static HotspotAllocationSnapshot getSnapshot() {
            // Try the global getter first, if available
            if (ALLOC_MX_BEAN_GETTER_GLOBAL != null) {
                try {
                    long allocatedBytes = (long) ALLOC_MX_BEAN_GETTER_GLOBAL.invoke(ALLOC_MX_BEAN);
                    if (allocatedBytes == -1L) {
                        throw new IllegalStateException("getTotalThreadAllocatedBytes is disabled");
                    }
                    return new GlobalHotspotAllocationSnapshot(allocatedBytes);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }

            // Fall back to per-thread getter
            long[] threadIds = ALLOC_MX_BEAN.getAllThreadIds();
            try {
                long[] allocatedBytes = (long[]) ALLOC_MX_BEAN_GETTER_PER_THREAD.invoke(ALLOC_MX_BEAN, (Object) threadIds);
                return new PerThreadHotspotAllocationSnapshot(threadIds, allocatedBytes);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static synchronized void startChurnProfile() {
            CHURN.clear();
            try {
                for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    ((NotificationEmitter) bean).addNotificationListener(LISTENER, null, null);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Should not be here");
            }
        }

        public static synchronized void finishChurnProfile(long churnWait) {
            // Notifications are asynchronous, need to wait a bit before deregistering the listener.
            try {
                Thread.sleep(churnWait);
            } catch (InterruptedException e) {
                // do not care
            }

            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                try {
                    ((NotificationEmitter) bean).removeNotificationListener(LISTENER);
                } catch (ListenerNotFoundException e) {
                    // Do nothing
                }
            }
        }

        public static synchronized Multiset<String> getChurn() {
            return (CHURN != null) ? CHURN : new HashMultiset<>();
        }
    }

}
