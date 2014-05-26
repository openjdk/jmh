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

import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.internal.ListStatistics;
import org.openjdk.jmh.util.internal.Statistics;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Result class that counts the number of operations performed during a specified unit of time.
 */
public class ThroughputResult extends Result {

    public ThroughputResult(ResultRole role, String label, long operations, long durationNs, TimeUnit outputTimeUnit) {
        this(role, label,
                of(1.0D * operations * TimeUnit.NANOSECONDS.convert(1, outputTimeUnit) / durationNs),
                outputTimeUnit, AggregationPolicy.SUM);
    }

    ThroughputResult(ResultRole role, String label, Statistics s, TimeUnit outputTimeUnit, AggregationPolicy policy) {
        super(role, label, s, outputTimeUnit, policy);
    }

    @Override
    public String getScoreUnit() {
        return "ops/" + TimeValue.tuToString(outputTimeUnit);
    }

    @Override
    public Aggregator getIterationAggregator() {
        return new ThroughputAggregator(AggregationPolicy.SUM);
    }

    @Override
    public Aggregator getRunAggregator() {
        return new ThroughputAggregator(AggregationPolicy.AVG);
    }

    static class ThroughputAggregator implements Aggregator<ThroughputResult> {
        private final AggregationPolicy policy;

        ThroughputAggregator(AggregationPolicy policy) {
            this.policy = policy;
        }

        @Override
        public Result aggregate(Collection<ThroughputResult> results) {
            ListStatistics stat = new ListStatistics();
            for (ThroughputResult r : results) {
                stat.addValue(r.getScore());
            }
            return new ThroughputResult(
                    Result.aggregateRoles(results),
                    Result.aggregateLabels(results),
                    stat,
                    Result.aggregateTimeunits(results),
                    policy
            );
        }
    }

}
