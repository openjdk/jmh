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

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Calculate statistics with just a single value.
 */
public class SingletonStatistics extends AbstractStatistics {

    private static final long serialVersionUID = -90642978235578197L;

    private final double value;

    public SingletonStatistics(double value) {
        this.value = value;
    }

    @Override
    public double getMax() {
        return value;
    }

    @Override
    public double getMin() {
        return value;
    }

    @Override
    public long getN() {
        return 1;
    }

    @Override
    public double getSum() {
        return value;
    }

    @Override
    public double getPercentile(double rank) {
        return value;
    }

    @Override
    public double getVariance() {
        return Double.NaN;
    }

    @Override
    public int[] getHistogram(double[] levels) {
        int[] result = new int[levels.length - 1];
        for (int c = 0; c < levels.length - 1; c++) {
            if (levels[c] <= value && value < levels[c + 1]) {
                result[c] = 1;
                break;
            }
        }
        return result;
    }

    @Override
    public Iterator<Map.Entry<Double, Long>> getRawData() {
        return new SingletonStatisticsIterator();
    }

    private class SingletonStatisticsIterator implements Iterator<Map.Entry<Double, Long>> {
        private boolean entryReturned = false;

        @Override
        public boolean hasNext() {
            return !entryReturned;
        }

        @Override
        public Map.Entry<Double, Long> next() {
            entryReturned = true;
            return new AbstractMap.SimpleImmutableEntry<>(value, 1L);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Element cannot be removed.");
        }
    }
}
