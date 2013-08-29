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
package org.openjdk.jmh.logic.results.internal;

import org.openjdk.jmh.logic.results.Aggregator;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.Statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Complete run result.
 * Contains the iteration results.
 *
 * @author anders.astrand@oracle.com
 * @author aleksey.shipilev@oracle.com
 */
public class RunResult implements Serializable {

    private static final long serialVersionUID = 6467912427356048369L;

    private final Collection<IterationResult> iterationResults;

    public RunResult(Collection<IterationResult> data) {
        this.iterationResults = data;
    }

    public Collection<IterationResult> getRawIterationResults() {
        return iterationResults;
    }

    public Result getPrimaryResult() {
        Result next = iterationResults.iterator().next().getPrimaryResult();

        @SuppressWarnings("unchecked")
        Aggregator<Result> aggregator = next.getRunAggregator();
        return aggregator.aggregate(getRawPrimaryResults());
    }

    public Collection<Result> getRawPrimaryResults() {
        Collection<Result> rs = new ArrayList<Result>();
        for (IterationResult k : iterationResults) {
            rs.add(k.getPrimaryResult());
        }
        return rs;
    }

    public Multimap<String, Result> getRawSecondaryResults() {
        Multimap<String, Result> rs = new HashMultimap<String, Result>();
        for (IterationResult k : iterationResults) {
            for (Map.Entry<String, Result> r : k.getSecondaryResults().entrySet()) {
                rs.put(r.getKey(), r.getValue());
            }
        }
        return rs;
    }

    public Map<String, Result> getSecondaryResults() {
        Multimap<String, Result> rs = getRawSecondaryResults();

        Map<String, Result> answers = new TreeMap<String, Result>();
        for (String k : rs.keys()) {
            Collection<Result> results = rs.get(k);
            Result next = results.iterator().next();

            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = next.getRunAggregator();
            answers.put(k, aggregator.aggregate(results));
        }

        return answers;
    }

    public String getScoreUnit() {
        return getPrimaryResult().getScoreUnit();
    }

    public Statistics getPrimaryStatistics() {
        Statistics s = new Statistics();
        for (IterationResult d : iterationResults) {
            s.addValue(d.getPrimaryResult().getScore());
        }
        return s;
    }

    public Map<String, Statistics> getSecondaryStatistics() {
        Map<String, Statistics> answer = new HashMap<String, Statistics>();
        for (IterationResult d : iterationResults) {
            for (Map.Entry<String, Result> e : d.getSecondaryResults().entrySet()) {
                Statistics s = answer.get(e.getKey());
                if (s == null) {
                    s = new Statistics();
                    answer.put(e.getKey(), s);
                }
                s.addValue(e.getValue().getScore());
            }
        }
        return answer;
    }

    public static RunResult merge(RunResult... rrs) {
        List<IterationResult> rs = new ArrayList<IterationResult>();
        for (RunResult rr : rrs) {
            if (rr == null) continue;
            rs.addAll(rr.getRawIterationResults());
        }
        return new RunResult(rs);
    }
}
