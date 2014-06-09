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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Complete run result.
 * Contains the iteration results.
 */
public class RunResult implements Serializable {

    private static final long serialVersionUID = 6467912427356048369L;

    private final Collection<BenchmarkResult> benchmarkResults;
    private final BenchmarkParams params;

    public RunResult(Collection<BenchmarkResult> data) {
        this.benchmarkResults = data;

        BenchmarkParams myParams = null;

        for (BenchmarkResult br : data) {
            BenchmarkParams params = br.getParams();

            if (myParams != null && !params.equals(myParams)) {
                throw new IllegalStateException("Aggregating the benchmark results from different benchmarks");
            } else {
                myParams = params;
            }
        }

        this.params = myParams;
    }

    public Collection<BenchmarkResult> getBenchmarkResults() {
        return benchmarkResults;
    }

    public Result getPrimaryResult() {
         return getFakeResult().getPrimaryResult();
    }

    public Map<String, Result> getSecondaryResults() {
        return getFakeResult().getSecondaryResults();
    }

    /**
     * Implementation note: RunResult tries to aggregate BenchmarkResult as if
     * the underlying iterations are aggregated in the single run.
     */
    private BenchmarkResult getFakeResult() {
        Collection<IterationResult> results = new ArrayList<IterationResult>();
        for (BenchmarkResult r : benchmarkResults) {
            for (IterationResult ir : r.getIterationResults()) {
                results.add(ir);
            }
        }
        BenchmarkResult result = new BenchmarkResult(results);
        for (BenchmarkResult br : benchmarkResults) {
            for (Result ar : br.getBenchmarkResults()) {
                result.amend(ar);
            }
        }
        return result;
    }

    public String getScoreUnit() {
        return getPrimaryResult().getScoreUnit();
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
