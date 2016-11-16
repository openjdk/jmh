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
package org.openjdk.jmh.results;

import org.openjdk.jmh.infra.BenchmarkParams;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/**
 * Complete run result.
 * Contains the iteration results.
 */
public class RunResult implements Serializable {

    private static final long serialVersionUID = 6467912427356048369L;

    private final Collection<BenchmarkResult> benchmarkResults;
    private final BenchmarkParams params;

    public RunResult(BenchmarkParams params, Collection<BenchmarkResult> data) {
        this.benchmarkResults = data;
        this.params = params;
    }

    public Collection<BenchmarkResult> getBenchmarkResults() {
        return benchmarkResults;
    }

    public Result getPrimaryResult() {
         return getAggregatedResult().getPrimaryResult();
    }

    public Map<String, Result> getSecondaryResults() {
        return getAggregatedResult().getSecondaryResults();
    }

    /**
     * Return the benchmark result, as if all iterations from all sub-benchmark results
     * were merged in a single result.
     *
     * @return merged benchmark result
     */
    public BenchmarkResult getAggregatedResult() {
        if (benchmarkResults.isEmpty()) {
            return null;
        }

        Collection<IterationResult> results = new ArrayList<>();
        for (BenchmarkResult r : benchmarkResults) {
            for (IterationResult ir : r.getIterationResults()) {
                results.add(ir);
            }
        }
        BenchmarkResult result = new BenchmarkResult(params, results);
        for (BenchmarkResult br : benchmarkResults) {
            for (String k : br.getBenchmarkResults().keys()) {
                for (Result r : br.getBenchmarkResults().get(k)) {
                    result.addBenchmarkResult(r);
                }
            }
        }
        return result;
    }

    public BenchmarkParams getParams() {
        return params;
    }

    public static final Comparator<RunResult> DEFAULT_SORT_COMPARATOR = new Comparator<RunResult>() {
        @Override
        public int compare(RunResult o1, RunResult o2) {
            return o1.params.compareTo(o2.params);
        }
    };

}
