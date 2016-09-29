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

import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Statistics;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Result class that stores average operation time.
 */
public class AverageTimeResult extends Result<AverageTimeResult> {
    private static final long serialVersionUID = 6937689337229703312L;

    public AverageTimeResult(ResultRole mode, String label, double operations, long durationNs, TimeUnit tu) {
        this(mode, label,
                of(durationNs / (operations * TimeUnit.NANOSECONDS.convert(1, tu))),
                TimeValue.tuToString(tu) + "/op");
    }

    AverageTimeResult(ResultRole mode, String label, Statistics value, String unit) {
        super(mode, label, value, unit, AggregationPolicy.AVG);
    }

    @Override
    protected Aggregator<AverageTimeResult> getThreadAggregator() {
        return new ResultAggregator();
    }

    @Override
    protected Aggregator<AverageTimeResult> getIterationAggregator() {
        return new ResultAggregator();
    }

    static class ResultAggregator implements Aggregator<AverageTimeResult> {
        @Override
        public AverageTimeResult aggregate(Collection<AverageTimeResult> results) {
            ListStatistics stat = new ListStatistics();
            for (AverageTimeResult r : results) {
                stat.addValue(r.getScore());
            }
            return new AverageTimeResult(
                    AggregatorUtils.aggregateRoles(results),
                    AggregatorUtils.aggregateLabels(results),
                    stat,
                    AggregatorUtils.aggregateUnits(results)
            );
        }
    }

}
