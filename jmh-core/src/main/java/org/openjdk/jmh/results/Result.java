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

import org.openjdk.jmh.util.Deduplicator;
import org.openjdk.jmh.util.ScoreFormatter;
import org.openjdk.jmh.util.SingletonStatistics;
import org.openjdk.jmh.util.Statistics;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

/**
 * Base class for all types of results that can be returned by a benchmark.
 */
public abstract class Result<T extends Result<T>> implements Serializable {
    private static final long serialVersionUID = -7332879501317733312L;
    private static final Deduplicator<String> DEDUP = new Deduplicator<>();

    protected final ResultRole role;
    protected final String label;
    protected final String unit;
    protected final Statistics statistics;
    protected final AggregationPolicy policy;

    public Result(ResultRole role, String label, Statistics s, String unit, AggregationPolicy policy) {
        this.role = role;
        this.label = DEDUP.dedup(label);
        this.unit = DEDUP.dedup(unit);
        this.statistics = s;
        this.policy = policy;
    }

    protected static Statistics of(double v) {
        return new SingletonStatistics(v);
    }

    /**
     * Return the result label.
     * @return result label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Return the result role.
     * @return result role
     */
    public ResultRole getRole() {
        return role;
    }

    /**
     * Return the statistics holding the subresults' values.
     *
     * <p>This method returns raw samples. The aggregation policy decides how to get the score
     * out of these raw samples. Use {@link #getScore()}, {@link #getScoreError()}, and
     * {@link #getScoreConfidence()} for scalar results.</p>
     *
     * @return statistics
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * The unit of the score for this result.
     *
     * @return String representation of the unit
     */
    public final String getScoreUnit() {
        return unit;
    }

    /**
     * The score for this result.
     *
     * @return double representing the score
     * @see #getScoreError()
     */
    public double getScore() {
        switch (policy) {
            case AVG:
                return statistics.getMean();
            case SUM:
                return statistics.getSum();
            case MAX:
                return statistics.getMax();
            case MIN:
                return statistics.getMin();
            default:
                throw new IllegalStateException("Unknown aggregation policy: " + policy);
        }
    }

    /**
     * The score error for this result.
     * @return score error, if available
     * @see #getScore()
     */
    public double getScoreError() {
        switch (policy) {
            case AVG:
                return statistics.getMeanErrorAt(0.999);
            case SUM:
            case MIN:
            case MAX:
                return Double.NaN;
            default:
                throw new IllegalStateException("Unknown aggregation policy: " + policy);
        }
    }

    /**
     * The score confidence interval for this result.
     * @return score confidence interval, if available; if not, the CI will match {@link #getScore()}
     * @see #getScore()
     */
    public double[] getScoreConfidence() {
        switch (policy) {
            case AVG:
                return statistics.getConfidenceIntervalAt(0.999);
            case MAX:
            case MIN:
            case SUM:
                double score = getScore();
                return new double[] {score, score};
            default:
                throw new IllegalStateException("Unknown aggregation policy: " + policy);
        }
    }

    /**
     * Get number of samples in the current result.
     * @return number of samples
     */
    public long getSampleCount() {
        return statistics.getN();
    }

    /**
     * Thread aggregator combines the thread results into iteration result.
     * @return thread aggregator
     */
    protected abstract Aggregator<T> getThreadAggregator();

    /**
     * Iteration aggregator combines the iteration results into benchmar result.
     * @return iteration aggregator
     */
    protected abstract Aggregator<T> getIterationAggregator();

    /**
     * Returns "0" result. This is used for un-biased aggregation of secondary results.
     * For instance, profilers might omit results in some iterations, thus we should pretend there were 0 results.
     * @return result that represents "empty" result, null if no sensible "empty" result can be created
     */
    protected T getZeroResult() {
        return null;
    }

    /**
     * Get derivative results for this result. These do not participate in aggregation,
     * and computed on the spot from the aggregated result.
     * @return
     */
    protected Collection<? extends Result> getDerivativeResults() {
        return Collections.emptyList();
    }

    /**
     * Result as represented by a String.
     *
     * @return String with the result and unit
     */
    @Override
    public String toString() {
        if (!Double.isNaN(getScoreError()) && !ScoreFormatter.isApproximate(getScore())) {
            return String.format("%s \u00B1(99.9%%) %s %s",
                    ScoreFormatter.format(getScore()),
                    ScoreFormatter.formatError(getScoreError()),
                    getScoreUnit());
        } else {
            return String.format("%s %s",
                    ScoreFormatter.format(getScore()),
                    getScoreUnit());
        }
    }

    /**
     * Print extended result information
     * @return String with extended info
     */
    public String extendedInfo() {
        return simpleExtendedInfo();
    }

    protected String simpleExtendedInfo() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Statistics stats = getStatistics();
        if (stats.getN() > 2 && !ScoreFormatter.isApproximate(getScore())) {
            double[] interval = getScoreConfidence();
            pw.println(String.format("  %s \u00B1(99.9%%) %s %s [%s]",
                    ScoreFormatter.format(getScore()),
                    ScoreFormatter.formatError((interval[1] - interval[0]) / 2),
                    getScoreUnit(), policy));
            pw.println(String.format("  (min, avg, max) = (%s, %s, %s), stdev = %s%n" +
                    "  CI (99.9%%): [%s, %s] (assumes normal distribution)",
                    ScoreFormatter.format(stats.getMin()),
                    ScoreFormatter.format(stats.getMean()),
                    ScoreFormatter.format(stats.getMax()),
                    ScoreFormatter.formatError(stats.getStandardDeviation()),
                    ScoreFormatter.format(interval[0]),
                    ScoreFormatter.format(interval[1]))
            );
        } else {
            pw.println(String.format("  %s %s", ScoreFormatter.format(stats.getMean()), getScoreUnit()));
        }
        pw.close();
        return sw.toString();
    }

    protected String distributionExtendedInfo() {
        Statistics stats = getStatistics();

        StringBuilder sb = new StringBuilder();

        if (stats.getN() > 2) {
            sb.append("  N = ").append(stats.getN()).append("\n");

            double[] interval = stats.getConfidenceIntervalAt(0.999);
            sb.append(String.format("  mean = %s \u00B1(99.9%%) %s",
                    ScoreFormatter.format(10, stats.getMean()),
                    ScoreFormatter.formatError((interval[1] - interval[0]) / 2)
            ));
            sb.append(" ").append(getScoreUnit()).append("\n");

            printHisto(stats, sb);
            printPercentiles(stats, sb);
        }

        return sb.toString();
    }

    private void printPercentiles(Statistics stats, StringBuilder sb) {
        sb.append("\n  Percentiles, ").append(getScoreUnit()).append(":\n");
        for (double p : new double[]{0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 0.99999, 0.999999, 1.0}) {
            sb.append(String.format("    %11s = %s %s\n",
                    "p(" + String.format("%.4f", p * 100) + ")",
                    ScoreFormatter.format(10, stats.getPercentile(p * 100)),
                    getScoreUnit()
                    ));
        }
    }

    static class LazyProps {
        private static final int MIN_HISTOGRAM_BINS = Integer.getInteger("jmh.histogramBins", 10);
    }

    private void printHisto(Statistics stats, StringBuilder sb) {
        sb.append("\n  Histogram, ").append(getScoreUnit()).append(":\n");

        double min = stats.getMin();
        double max = stats.getMax();

        double binSize = Math.pow(10, Math.floor(Math.log10(max - min)));
        min = Math.floor(min / binSize) * binSize;
        max =  Math.ceil(max / binSize) * binSize;
        double range = max - min;

        double[] levels;
        if (range > 0) {
            while ((range / binSize) < LazyProps.MIN_HISTOGRAM_BINS) {
                binSize /= 2;
            }

            int binCount = Math.max(2, (int) Math.ceil(range / binSize));

            levels = new double[binCount];
            for (int c = 0; c < binCount; c++) {
                levels[c] = min + (c * binSize);
            }
        } else {
            levels = new double[] {
                    stats.getMin() - Math.ulp(stats.getMin()),
                    stats.getMax() + Math.ulp(stats.getMax())
            };
        }

        int width = ScoreFormatter.format(1, max).length();

        int[] histo = stats.getHistogram(levels);
        for (int c = 0; c < levels.length - 1; c++) {
            sb.append(String.format("    [%" + width + "s, %" + width + "s) = %d %n",
                    ScoreFormatter.formatExact(width, levels[c]),
                    ScoreFormatter.formatExact(width, levels[c + 1]),
                    histo[c]));
        }
    }

}
