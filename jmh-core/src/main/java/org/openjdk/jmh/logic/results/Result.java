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

import org.openjdk.jmh.util.internal.Statistics;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all types of results that can be returned by a microbenchmark.
 *
 * @author staffan.friberg@oracle.com
 */
public abstract class Result<T extends Result<T>> implements Serializable {

    protected final ResultRole role;
    protected final String label;
    protected final Statistics statistics;

    public Result(ResultRole role, String label, Statistics statistics) {
        this.role = role;
        this.label = label;
        this.statistics = statistics == null ? new Statistics() : statistics;
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
    public abstract double getScore();

    public abstract Aggregator<T> getIterationAggregator();

    public abstract Aggregator<T> getRunAggregator();

    /**
     * Result as represented by a String.
     *
     * @return String with the result and unit
     */
    @Override
    public String toString() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(3);
        nf.setGroupingUsed(false);

        return nf.format(getScore()) + ' ' + getScoreUnit();
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public String extendedInfo(String label) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        if (statistics.getN() > 2) {
            double[] interval95 = statistics.getConfidenceInterval(0.05);
            double[] interval99 = statistics.getConfidenceInterval(0.01);
            pw.println(String.format("Result %s: %.3f \u00B1(95%%) %.3f \u00B1(99%%) %.3f %s",
                    (label == null) ? "" : "\"" + label + "\"",
                    (interval95[0] + interval95[1]) / 2, (interval95[1] - interval95[0]) / 2, (interval99[1] - interval99[0]) / 2,
                    getScoreUnit()));
            pw.println(String.format("  Statistics: (min, avg, max) = (%.3f, %.3f, %.3f), stdev = %.3f",
                    statistics.getMin(), statistics.getMean(), statistics.getMax(), statistics.getStandardDeviation()));
            pw.println(String.format("  Confidence intervals: 95%% [%.3f, %.3f], 99%% [%.3f, %.3f]",
                    interval95[0], interval95[1], interval99[0], interval99[1]));
        } else {
            pw.println(String.format("Run result: %.2f (<= 2 iterations)", statistics.getMean()));
        }
        pw.close();
        return sw.toString();
    }



    public String getLabel() {
        return label;
    }

    public ResultRole getRole() {
        return role;
    }
}
