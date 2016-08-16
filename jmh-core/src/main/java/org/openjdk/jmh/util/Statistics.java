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
package org.openjdk.jmh.util;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;


public interface Statistics extends Serializable, StatisticalSummary, Comparable<Statistics> {

    /**
     * Gets the confidence interval at given confidence level.
     * @param confidence confidence level (e.g. 0.95)
     * @return the interval in which mean lies with the given confidence level
     */
    double[] getConfidenceIntervalAt(double confidence);

    /**
     * Gets the mean error at given confidence level.
     * @param confidence confidence level (e.g. 0.95)
     * @return the mean error with the given confidence level
     */
    double getMeanErrorAt(double confidence);

    /**
     * Checks if this statistics statistically different from the given one
     * with the given confidence level.
     * @param other statistics to test against
     * @param confidence confidence level (e.g. 0.95)
     * @return true, if mean difference is statistically significant
     */
    boolean isDifferent(Statistics other, double confidence);

    /**
     * Compares this statistics to another one.
     * Follows the contract of {@link Comparable}.
     *
     * @param other statistics to compare against
     * @return a negative integer, zero, or a positive integer as this statistics
     *          is less than, equal to, or greater than the specified statistics.
     */
    int compareTo(Statistics other);

    /**
     * Compares this statistics to another one.
     * Follows the contract of {@link Comparable}.
     *
     * @param other statistics to compare against
     * @param confidence confidence level (e.g. 0.99)
     * @return a negative integer, zero, or a positive integer as this statistics
     *          is less than, equal to, or greater than the specified statistics.
     */

    int compareTo(Statistics other, double confidence);

    /**
     * Returns the maximum for this statistics.
     * @return maximum
     */
    double getMax();

    /**
     * Returns the minimum for this statistics.
     * @return minimum
     */
    double getMin();

    /**
     * Returns the arithmetic mean for this statistics.
     * @return arithmetic mean
     */
    double getMean();

    /**
     * Returns the number of samples in this statistics.
     * @return number of samples
     */
    long getN();

    /**
     * Returns the sum of samples in this statistics.
     * @return sum
     */
    double getSum();

    /**
     * Returns the standard deviation for this statistics.
     * @return standard deviation
     */
    double getStandardDeviation();

    /**
     * Returns the variance for this statistics.
     * @return variance
     */
    double getVariance();

    /**
     * Returns the percentile at given rank.
     * @param rank the rank, [0..100]
     * @return percentile
     */
    double getPercentile(double rank);

    /**
     * Returns the histogram for this statistics. The histogram bin count would
     * be equal to number of levels, minus one; so that each i-th bin is the
     * number of samples in [i-th, (i+1)-th) levels.
     *
     * @param levels levels
     * @return histogram data
     */
    int[] getHistogram(double[] levels);

    /**
     * Returns the raw data for this statistics. This data can be useful for
     * custom postprocessing and statistics computations.  Note, that values of
     * multiple calls may not be unique. Ordering of the values is not specified.
     *
     * @return iterator to raw data. Each item is pair of actual value and
     *          number of occurrences of this value.
     */
    Iterator<Entry<Double, Long>> getRawData();
}
