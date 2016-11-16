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
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import java.io.Serializable;
import java.util.*;

/**
 * Benchmark result.
 * Contains iteration results.
 */
public class BenchmarkResult implements Serializable {

    private static final long serialVersionUID = 6467912427356048369L;

    private final Collection<IterationResult> iterationResults;
    private final Multimap<String, Result> benchmarkResults;
    private final BenchmarkParams params;
    private final BenchmarkResultMetaData metadata;

    public BenchmarkResult(BenchmarkParams params, Collection<IterationResult> data) {
        this(params, data, null);
    }

    public BenchmarkResult(BenchmarkParams params, Collection<IterationResult> data, BenchmarkResultMetaData md) {
        this.metadata = md;
        this.benchmarkResults = new HashMultimap<>();
        this.iterationResults = data;
        this.params = params;
    }

    /**
     * @return returns the benchmark metadata, "null" otherwise
     */
    public BenchmarkResultMetaData getMetadata() {
        return metadata;
    }

    public void addBenchmarkResult(Result r) {
        benchmarkResults.put(r.getLabel(), r);
    }

    public Collection<IterationResult> getIterationResults() {
        return iterationResults;
    }

    public Multimap<String, Result> getBenchmarkResults() {
        return benchmarkResults;
    }

    public Result getPrimaryResult() {
        Aggregator<Result> aggregator = null;
        Collection<Result> aggrs = new ArrayList<>();
        for (IterationResult r : iterationResults) {
            Result e = r.getPrimaryResult();
            aggrs.add(e);
            aggregator = e.getIterationAggregator();
        }
        for (Result r : benchmarkResults.values()) {
            if (r.getRole().isPrimary()) {
                aggrs.add(r);
            }
        }

        if (aggregator != null) {
            return aggregator.aggregate(aggrs);
        } else {
            throw new IllegalStateException("No aggregator for primary result");
        }
    }

    public Map<String, Result> getSecondaryResults() {
        // label -> collection of results
        Multimap<String, Result> allSecondary = new HashMultimap<>();

        // Build multiset of all results to capture if some labels are missing in some of the iterations
        for (IterationResult ir : iterationResults) {
            Map<String, Result> secondaryResults = ir.getSecondaryResults();
            for (Map.Entry<String, Result> entry : secondaryResults.entrySet()) {
                // skip derivatives from aggregation here
                if (entry.getValue().getRole().isDerivative()) continue;

                allSecondary.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Result> answers = new TreeMap<>();

        int totalIterations = iterationResults.size();
        // Create "0" entries in case certain labels did not appear in some of the iterations
        for (String label : allSecondary.keys()) {
            Collection<Result> results = allSecondary.get(label);
            Result firstResult = results.iterator().next();
            Result emptyResult = firstResult.getZeroResult();
            if (emptyResult != null) {
                for (int i = results.size(); i < totalIterations; i++) {
                    allSecondary.put(label, emptyResult);
                }
            }
            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = firstResult.getIterationAggregator();
            if (aggregator == null) {
                if (results.size() == 1) {
                    answers.put(label, firstResult);
                    continue;
                }
                throw new IllegalStateException("No aggregator for " + firstResult);
            }

            // Note: should not use "results" here since the contents was just updated by "put" above
            Result aggregate = aggregator.aggregate(allSecondary.get(label));
            answers.put(label, aggregate);
        }

        for (String label : benchmarkResults.keys()) {
            Aggregator<Result> aggregator = null;
            Collection<Result> results = new ArrayList<>();
            for (Result r : benchmarkResults.get(label)) {
                if (r.getRole().isSecondary() && !r.getRole().isDerivative()) {
                    results.add(r);
                    aggregator = r.getIterationAggregator();
                }
            }
            if (aggregator != null) {
                answers.put(label, aggregator.aggregate(results));
            }
        }

        // put all secondary derivative results on top: from primaries
        answers.putAll(produceDerivative(getPrimaryResult()));

        // add all derivative results on top: from secondaries
        Map<String, Result> adds = new HashMap<>();
        for (Result r : answers.values()) {
            adds.putAll(produceDerivative(r));
        }
        answers.putAll(adds);

        return answers;
    }

    private Map<String, Result> produceDerivative(Result r) {
        Map<String, Result> map = new HashMap<>();
        for (Object rr : r.getDerivativeResults()) {
            map.put(((Result) rr).getLabel(), (Result) rr);
        }
        return map;
    }

    public String getScoreUnit() {
        return getPrimaryResult().getScoreUnit();
    }

    public BenchmarkParams getParams() {
        return params;
    }
}
