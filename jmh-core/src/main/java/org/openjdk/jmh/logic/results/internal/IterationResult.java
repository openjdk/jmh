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
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.TreeMultimap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Complete iteration result.
 * Contains the sub-thread results.
 *
 * @author anders.astrand@oracle.com
 * @author aleksey.shipilev@oracle.com
 */
public class IterationResult implements Serializable {

    private static final long serialVersionUID = 9128620268354234707L;

    /** Original list of Result */
    private final Multimap<String, Result> results;

    /** Aggregated result */
    private final Map<String, Result> result;

    /**
     * Constructor
     *
     * @param rrs Results from the threads
     */
    public IterationResult(List<Result> rrs) {
        this.results = new TreeMultimap<String, Result>();
        for (Result r : rrs) {
            results.put(r.getLabel(), r);
        }

        this.result = new TreeMap<String, Result>();
        if (!rrs.isEmpty()) {
            Result next = rrs.iterator().next();

            @SuppressWarnings("unchecked")
            Aggregator<Result> aggregator = next.getIterationAggregator();

            for (String k : results.keys()) {
                Result r = aggregator.aggregate(results.get(k));
                result.put(r.getLabel(), r);
            }
        }
    }

    public Multimap<String, Result> getSubresults() {
        return results;
    }

    public Map<String, Double> getScore() {
        Map<String, Double> r = new TreeMap<String, Double>();
        for (String k : result.keySet()) {
            r.put(k, result.get(k).getScore());
        }
        return r;
    }

    public Map<String, Result> getResult() {
        return result;
    }

    public String getScoreUnit() {
        return results.values().iterator().next().getScoreUnit();
    }

    public String toPrintable() {
        if (result.size() > 1) {
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            String unit = "";
            for (Map.Entry<String, Result> res : result.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;

                // rough estimate
                int threads = results.get(res.getKey()).size();

                sb.append(res.getKey());
                sb.append("{t=").append(threads).append("}");
                sb.append(" = ");
                sb.append(res.getValue().toPrintableScore());

                unit = res.getValue().getScoreUnit();
            }
            sb.append(" ");
            sb.append(unit);
            return sb.toString();
        } else if (result.size() == 1) {
            return result.values().iterator().next().toString();
        } else {
            return "N/A";
        }
    }
}
