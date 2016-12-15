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
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.Multiset;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
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

    public GCProfiler() throws ProfilerException {
    }

    @Override
    public String getDescription() {
        return "GC profiling via standard MBeans";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        VMSupport.startChurnProfile();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        this.beforeGCCount = gcCount;
        this.beforeGCTime = gcTime;
        this.beforeAllocated = VMSupport.getSnapshot();
        this.beforeTime = System.nanoTime();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iResult) {
        VMSupport.finishChurnProfile();
        long afterTime = System.nanoTime();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        List<ScalarResult> results = new ArrayList<>();

        if (beforeAllocated == HotspotAllocationSnapshot.EMPTY) {
            // When allocation profiling fails, make sure it is distinguishable in report
            results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate",
                    Double.NaN,
                    "MB/sec", AggregationPolicy.AVG));
        } else {
            HotspotAllocationSnapshot newSnapshot = VMSupport.getSnapshot();
            long allocated = newSnapshot.subtract(beforeAllocated);
            // When no allocations measured, we still need to report results to avoid user confusion
            results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate",
                            (afterTime != beforeTime) ?
                                    1.0 * allocated / 1024 / 1024 * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) :
                                    Double.NaN,
                            "MB/sec", AggregationPolicy.AVG));
            if (allocated != 0) {
                long allOps = iResult.getMetadata().getAllOps();
                results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate.norm",
                                (allOps != 0) ?
                                        1.0 * allocated / allOps :
                                        Double.NaN,
                                "B/op", AggregationPolicy.AVG));
            }
        }

        results.add(new ScalarResult(
                Defaults.PREFIX + "gc.count",
                gcCount - beforeGCCount,
                "counts",
                AggregationPolicy.SUM));

        if (gcCount != beforeGCCount || gcTime != beforeGCTime) {
            results.add(new ScalarResult(
                    Defaults.PREFIX + "gc.time",
                    gcTime - beforeGCTime,
                    "ms",
                    AggregationPolicy.SUM));
        }

        Multiset<String> churn = VMSupport.getChurn();
        for (String space : churn.keys()) {
            double churnRate = (afterTime != beforeTime) ?
                    1.0 * churn.count(space) * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) / 1024 / 1024 :
                    Double.NaN;

            double churnNorm = 1.0 * churn.count(space) / iResult.getMetadata().getAllOps();

            String spaceName = space.replaceAll(" ", "_");

            results.add(new ScalarResult(
                    Defaults.PREFIX + "gc.churn." + spaceName + "",
                    churnRate,
                    "MB/sec",
                    AggregationPolicy.AVG));

            results.add(new ScalarResult(
                    Defaults.PREFIX + "gc.churn." + spaceName + ".norm",
                    churnNorm,
                    "B/op",
                    AggregationPolicy.AVG));
        }

        return results;
    }

    static class HotspotAllocationSnapshot {
        public final static HotspotAllocationSnapshot EMPTY = new HotspotAllocationSnapshot(new long[0], new long[0]);

        private final long[] threadIds;
        private final long[] allocatedBytes;

        private HotspotAllocationSnapshot(long[] threadIds, long[] allocatedBytes) {
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
        public long subtract(HotspotAllocationSnapshot other) {
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
        private static final boolean ALLOC_AVAILABLE;
        private static ThreadMXBean ALLOC_MX_BEAN;
        private static Method ALLOC_MX_BEAN_GETTER;
        private static final boolean CHURN_AVAILABLE;
        private static NotificationListener listener;
        private static Multiset<String> churn;

        static {
            ALLOC_AVAILABLE = tryInitAlloc();
            CHURN_AVAILABLE = tryInitChurn();
        }

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
                ALLOC_MX_BEAN_GETTER = internalIntf.getMethod("getThreadAllocatedBytes", long[].class);
                getAllocatedBytes(bean.getAllThreadIds());

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
                newListener();
                return true;
            } catch (Throwable e) {
                System.out.println("Churn profiling is not available: " + e.getMessage());
            }

            return false;
        }

        private static long[] getAllocatedBytes(long[] threadIds) {
            try {
                return (long[]) ALLOC_MX_BEAN_GETTER.invoke(ALLOC_MX_BEAN, (Object) threadIds);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private static NotificationListener newListener() {
            churn = new HashMultiset<>();
            try {
                final Class<?> infoKlass = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
                final Field notifNameField = infoKlass.getField("GARBAGE_COLLECTION_NOTIFICATION");
                final Method infoMethod = infoKlass.getMethod("from", CompositeData.class);
                final Method getGcInfo = infoKlass.getMethod("getGcInfo");
                final Method getMemoryUsageBeforeGc = getGcInfo.getReturnType().getMethod("getMemoryUsageBeforeGc");
                final Method getMemoryUsageAfterGc = getGcInfo.getReturnType().getMethod("getMemoryUsageAfterGc");

                return new NotificationListener() {
                    @Override
                    public void handleNotification(Notification n, Object o) {
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
                                        churn.add(name, c);
                                    }
                                }
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            // Do nothing, counters would not get populated
                        }
                    }
                };
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        public static HotspotAllocationSnapshot getSnapshot() {
            if (!ALLOC_AVAILABLE) return HotspotAllocationSnapshot.EMPTY;
            long[] threadIds = ALLOC_MX_BEAN.getAllThreadIds();
            long[] allocatedBytes = getAllocatedBytes(threadIds);
            return new HotspotAllocationSnapshot(threadIds, allocatedBytes);
        }

        public static synchronized void startChurnProfile() {
            if (!CHURN_AVAILABLE) return;
            if (listener != null) {
                throw new IllegalStateException("Churn profile already started");
            }
            listener = newListener();
            try {
                for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    ((NotificationEmitter) bean).addNotificationListener(listener, null, null);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Should not be here");
            }
        }

        public static synchronized void finishChurnProfile() {
            if (!CHURN_AVAILABLE) return;
            if (listener == null) {
                throw new IllegalStateException("Churn profile already stopped");
            }

            // Notifications are asynchronous, need to wait a bit before deregistering the listener.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // do not care
            }

            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                try {
                    ((NotificationEmitter) bean).removeNotificationListener(listener);
                } catch (ListenerNotFoundException e) {
                    // Do nothing
                }
            }
            listener = null;
        }

        public static synchronized Multiset<String> getChurn() {
            return (churn != null) ? churn : new HashMultiset<String>();
        }
    }

}
