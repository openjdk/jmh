/**
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

import org.openjdk.jmh.util.internal.Statistics;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Statistics
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com
 */
public class TestStatistics {

    private static final double[] VALUES = {
        60.89053178, 3.589312005, 42.73638635, 85.55397805, 96.66786311,
        29.31809699, 63.50268147, 52.24157468, 64.68049085, 2.34517545,
        92.62435741, 7.50775664, 31.92395987, 82.68609724, 71.07171954,
        15.78967174, 34.43339987, 65.40063304, 69.86288638, 22.55130769,
        36.99130073, 60.17648239, 33.1484382, 56.4605944, 93.67454206
    };

    private static final Statistics instance = new Statistics();

    public TestStatistics() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        for (double value : VALUES) {
            instance.addValue(value);
        }
    }

    /**
     * Test of add method, of class Statistics.
     */
    @Test
    public strictfp void testAdd_double() {
        Statistics stats = new Statistics();
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
        assertEquals((long) VALUES.length, (long) instance.getN());
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
     * Test of getConfidenceInterval, of class Statistics
     */
    @Test
    public strictfp void testGetConfidenceInterval() {
        double[] interval = instance.getConfidenceInterval(0.05);
        assertEquals(39.234, interval[0], 0.002);
        assertEquals(62.831, interval[1], 0.002);
    }

    /**
     * Test of toString, of class Statistics
     */
    @Test
    public strictfp void testToString() {
        String expResult = "Mean: 51.03316951740001 Min: 2.34517545 Max: 96.66786311 StdDev: 28.582874479178013";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
}
