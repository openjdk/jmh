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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Benchmark result.
 * Contains iteration results.
 */
public class BenchmarkResult implements Serializable {

    private static final long serialVersionUID = 6467912427356048369L;

    private final Collection<IterationResult> iterationResults;
    private final Collection<Result> benchmarkResults;
    private final BenchmarkParams params;

    public BenchmarkResult(Collection<IterationResult> data) {
        this.benchmarkResults = new ArrayList<Result>();
        this.iterationResults = data;

        BenchmarkParams myParams = null;

        for (IterationResult ir : data) {
            BenchmarkParams params = ir.getBenchmarkParams();

            if (myParams != null && !params.equals(myParams)) {
                throw new IllegalStateException("Aggregating the iteration results from different benchmarks");
            } else {
                myParams = params;
            }
        }
        this.params = myParams;
    }

    public void amend(Result r) {
        // FIXME: Assumes secondary result.
        benchmarkResults.add(r);
    }

    public Collection<IterationResult> getIterationResults() {
        return iterationResults;
    }

    public Collection<Result> getBenchmarkResults() {
        return benchmarkResults;
    }

    public Result getPrimaryResult() {
        @SuppressWarnings("unchecked")
        Aggregator<Result> aggregator = iterationResults.iterator().next().getPrimaryResult().getIterationAggregator();

        Collection<Result> aggrs = new ArrayList<Result>();
        for (IterationResult r : iterationResults) {
            aggrs.add(r.getPrimaryResult());
        }
        return aggregator.aggregate(aggrs);
    }

    public Map<String, Result> getSecondaryResults() {
        Set<String> labels = new HashSet<String>();
        for (IterationResult r : iterationResults) {
            labels.addAll(r.getSecondaryResults().keySet());
        }

        Map<String, Result> answers = new TreeMap<String, Result>();
        for (String label : labels) {
            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = iterationResults.iterator().next().getSecondaryResults().get(label).getIterationAggregator();

            Collection<Result> results = new ArrayList<Result>();
            for (IterationResult r : iterationResults) {
                results.add(r.getSecondaryResults().get(label));
            }
            answers.put(label, aggregator.aggregate(results));
        }

        for (Result r : benchmarkResults) {
            answers.put(r.getLabel(), r);
        }

        return answers;
    }

    public String getScoreUnit() {
        return getPrimaryResult().getScoreUnit();
    }

    public BenchmarkParams getParams() {
        return params;
    }
}
