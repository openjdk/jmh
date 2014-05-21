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

import org.openjdk.jmh.util.internal.ListStatistics;
import org.openjdk.jmh.util.internal.Statistics;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all types of results that can be returned by a microbenchmark.
 */
public abstract class Result<T extends Result<T>> implements Serializable {

    protected final ResultRole role;
    protected final String label;
    protected final TimeUnit outputTimeUnit;
    protected final Statistics statistics;
    protected final AggregationPolicy policy;

    public Result(ResultRole role, String label, Statistics s, TimeUnit outputTimeUnit, AggregationPolicy policy) {
        this.role = role;
        this.label = label;
        this.outputTimeUnit = outputTimeUnit;
        this.statistics = s;
        this.policy = policy;
    }

    protected static Statistics of(double v) {
        ListStatistics s = new ListStatistics();
        s.addValue(v);
        return s;
    }

    /**
     * The unit of the score for one iteration.
     *
     * @return String representing the unit of the score
     */
    public abstract String getScoreUnit();

    /**
     * The score of one iteration.
     *
     * @return double representing the score
     */
    public double getScore() {
        switch (policy) {
            case AVG:
                return statistics.getMean();
            case SUM:
                return statistics.getSum();
            default:
                throw new IllegalStateException("Unknown aggregation policy: " + policy);
        }
    }

    public abstract Aggregator<T> getIterationAggregator();

    public abstract Aggregator<T> getRunAggregator();

    /**
     * Result as represented by a String.
     *
     * @return String with the result and unit
     */
    @Override
    public String toString() {
        return String.format("%.3f %s", getScore(), getScoreUnit());
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public String extendedInfo(String label) {
        return simpleExtendedInfo(label);
    }

    protected String simpleExtendedInfo(String label) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Statistics stats = getStatistics();
        if (stats.getN() > 2) {
            double[] interval = stats.getConfidenceIntervalAt(0.999);
            pw.println(String.format("Result%s: %.3f \u00B1(99.9%%) %.3f %s",
                    (label == null) ? "" : " \"" + label + "\"",
                    stats.getMean(), (interval[1] - interval[0]) / 2,
                    getScoreUnit()));
            pw.println(String.format("  Statistics: (min, avg, max) = (%.3f, %.3f, %.3f), stdev = %.3f%n" +
                    "  Confidence interval (99.9%%): [%.3f, %.3f]",
                    stats.getMin(), stats.getMean(), stats.getMax(), stats.getStandardDeviation(),
                    interval[0], interval[1]));
        } else {
            pw.println(String.format("Run result: %.2f (<= 2 iterations)", stats.getMean()));
        }
        pw.close();
        return sw.toString();
    }

    protected String percentileExtendedInfo(String label) {
        Statistics stats = getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("  Samples, N = ").append(stats.getN()).append("\n");

        if (stats.getN() > 2) {
            double[] interval = stats.getConfidenceIntervalAt(0.999);
            sb.append(String.format("        mean = %10.3f \u00B1(99.9%%) %.3f",
                    stats.getMean(),
                    (interval[1] - interval[0]) / 2
            ));
        } else {
            sb.append(String.format("        mean = %10.3f (<= 2 iterations)",
                    stats.getMean()
            ));
        }
        sb.append(" ").append(getScoreUnit()).append("\n");

        sb.append(String.format("         min = %10.3f %s\n", stats.getMin(), getScoreUnit()));

        for (double p : new double[] {0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 0.99999, 0.999999}) {
            sb.append(String.format("  %9s = %10.3f %s\n",
                    "p(" + String.format("%7.4f", p*100) + ")",
                    stats.getPercentile(p*100),
                    getScoreUnit()
            ));
        }

        sb.append(String.format("         max = %10.3f %s\n", stats.getMax(), getScoreUnit()));

        return sb.toString();
    }

    public String getLabel() {
        return label;
    }

    public ResultRole getRole() {
        return role;
    }
}
