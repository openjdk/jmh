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
package org.openjdk.jmh.results;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestSingleShotResult {

    private static final double ASSERT_ACCURACY = 0.0000001;

    @Test
    public void testIterationAggregator1() {
        SingleShotResult r1 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 1000L, 1, TimeUnit.MICROSECONDS);
        SingleShotResult r2 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 2000L, 1, TimeUnit.MICROSECONDS);
        Result result = r1.getIterationAggregator().aggregate(Arrays.asList(r1, r2));

        assertEquals(1.5, result.getScore(), ASSERT_ACCURACY);
        assertEquals("us/op", result.getScoreUnit());
    }

    @Test
    public void testThreadAggregator1() {
        SingleShotResult r1 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 1000L, 1, TimeUnit.MICROSECONDS);
        SingleShotResult r2 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 2000L, 1, TimeUnit.MICROSECONDS);
        Result result = r1.getThreadAggregator().aggregate(Arrays.asList(r1, r2));

        assertEquals(1.5, result.getScore(), ASSERT_ACCURACY);
        assertEquals("us/op", result.getScoreUnit());
    }

    @Test
    public void testMultiops() {
        SingleShotResult r1 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 1000L, 1, TimeUnit.MICROSECONDS);
        SingleShotResult r2 = new SingleShotResult(ResultRole.PRIMARY, "Test1", 1000L, 2, TimeUnit.MICROSECONDS);

        assertEquals(1, r1.getScore(), ASSERT_ACCURACY);
        assertEquals(0.5, r2.getScore(), ASSERT_ACCURACY);
    }

}
