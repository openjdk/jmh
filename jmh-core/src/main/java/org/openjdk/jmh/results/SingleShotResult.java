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
 * Result class that stores once operation execution time.
 */
public class SingleShotResult extends Result<SingleShotResult> {
    private static final long serialVersionUID = -1251578870918524737L;

    public SingleShotResult(ResultRole role, String label, long duration, TimeUnit outputTimeUnit) {
        this(role, label,
                of(1.0D * duration / TimeUnit.NANOSECONDS.convert(1, outputTimeUnit)),
                TimeValue.tuToString(outputTimeUnit) + "/op");
    }

    SingleShotResult(ResultRole mode, String label, Statistics s, String unit) {
        super(mode, label, s, unit, AggregationPolicy.AVG);
    }

    @Override
    public String extendedInfo() {
        return distributionExtendedInfo();
    }

    @Override
    protected Aggregator<SingleShotResult> getThreadAggregator() {
        return new AveragingAggregator();
    }

    @Override
    protected Aggregator<SingleShotResult> getIterationAggregator() {
        return new AveragingAggregator();
    }

    /**
     * Averages the time on all levels.
     */
    static class AveragingAggregator implements Aggregator<SingleShotResult> {
        @Override
        public SingleShotResult aggregate(Collection<SingleShotResult> results) {
            ListStatistics stat = new ListStatistics();
            for (SingleShotResult r : results) {
                stat.addValue(r.getScore());
            }
            return new SingleShotResult(
                    AggregatorUtils.aggregateRoles(results),
                    AggregatorUtils.aggregateLabels(results),
                    stat,
                    AggregatorUtils.aggregateUnits(results)
            );
        }

    }

}
