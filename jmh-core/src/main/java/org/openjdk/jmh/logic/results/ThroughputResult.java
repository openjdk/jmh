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
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com
 */
public class ThroughputResult extends Result {

    /** Total number of operations during iteration */
    private final long operations;
    /** Duration of iteration in NanoSeconds */
    private final long durationNs;
    /** The TimeUnit to use when calculating the score */
    private final TimeUnit outputTimeUnit;

    /**
     * Sets up the result with the default output unit MilliSeconds
     *
     * @param operations Total number of operations during iteration
     * @param durationNs   Duration of iteration in NanoSeconds
     */
    public ThroughputResult(ResultRole role, String label, long operations, long durationNs) {
        this(role, label, operations, durationNs, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets up the result
     *
     * @param operations     Total number of operations during iteration
     * @param durationNs       Duration of iteration in NanoSeconds
     * @param outputTimeUnit The TimeUnit to use when calculating the score
     */
    public ThroughputResult(ResultRole role, String label, long operations, long durationNs, TimeUnit outputTimeUnit) {
        this(role, label, operations, durationNs, outputTimeUnit, null);
    }

    ThroughputResult(ResultRole role, String label, long operations, long durationNs, TimeUnit outputTimeUnit, Statistics stats) {
        super(role, label, stats);
        this.operations = operations;
        this.durationNs = durationNs;
        this.outputTimeUnit = outputTimeUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getScoreUnit() {
        return "ops/" + TimeValue.tuToString(outputTimeUnit);
    }

    /** {@inheritDoc} */
    @Override
    public double getScore() {
        return 1.0D * operations * outputTimeUnit.toNanos(1) / durationNs;
    }

    @Override
    public Aggregator getIterationAggregator() {
        // compute sum
        return new Aggregator<ThroughputResult>() {
            @Override
            public Result aggregate(Collection<ThroughputResult> results) {
                ListStatistics stat = new ListStatistics();
                ResultRole mode = null;
                String label = null;
                double operations = 0;
                TimeUnit tu = null;

                final long normalizedDuration = TimeUnit.MINUTES.toNanos(1);

                for (ThroughputResult r : results) {
                    stat.addValue(r.getScore());
                    mode = r.role;
                    tu = r.outputTimeUnit;
                    label = r.label;
                    operations += 1.0D * r.operations * normalizedDuration / r.durationNs;
                }

                // care about long underflow/overflow
                if (Long.MIN_VALUE < operations && operations < Long.MAX_VALUE) {
                    return new ThroughputResult(mode, label, (long) operations, normalizedDuration, tu, stat);
                } else {
                    throw new IllegalStateException("Internal error: the operation count does not fit into long: " + operations);
                }
            }
        };
    }

    @Override
    public Aggregator getRunAggregator() {
        // compute mean
        return new Aggregator<ThroughputResult>() {
            @Override
            public Result aggregate(Collection<ThroughputResult> results) {
                ListStatistics stat = new ListStatistics();
                ResultRole role = null;
                String label = null;
                double operations = 0;
                TimeUnit tu = null;

                final long normalizedDuration = TimeUnit.MINUTES.toNanos(1);

                for (ThroughputResult r : results) {
                    stat.addValue(r.getScore());
                    role = r.role;
                    tu = r.outputTimeUnit;
                    label = r.label;
                    operations += 1.0D * r.operations * normalizedDuration / r.durationNs;
                }
                operations /= results.size();

                // care about long underflow/overflow
                if (Long.MIN_VALUE < operations && operations < Long.MAX_VALUE) {
                    return new ThroughputResult(role, label, (long) operations, normalizedDuration, tu, stat);
                } else {
                    throw new IllegalStateException("Internal error: the operation count does not fit into long: " + operations);
                }
            }
        };
    }

}
