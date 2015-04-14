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
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.Multiset;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GCProfiler implements InternalProfiler {
    private long beforeTime;
    private long beforeGCCount;
    private long beforeGCTime;
    private final NotificationListener listener;
    private volatile Multiset<String> churn;
    private Set<String> observedSpaces;

    public GCProfiler() {
        churn = new HashMultiset<String>();
        observedSpaces = Collections.synchronizedSet(new HashSet<String>());

        NotificationListener listener = null;
        try {
            final Class<?> infoKlass = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
            final Field notifNameField = infoKlass.getField("GARBAGE_COLLECTION_NOTIFICATION");
            final Method infoMethod = infoKlass.getMethod("from", CompositeData.class);
            final Method getGcInfo = infoKlass.getMethod("getGcInfo");
            final Method getMemoryUsageBeforeGc = getGcInfo.getReturnType().getMethod("getMemoryUsageBeforeGc");
            final Method getMemoryUsageAfterGc = getGcInfo.getReturnType().getMethod("getMemoryUsageAfterGc");

            listener = new NotificationListener() {
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
                                    observedSpaces.add(name);
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        // Do nothing, counters would not get populated
                    } catch (InvocationTargetException e) {
                        // Do nothing, counters would not get populated
                    }
                }
            };
        } catch (ClassNotFoundException e) {
            // Nothing to do here, listener is not functional
        } catch (NoSuchFieldException e) {
            // Nothing to do here, listener is not functional
        } catch (NoSuchMethodException e) {
            // Nothing to do here, listener is not functional
        }

        this.listener = listener;
    }

    @Override
    public String getDescription() {
        return "GC profiling via standard MBeans";
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        return true;
    }

    @Override
    public String label() {
        return "gc";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        installHooks();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        this.beforeGCCount = gcCount;
        this.beforeGCTime = gcTime;
        this.beforeTime = System.nanoTime();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iResult) {
        uninstallHooks();
        long afterTime = System.nanoTime();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        List<ProfilerResult> results = new ArrayList<ProfilerResult>();

        results.add(new ProfilerResult(
                Defaults.PREFIX + "gc.count",
                gcCount - beforeGCCount,
                "counts",
                AggregationPolicy.SUM));

        results.add(new ProfilerResult(
                Defaults.PREFIX + "gc.time",
                gcTime - beforeGCTime,
                "ms",
                AggregationPolicy.SUM));

        for (String space : observedSpaces) {
            double churnRate = 1.0 * churn.count(space) * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime);
            String spaceName = space.replaceAll(" ", "_");

            results.add(new ProfilerResult(
                    Defaults.PREFIX + "gc.churn." + spaceName + "",
                    churnRate / 1024 / 1024,
                    "MB/sec",
                    AggregationPolicy.AVG));

            results.add(new ProfilerResult(Defaults.PREFIX + "gc.churn." + spaceName + ".norm",
                    churnRate / iResult.getMetadata().getAllOps(),
                    "B/op",
                    AggregationPolicy.AVG));
        }

        return results;
    }

    private boolean hooksInstalled;

    public synchronized void installHooks() {
        if (listener == null) return;
        if (hooksInstalled) return;
        hooksInstalled = true;
        churn = new HashMultiset<String>();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter) bean).addNotificationListener(listener, null, null);
        }
    }

    public synchronized void uninstallHooks() {
        if (listener == null) return;
        if (!hooksInstalled) return;
        hooksInstalled = false;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            try {
                ((NotificationEmitter) bean).removeNotificationListener(listener);
            } catch (ListenerNotFoundException e) {
                // Do nothing
            }
        }
    }

}
