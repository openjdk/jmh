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
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;

import java.io.Serializable;
import java.util.*;

/**
 * Class contains all info returned by benchmark iteration or/and collected during benchmark iteration.
 */
public class IterationResult implements Serializable {
    private static final long serialVersionUID = 960397066774710819L;

    private static final Multimap<String, Result> EMPTY_MAP = new TreeMultimap<>();
    private static final List<Result> EMPTY_LIST = Collections.emptyList();

    private final BenchmarkParams benchmarkParams;
    private final IterationParams params;
    private final IterationResultMetaData metadata;
    private Collection<Result> primaryResults;
    private Multimap<String, Result> secondaryResults;

    public IterationResult(BenchmarkParams benchmarkParams, IterationParams params, IterationResultMetaData md) {
        this.benchmarkParams = benchmarkParams;
        this.params = params;
        this.metadata = md;
        this.primaryResults = EMPTY_LIST;
        this.secondaryResults = EMPTY_MAP;
    }

    public IterationResultMetaData getMetadata() {
        return metadata;
    }

    public void addResults(Collection<? extends Result> rs) {
        for (Result r : rs) {
            addResult(r);
        }
    }

    public void addResult(Result result) {
        if (result.getRole().isPrimary()) {
            if (primaryResults == EMPTY_LIST) {
                primaryResults = Collections.singleton(result);
            } else if (primaryResults.size() == 1) {
                List<Result> newResults = new ArrayList<>(2);
                newResults.addAll(primaryResults);
                newResults.add(result);
                primaryResults = newResults;
            } else {
                primaryResults.add(result);
            }
        }

        if (result.getRole().isSecondary()) {
            if (secondaryResults == EMPTY_MAP) {
                secondaryResults = new TreeMultimap<>();
            }
            secondaryResults.put(result.getLabel(), result);
        }
    }

    public Collection<Result> getRawPrimaryResults() {
        return primaryResults;
    }

    public Multimap<String, Result> getRawSecondaryResults() {
        return secondaryResults;
    }

    public Map<String, Result> getSecondaryResults() {
        Map<String, Result> answer = new TreeMap<>();
        for (String label : secondaryResults.keys()) {
            Collection<Result> results = secondaryResults.get(label);

            Result next = results.iterator().next();

            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = next.getThreadAggregator();
            Result result = aggregator.aggregate(results);
            answer.put(label, result);
        }

        // put all secondary derivative results on top: from primaries
        answer.putAll(produceDerivative(getPrimaryResult()));

        // add all secondary derivative results on top: from secondaries
        Map<String, Result> adds = new HashMap<>();
        for (Result r : answer.values()) {
            adds.putAll(produceDerivative(r));
        }
        answer.putAll(adds);

        return answer;
    }

    private Map<String, Result> produceDerivative(Result r) {
        Map<String, Result> map = new HashMap<>();
        for (Object rr : r.getDerivativeResults()) {
            map.put(((Result) rr).getLabel(), (Result) rr);
        }
        return map;
    }

    public Result getPrimaryResult() {
        @SuppressWarnings("unchecked")
        Aggregator<Result> aggregator = primaryResults.iterator().next().getThreadAggregator();
        return aggregator.aggregate(primaryResults);
    }

    public IterationParams getParams() {
        return params;
    }

    public BenchmarkParams getBenchmarkParams() {
        return benchmarkParams;
    }

    public String getScoreUnit() {
        return getPrimaryResult().getScoreUnit();
    }

}
