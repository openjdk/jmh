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

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.inference.TestUtils;

public abstract class AbstractStatistics implements Statistics {
    private static final long serialVersionUID = 1536835581997509117L;

    /**
     * Returns the interval c1, c2 of which there's an 1-alpha
     * probability of the mean being within the interval.
     *
     * @param confidence level
     * @return the confidence interval
     */
    @Override
    public double[] getConfidenceIntervalAt(double confidence) {
        double[] interval = new double[2];

        if (getN() <= 2) {
            interval[0] = interval[1] = Double.NaN;
            return interval;
        }

        TDistribution tDist = new TDistribution(getN() - 1);
        double a = tDist.inverseCumulativeProbability(1 - (1 - confidence) / 2);
        interval[0] = getMean() - a * getStandardDeviation() / Math.sqrt(getN());
        interval[1] = getMean() + a * getStandardDeviation() / Math.sqrt(getN());

        return interval;
    }

    @Override
    public boolean isDifferent(Statistics other, double confidence) {
        return TestUtils.tTest(this, other, 1 - confidence);
    }

    @Override
    public double getMeanErrorAt(double confidence) {
        if (getN() <= 2) return Double.NaN;
        TDistribution tDist = new TDistribution(getN() - 1);
        double a = tDist.inverseCumulativeProbability(1 - (1 - confidence) / 2);
        return a * getStandardDeviation() / Math.sqrt(getN());
    }

    @Override
    public String toString() {
        return "N:" + getN() + " Mean: " + getMean()
                + " Min: " + getMin() + " Max: " + getMax()
                + " StdDev: " + getStandardDeviation();
    }

    @Override
    public double getMean() {
        if (getN() > 0) {
            return getSum() / getN();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

    @Override
    public int compareTo(Statistics other, double confidence) {
        if (isDifferent(other, confidence)) {
            double t = getMean();
            double o = other.getMean();
            return (t > o) ? -1 : 1;
        } else {
            return 0;
        }
    }

    @Override
    public int compareTo(Statistics other) {
        return compareTo(other, 0.99);
    }
}
