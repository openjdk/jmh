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
 * Result class that stores once operation execution time.
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
public class SingleShotTime extends Result {

    /** Duration of iteration in NanoSeconds */
    private final long duration;
    /** The TimeUnit to use when calculating the time */
    private final TimeUnit outputTimeUnit;

    /**
     * Sets up the result
     *
     * @param duration       Duration of iteration in NanoSeconds
     * @param outputTimeUnit The TimeUnit to use when calculating the score
     */
    public SingleShotTime(ResultRole mode, String label, long duration, TimeUnit outputTimeUnit) {
        this(mode, label, duration, outputTimeUnit, null);
    }

    SingleShotTime(ResultRole mode, String label, long duration, TimeUnit outputTimeUnit, Statistics stat) {
        super(mode, label, stat);
        this.duration = duration;
        this.outputTimeUnit = outputTimeUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getScoreUnit() {
        return TimeValue.tuToString(outputTimeUnit);
    }

    /** {@inheritDoc} */
    @Override
    public double getScore() {
        return (duration / (double) outputTimeUnit.toNanos(1)) ;
    }

    @Override
    public Aggregator getIterationAggregator() {
        return new AveragingAggregator();
    }

    @Override
    public Aggregator getRunAggregator() {
        return new AveragingAggregator();
    }

    /**
     * Averages the time on all levels.
     */
    public static class AveragingAggregator implements Aggregator<SingleShotTime> {
        @Override
        public Result aggregate(Collection<SingleShotTime> results) {
            ResultRole role = null;
            String label = null;
            ListStatistics stat = new ListStatistics();
            long duration = 0;
            TimeUnit tu = null;
            for (SingleShotTime r : results) {
                role = r.role;
                tu = r.outputTimeUnit;
                label = r.label;
                duration += r.duration;
                stat.addValue(r.getScore());
            }
            return new SingleShotTime(role, label, duration / results.size(), tu, stat);
        }

    }

}
