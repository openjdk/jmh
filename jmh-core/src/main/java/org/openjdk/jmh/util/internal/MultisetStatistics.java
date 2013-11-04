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

import java.util.Collections;
import java.util.Iterator;

public class MultisetStatistics extends AbstractStatistics {

    private final Multiset<Double> values;

    public MultisetStatistics() {
        values = new TreeMultiset<Double>();
    }

    public void addValue(double d, int count) {
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
    public int getN() {
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

    @Override
    public double getPercentile(double rank) {
        if (rank < 0.0d || rank > 100.0d)
            throw new IllegalArgumentException("Rank should be within [0; 100]");

        long thresh = (long) (values.size() * rank / 100.0);
        long cur = 0;
        for (double d : values.keys()) {
            cur += values.count(d);
            if (cur >= thresh) return d;
        }
        return Double.NaN;
    }

    @Override
    protected DoubleIterator valuesIterator() {
        return new DoubleIterator() {
            private Iterator<Double> current = values.keys().iterator();
            private int count;
            private Double val;

            private void ensureNonEmpty() {
                while (count == 0 && current.hasNext()) {
                    val = current.next();
                    count = values.count(val);
                }
            }

            @Override
            public boolean hasNext() {
                ensureNonEmpty();
                return (count > 0);
            }

            @Override
            public double next() {
                ensureNonEmpty();
                if (count > 0) {
                    count--;
                    return val;
                } else {
                    return Collections.<Double>emptyIterator().next();
                }
            }
        };
    }
}
