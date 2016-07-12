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

import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Utils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests for AggregateResult
 */
public class TestAggregateResult {

    private static IterationResult result;
    private static final double[] values = {10.0, 20.0, 30.0, 40.0, 50.0};

    @BeforeClass
    public static void setupClass() {
        result = new IterationResult(
                new BenchmarkParams("blah", "blah", false,
                        1, new int[]{1}, Collections.<String>emptyList(),
                        1, 1,
                        new IterationParams(IterationType.WARMUP, 1, TimeValue.seconds(1), 1),
                        new IterationParams(IterationType.MEASUREMENT, 1, TimeValue.seconds(1), 1),
                        Mode.Throughput, null, TimeUnit.SECONDS, 1,
                        Utils.getCurrentJvm(), Collections.<String>emptyList(),
                        TimeValue.days(1)),
                new IterationParams(IterationType.MEASUREMENT, 1, TimeValue.days(1), 1),
                null
        );
        for (double d : values) {
            result.addResult(new ThroughputResult(ResultRole.PRIMARY, "test1", (long) d, 10 * 1000 * 1000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testScore() throws Exception {
        assertEquals(15.0, result.getPrimaryResult().getScore(), 0.00001);
    }

    @Test
    public void testScoreUnit() throws Exception {
        assertEquals((new ThroughputResult(ResultRole.PRIMARY, "test1", 1, 1, TimeUnit.MILLISECONDS)).getScoreUnit(), result.getScoreUnit());
    }

}
