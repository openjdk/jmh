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
package org.openjdk.jmh.util.internal;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import java.io.Serializable;

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

    double getStandardDeviation();

    double getMax();

    double getMin();

    double getMean();

    long getN();

    double getSum();

    double getVariance();

    double getPercentile(double rank);
}
