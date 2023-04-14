/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MemPoolProfiler implements InternalProfiler {
    static final double BYTES_PER_KIB = 1024D;

    @Override
    public String getDescription() {
        return "Memory pool/footprint profiling via standard MBeans";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans()) {
            b.resetPeakUsage();
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        List<Result> results = new ArrayList<>();

        long sumCodeHeap = 0L;
        long sum = 0L;
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            long used = bean.getPeakUsage().getUsed();
            if (bean.getName().contains("CodeHeap")) {
                sumCodeHeap += used;
            }
            sum += used;

            results.add(new ScalarResult(Defaults.PREFIX + "mempool." + bean.getName() + ".used", used / BYTES_PER_KIB, "KiB", AggregationPolicy.MAX));
        }

        results.add(new ScalarResult(Defaults.PREFIX + "mempool.total.codeheap.used", sumCodeHeap / BYTES_PER_KIB, "KiB", AggregationPolicy.MAX));
        results.add(new ScalarResult(Defaults.PREFIX + "mempool.total.used", sum / BYTES_PER_KIB, "KiB", AggregationPolicy.MAX));
        return results;
    }
}
