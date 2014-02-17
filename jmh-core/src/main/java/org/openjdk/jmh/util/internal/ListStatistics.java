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

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculate statistics over a list of doubles.
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com
 */
public class ListStatistics extends AbstractStatistics {

    private static final long serialVersionUID = -90642978235578197L;

    private final List<Double> values;

    public ListStatistics() {
        values = new ArrayList<Double>();
    }

    public ListStatistics(double[] samples) {
        this();
        for (double d : samples) {
            addValue(d);
        }
    }

    public ListStatistics(long[] samples) {
        this();
        for (long l : samples) {
            addValue((double) l);
        }
    }

    public void addValue(double d) {
        values.add(d);
    }

    @Override
    public double getMax() {
        if (getN() > 0) {
            double m = Double.NEGATIVE_INFINITY;
            for (double d : values) {
                m = Math.max(m, d);
            }
            return m;
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getMin() {
        if (getN() > 0) {
            double m = Double.POSITIVE_INFINITY;
            for (double d : values) {
                m = Math.min(m, d);
            }
            return m;
        } else {
            return Double.NaN;
        }
    }

    @Override
    public long getN() {
        return values.size();
    }

    @Override
    public double getSum() {
        if (getN() > 0) {
            double s = 0;
            for (double d : values) {
                s += d;
            }
            return s;
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getPercentile(double rank) {
        if (values.size() == 0) {
            return Double.NaN;
        }

        if (rank == 0) {
            return getMin();
        }

        double[] vs = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vs[i] = values.get(i);
        }

        Percentile p = new Percentile();
        return p.evaluate(vs, rank);
    }

    @Override
    public double getVariance() {
        if (getN() > 0) {
            double v = 0;
            double m = getMean();
            for (double d : values) {
                v += Math.pow(d - m, 2);
            }
            return v / (getN() - 1);
        } else {
            return Double.NaN;
        }
    }

}
