/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Statistics;

import java.util.Collection;

public class ScalarResult extends Result<ScalarResult> {
    private static final long serialVersionUID = 3407232747805728586L;

    public ScalarResult(String label, double n, String unit, AggregationPolicy policy) {
        this(label, of(n), unit, policy);
    }

    ScalarResult(String label, Statistics s, String unit, AggregationPolicy policy) {
        super(ResultRole.SECONDARY, label, s, unit, policy);
    }

    @Override
    protected Aggregator<ScalarResult> getThreadAggregator() {
        return new ScalarResultAggregator();
    }

    @Override
    protected Aggregator<ScalarResult> getIterationAggregator() {
        return new ScalarResultAggregator();
    }

    @Override
    protected ScalarResult getZeroResult() {
        return new ScalarResult(label, 0, unit, policy);
    }

    static class ScalarResultAggregator implements Aggregator<ScalarResult> {
        @Override
        public ScalarResult aggregate(Collection<ScalarResult> results) {
            ListStatistics stats = new ListStatistics();
            for (ScalarResult r : results) {
                stats.addValue(r.getScore());
            }
            return new ScalarResult(
                    AggregatorUtils.aggregateLabels(results),
                    stats,
                    AggregatorUtils.aggregateUnits(results),
                    AggregatorUtils.aggregatePolicies(results)
            );
        }
    }
}
