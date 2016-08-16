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

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Statistics
 */
public class TestSingletonStatistics {

    private static final double VALUE = 42.73638635;

    private static final ListStatistics listStats = new ListStatistics();
    private static final SingletonStatistics singStats = new SingletonStatistics(VALUE);

    @BeforeClass
    public static void setUpClass() throws Exception {
        listStats.addValue(VALUE);
    }

    /**
     * Test of getN method, of class Statistics.
     */
    @Test
    public strictfp void testGetN() {
        assertEquals(listStats.getN(), singStats.getN());
    }

    /**
     * Test of getSum method, of class Statistics.
     */
    @Test
    public strictfp void testGetSum() {
        assertEquals(listStats.getSum(), singStats.getSum(), 0.001);
    }

    /**
     * Test of getMean method, of class Statistics.
     */
    @Test
    public strictfp void testGetMean() {
        assertEquals(listStats.getMean(), singStats.getMean(), 0.001);
    }

    /**
     * Test of getMax method, of class Statistics.
     */
    @Test
    public strictfp void testGetMax() {
        assertEquals(listStats.getMax(), singStats.getMax(), 0.001);
    }

    /**
     * Test of getMin method, of class Statistics.
     */
    @Test
    public strictfp void testGetMin() {
        assertEquals(listStats.getMin(), singStats.getMin(), 0.001);
    }

    /**
     * Test of getVariance method, of class Statistics.
     */
    @Test
    public strictfp void testGetVariance() {
        assertEquals(listStats.getVariance(), singStats.getVariance(), 0.001);
    }

    /**
     * Test of getStandardDeviation method, of class Statistics.
     */
    @Test
    public strictfp void testGetStandardDeviation() {
        assertEquals(listStats.getStandardDeviation(), singStats.getStandardDeviation(), 0.001);
    }

    /**
     * Test of getConfidenceIntervalAt, of class Statistics
     */
    @Test
    public strictfp void testGetConfidenceInterval() {
        double[] listInterval = listStats.getConfidenceIntervalAt(0.999);
        double[] singInterval = singStats.getConfidenceIntervalAt(0.999);
        assertEquals(listInterval[0], singInterval[0], 0.001);
        assertEquals(listInterval[1], singInterval[1], 0.001);
    }

    @Test
    public strictfp void testPercentile_00() {
        assertEquals(listStats.getPercentile(0), singStats.getPercentile(0), 0.002);
    }

    @Test
    public strictfp void testPercentile_50() {
        assertEquals(listStats.getPercentile(50), singStats.getPercentile(50), 0.002);
    }

    @Test
    public strictfp void testPercentile_90() {
        assertEquals(listStats.getPercentile(90), singStats.getPercentile(90), 0.002);
    }

    @Test
    public strictfp void testPercentile_99() {
        assertEquals(listStats.getPercentile(99), singStats.getPercentile(99), 0.002);
    }

    @Test
    public strictfp void testPercentile_100() {
        assertEquals(listStats.getPercentile(100), singStats.getPercentile(100), 0.002);
    }

    @Test
    public strictfp void testSingle() {
        SingletonStatistics s = new SingletonStatistics(42.0D);

        Assert.assertEquals(1, s.getN());
        Assert.assertEquals(42.0D, s.getSum());
        Assert.assertEquals(42.0D, s.getMin());
        Assert.assertEquals(42.0D, s.getMax());
        Assert.assertEquals(42.0D, s.getMean());
        Assert.assertEquals(Double.NaN, s.getMeanErrorAt(0.5));
        Assert.assertEquals(Double.NaN, s.getVariance());
        Assert.assertEquals(Double.NaN, s.getStandardDeviation());
        Assert.assertEquals(Double.NaN, s.getConfidenceIntervalAt(0.50)[0]);
        Assert.assertEquals(Double.NaN, s.getConfidenceIntervalAt(0.50)[1]);
        Assert.assertEquals(42.0D, s.getPercentile(0));
        Assert.assertEquals(42.0D, s.getPercentile(100));
    }

    @Test
    public strictfp void testHistogram_MinMax() {
        SingletonStatistics s = new SingletonStatistics(42.5);

        Util.assertHistogram(s,
                new double[] {Double.MIN_VALUE, Double.MAX_VALUE},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_42_43() {
        SingletonStatistics s = new SingletonStatistics(42.5);

        Util.assertHistogram(s,
                new double[] {42, 43},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_0_42() {
        SingletonStatistics s = new SingletonStatistics(42.5);

        Util.assertHistogram(s,
                new double[] {0, 42},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_43_100() {
        SingletonStatistics s = new SingletonStatistics(42.5);

        Util.assertHistogram(s,
                new double[] {43, 100},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_leftBound() {
        SingletonStatistics s = new SingletonStatistics(10);

        Util.assertHistogram(s,
                new double[] {10, 100},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_rightBound() {
        SingletonStatistics s = new SingletonStatistics(10);

        Util.assertHistogram(s,
                new double[] {0, 10},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_left() {
        SingletonStatistics s = new SingletonStatistics(9);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {0, 0, 0, 1}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_right() {
        SingletonStatistics s = new SingletonStatistics(1);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {1, 0, 0, 0}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_middle() {
        SingletonStatistics s = new SingletonStatistics(5);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {0, 0, 1, 0}
        );
    }

    /**
     * Test of iterator which make accessible raw data.
     */
    @Test
    public strictfp void testRawDataIterator() {
        Iterator<Map.Entry<Double, Long>> singIter = singStats.getRawData();
        Assert.assertTrue(singIter.hasNext());
        Map.Entry<Double, Long> entry = singIter.next();
        Assert.assertEquals(entry.getKey(), VALUE);
        Assert.assertEquals(entry.getValue().longValue(), 1L);
        Assert.assertFalse(singIter.hasNext());
    }

}
