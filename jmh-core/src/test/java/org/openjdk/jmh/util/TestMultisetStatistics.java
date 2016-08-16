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

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Statistics
 */
public class TestMultisetStatistics {

    private static final double[] VALUES = {
        60.89053178, 3.589312005, 42.73638635, 85.55397805, 96.66786311,
        29.31809699, 63.50268147, 52.24157468, 64.68049085, 2.34517545,
        92.62435741, 7.50775664, 31.92395987, 82.68609724, 71.07171954,
        15.78967174, 34.43339987, 65.40063304, 69.86288638, 22.55130769,
        36.99130073, 60.17648239, 33.1484382, 56.4605944, 93.67454206
    };

    private static final MultisetStatistics instance = new MultisetStatistics();

    @BeforeClass
    public static void setUpClass() throws Exception {
        for (double value : VALUES) {
            instance.addValue(value, 1);
        }
    }

    /**
     * Test of add method, of class Statistics.
     */
    @Test
    public strictfp void testAdd_double() {
        ListStatistics stats = new ListStatistics();
        stats.addValue(VALUES[0]);
        assertEquals(1, stats.getN());
        assertEquals(VALUES[0], stats.getSum(), 0.0);
        assertEquals(VALUES[0], stats.getMax(), 0.0);
        assertEquals(VALUES[0], stats.getMin(), 0.0);
        assertEquals(VALUES[0], stats.getMean(), 0.0);
        assertEquals(Double.NaN, stats.getVariance(), 0.0);
        assertEquals(Double.NaN, stats.getStandardDeviation(), 0.0);
    }

    /**
     * Test of getN method, of class Statistics.
     */
    @Test
    public strictfp void testGetN() {
        assertEquals((long) VALUES.length, instance.getN());
    }

    /**
     * Test of getSum method, of class Statistics.
     */
    @Test
    public strictfp void testGetSum() {
        assertEquals(1275.829, instance.getSum(), 0.001);
    }

    /**
     * Test of getMean method, of class Statistics.
     */
    @Test
    public strictfp void testGetMean() {
        assertEquals(51.033, instance.getMean(), 0.001);
    }

    /**
     * Test of getMax method, of class Statistics.
     */
    @Test
    public strictfp void testGetMax() {
        assertEquals(96.66786311, instance.getMax(), 0.0);
    }

    /**
     * Test of getMin method, of class Statistics.
     */
    @Test
    public strictfp void testGetMin() {
        assertEquals(2.34517545, instance.getMin(), 0.0);
    }

    /**
     * Test of getVariance method, of class Statistics.
     */
    @Test
    public strictfp void testGetVariance() {
        assertEquals(816.9807, instance.getVariance(), 0.0001);
    }

    /**
     * Test of getStandardDeviation method, of class Statistics.
     */
    @Test
    public strictfp void testGetStandardDeviation() {
        assertEquals(28.5828, instance.getStandardDeviation(), 0.0001);
    }

    /**
     * Test of getConfidenceIntervalAt, of class Statistics
     */
    @Test
    public strictfp void testGetConfidenceInterval() {
        double[] interval = instance.getConfidenceIntervalAt(0.999);
        assertEquals(29.62232, interval[0], 0.002);
        assertEquals(72.44402, interval[1], 0.002);
    }

    @Test
    public strictfp void testPercentile_00() {
        assertEquals(2.345, instance.getPercentile(0), 0.002);
    }

    @Test
    public strictfp void testPercentile_50() {
        assertEquals(56.460, instance.getPercentile(50), 0.002);
    }

    @Test
    public strictfp void testPercentile_90() {
        assertEquals(93.044, instance.getPercentile(90), 0.002);
    }

    @Test
    public strictfp void testPercentile_99() {
        assertEquals(96.667, instance.getPercentile(99), 0.002);
    }

    @Test
    public strictfp void testPercentile_100() {
        assertEquals(96.667, instance.getPercentile(100), 0.002);
    }

    /**
     * Test of toString, of class Statistics
     */
    @Test
    public strictfp void testToString() {
        String expResult = "N:25 Mean: 51.033169517400005 Min: 2.34517545 Max: 96.66786311 StdDev: 28.582874479178017";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    @Test
    public strictfp void testSignificant_Always() {
        MultisetStatistics s1 = new MultisetStatistics();
        MultisetStatistics s2 = new MultisetStatistics();

        s1.addValue(1, 10);
        s1.addValue(1.1, 10);
        s2.addValue(2, 10);
        s2.addValue(2.1, 10);

        for (double conf : new double[] {0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999}) {
            Assert.assertTrue("Diff significant at " + conf, s1.isDifferent(s2, conf));
            Assert.assertEquals(1, s1.compareTo(s2, conf));
        }
    }

    @Test
    public strictfp void testSignificant_Never() {
        MultisetStatistics s1 = new MultisetStatistics();
        MultisetStatistics s2 = new MultisetStatistics();

        s1.addValue(1, 10);
        s1.addValue(1.1, 10);
        s2.addValue(1, 10);
        s2.addValue(1.1, 10);

        for (double conf : new double[] {0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999}) {
            Assert.assertFalse("Diff not significant at " + conf, s1.isDifferent(s2, conf));
            Assert.assertEquals(0, s1.compareTo(s2, conf));
        }
    }

    @Test
    public strictfp void testSignificant_Sometimes() {
        MultisetStatistics s1 = new MultisetStatistics();
        MultisetStatistics s2 = new MultisetStatistics();

        s1.addValue(1, 10);
        s1.addValue(2, 10);
        s2.addValue(1, 10);
        s2.addValue(3, 10);

        Assert.assertTrue("Diff significant at 0.5", s1.isDifferent(s2, 0.5));
        Assert.assertTrue("Diff significant at 0.9", s1.isDifferent(s2, 0.9));
        Assert.assertFalse("Diff not significant at 0.99", s1.isDifferent(s2, 0.99));
        Assert.assertFalse("Diff not significant at 0.999", s1.isDifferent(s2, 0.999));
        Assert.assertFalse("Diff not significant at 0.9999", s1.isDifferent(s2, 0.9999));

        Assert.assertEquals("compareTo at 0.5", 1, s1.compareTo(s2, 0.5));
        Assert.assertEquals("compareTo at 0.9", 1, s1.compareTo(s2, 0.9));
        Assert.assertEquals("compareTo at 0.99", 0, s1.compareTo(s2, 0.99));
        Assert.assertEquals("compareTo at 0.999", 0, s1.compareTo(s2, 0.999));
        Assert.assertEquals("compareTo at 0.9999", 0, s1.compareTo(s2, 0.9999));
    }


    @Test
    public strictfp void testEmpty() {
        Statistics s = new ListStatistics();

        Assert.assertEquals(0, s.getN());
        Assert.assertEquals(Double.NaN, s.getSum());
        Assert.assertEquals(Double.NaN, s.getMin());
        Assert.assertEquals(Double.NaN, s.getMax());
        Assert.assertEquals(Double.NaN, s.getMean());
        Assert.assertEquals(Double.NaN, s.getMeanErrorAt(0.5));
        Assert.assertEquals(Double.NaN, s.getVariance());
        Assert.assertEquals(Double.NaN, s.getStandardDeviation());
        Assert.assertEquals(Double.NaN, s.getConfidenceIntervalAt(0.50)[0]);
        Assert.assertEquals(Double.NaN, s.getConfidenceIntervalAt(0.50)[1]);
        Assert.assertEquals(Double.NaN, s.getPercentile(0));
        Assert.assertEquals(Double.NaN, s.getPercentile(100));
    }

    @Test
    public strictfp void testSingle() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(42.0D, 1);

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
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(42.5, 1);

        Util.assertHistogram(s,
                new double[] {Double.MIN_VALUE, Double.MAX_VALUE},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_42_43() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(42.5, 1);

        Util.assertHistogram(s,
                new double[] {42, 43},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_0_42() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(42.5, 1);

        Util.assertHistogram(s,
                new double[] {0, 42},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_43_100() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(42.5, 1);

        Util.assertHistogram(s,
                new double[] {43, 100},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_leftBound() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(10, 1);

        Util.assertHistogram(s,
                new double[] {10, 100},
                new int[] {1}
        );
    }

    @Test
    public strictfp void testHistogram_rightBound() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(10, 1);

        Util.assertHistogram(s,
                new double[] {0, 10},
                new int[] {0}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_left() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(9, 1);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {0, 0, 0, 1}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_right() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(1, 1);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {1, 0, 0, 0}
        );
    }

    @Test
    public strictfp void testHistogram_emptyLevels_middle() {
        MultisetStatistics s = new MultisetStatistics();
        s.addValue(5, 1);

        Util.assertHistogram(s,
                new double[] {0, 2, 4, 8, 10},
                new int[] {0, 0, 1, 0}
        );
    }

    @Test
    public strictfp void testHistogram_increasing() {
        MultisetStatistics s = new MultisetStatistics();
        for (int c = 0; c <= 10; c++) {
            s.addValue(c * 10, c);
        }

        Util.assertHistogram(s,
                new double[] {0, 200},
                new int[] {55}
        );

        Util.assertHistogram(s,
                new double[] {0, 50, 101},
                new int[] {10, 45}
        );

        Util.assertHistogram(s,
                new double[] {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100},
                new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
        );
    }

    /**
     * Test of iterator which make accessible raw data.
     * Iterate over default instance with no duplicates.
     */
    @Test
    public strictfp void testRawDataIterator_no_duplicates() {
        int itemCount = 0;
        for (Map.Entry<Double, Long> entry : Utils.adaptForLoop(instance.getRawData())) {
            Assert.assertEquals(entry.getValue().longValue(), 1L);

            // Check if key (the actual data) is in the VALUES collection,
            // else fail the test (the Multiset was constructed with values
            // from VALUES collection, so it should be there).
            boolean keyIsPresent = false;
            double key = entry.getKey();
            for (double value : VALUES) {
                if (Double.compare(value, key) == 0) {
                    keyIsPresent = true;
                }
            }
            Assert.assertTrue("Value from iterator is not present in source collection", keyIsPresent);

            itemCount++;
        }
        Assert.assertEquals(itemCount, VALUES.length);
    }

    /**
     * Test of iterator which make accessible raw data.
     * Iterate over new instance with duplicates.
     */
    @Test
    public strictfp void testRawDataIterator_duplicates() {
        MultisetStatistics s = new MultisetStatistics();
        for (int c = 0; c <= 10; c++) {
            s.addValue(c * 10, c);
        }

        int itemCount = 0;
        for (Map.Entry<Double, Long> entry : Utils.adaptForLoop(s.getRawData())) {
            Assert.assertEquals(entry.getKey(), (double)(entry.getValue() * 10));
            itemCount++;
        }
        Assert.assertEquals(itemCount, 10);
    }

}
