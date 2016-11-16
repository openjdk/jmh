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

import java.util.*;

public class MultisetStatistics extends AbstractStatistics {
    private static final long serialVersionUID = -4401871054963903938L;

    private final Multiset<Double> values;

    public MultisetStatistics() {
        values = new TreeMultiset<>();
    }

    public void addValue(double d, long count) {
        values.add(d, count);
    }

    @Override
    public double getMax() {
        double max = Double.NEGATIVE_INFINITY;
        for (double d : values.keys()) {
            max = Math.max(max, d);
        }
        return max;
    }

    @Override
    public double getMin() {
        double min = Double.POSITIVE_INFINITY;
        for (double d : values.keys()) {
            min = Math.min(min, d);
        }
        return min;
    }

    @Override
    public long getN() {
        return values.size();
    }

    @Override
    public double getSum() {
        double sum = 0;
        for (double d : values.keys()) {
            sum += d*values.count(d);
        }
        return sum;
    }

    private double get(long index) {
        long cur = 0;
        for (double d : values.keys()) {
            cur += values.count(d);
            if (cur >= index) return d;
        }
        return getMax();
    }

    @Override
    public double getPercentile(double rank) {
        if (rank < 0.0d || rank > 100.0d)
            throw new IllegalArgumentException("Rank should be within [0; 100]");

        if (rank == 0.0d) {
            return getMin();
        }

        double pos = rank * (values.size() + 1) / 100;
        double floorPos = Math.floor(pos);

        double flooredValue = get((long) floorPos);
        double nextValue = get((long) floorPos + 1);

        return flooredValue + (nextValue - flooredValue) * (pos - floorPos);
    }

    @Override
    public double getVariance() {
        if (getN() > 0) {
            double v = 0;
            double m = getMean();
            for (double d : values.keys()) {
                v += Math.pow(d - m, 2) * values.count(d);
            }
            return v / (getN() - 1);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public int[] getHistogram(double[] levels) {
        if (levels.length < 2) {
            throw new IllegalArgumentException("Expected more than two levels");
        }

        List<Double> vs = new ArrayList<>(values.keys());
        Collections.sort(vs);

        int[] result = new int[levels.length - 1];

        int c = 0;
        values: for (double v : vs) {
            while (levels[c] > v || v >= levels[c + 1]) {
                c++;
                if (c > levels.length - 2) break values;
            }
            result[c] += values.count(v);
        }

        return result;
    }

    @Override
    public Iterator<Map.Entry<Double, Long>> getRawData() {
        return values.entrySet().iterator();
    }
}
