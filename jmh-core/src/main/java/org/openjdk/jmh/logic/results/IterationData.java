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
package org.openjdk.jmh.logic.results;

import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.IterationParams;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.TreeMultimap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Class contains all info returned by microbenchmark iteration or/and collected during microbenchmark iteration.
 *
 * @author sergey.kuksenko@oracle.com
 */
public class IterationData implements Serializable {

    private final BenchmarkRecord benchmark;
    private final IterationParams params;
    private final List<Result> primaryResults;
    private final List<ProfilerResult> profilerResults;
    private String scoreUnit;

    public IterationData(BenchmarkRecord benchmark, IterationParams params) {
        this.benchmark = benchmark;
        this.params = params;
        this.primaryResults = new ArrayList<Result>(params.getThreads());
        this.profilerResults = new ArrayList<ProfilerResult>();
    }

    public void addResult(Result result) {
        if (scoreUnit == null) {
            scoreUnit = result.getScoreUnit();
        } else {
            if (!scoreUnit.equals(result.getScoreUnit())) {
                throw new IllegalStateException("Adding the result with another score unit!");
            }
        }

//        if (label != null) {
//            if (!label.equals(result.getLabel())) {
//                throw new IllegalStateException("Aggregating the results with different labels");
//            }
//        } else {
//            label = r.label;
//        }

        primaryResults.add(result);

        // FIXME: Take care of secondary results and separate them by label
    }

    public List<Result> getPrimaryResults() {
        return primaryResults;
    }

    public Result getPrimaryResult() {
        Multimap<String, Result> results = new TreeMultimap<String, Result>();
        for (Result r : primaryResults) {
            results.put(r.getLabel(), r);
        }

        Result next = primaryResults.iterator().next();

        @SuppressWarnings("unchecked")
        Aggregator<Result> aggregator = next.getIterationAggregator();
        return aggregator.aggregate(primaryResults);
    }

    public boolean isResultsEmpty() {
        return primaryResults.isEmpty();
    }

    public void clearResults() {
        primaryResults.clear();
    }

    public BenchmarkRecord getBenchmark() {
        return benchmark;
    }

    public IterationParams getParams() {
        return params;
    }

    public void addProfileResult(ProfilerResult profilerResult) {
        profilerResults.add(profilerResult);
    }

    public List<ProfilerResult> getProfilerResults() {
        return profilerResults;
    }

    public String getScoreUnit() {
        return scoreUnit;
    }

    public String toPrintable() {
        return getPrimaryResult().toString();
//        if (primaryResults.size() > 1) {
//            boolean first = true;
//            StringBuilder sb = new StringBuilder();
//            String unit = "";
//            for (Map.Entry<String, Result> res : primaryResults.entrySet()) {
//                if (!first) {
//                    sb.append(", ");
//                }
//                first = false;
//
//                // rough estimate
//                int threads = primaryResults.get(res.getKey()).size();
//
//                sb.append(res.getKey());
//                sb.append("{t=").append(threads).append("}");
//                sb.append(" = ");
//                sb.append(res.getValue().toPrintableScore());
//
//                unit = res.getValue().getScoreUnit();
//            }
//            sb.append(" ");
//            sb.append(unit);
//            return sb.toString();
//        } else if (result.size() == 1) {
//            return result.values().iterator().next().toString();
//        } else {
//            return "N/A";
//        }
    }

}
