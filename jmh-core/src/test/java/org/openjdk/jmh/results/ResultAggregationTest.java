/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.util.SampleBuffer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ResultAggregationTest {

    @Test
    public void testThroughput() {
        IterationResult ir = new IterationResult(null, null, null);
        ir.addResult(new ThroughputResult(ResultRole.PRIMARY, "", 10000, 1, TimeUnit.NANOSECONDS));
        ir.addResult(new ThroughputResult(ResultRole.PRIMARY, "", 10000, 1, TimeUnit.NANOSECONDS));
        ir.addResult(new ThroughputResult(ResultRole.SECONDARY, "sec", 5000, 1, TimeUnit.NANOSECONDS));
        ir.addResult(new ThroughputResult(ResultRole.SECONDARY, "sec", 5000, 1, TimeUnit.NANOSECONDS));
        Assert.assertEquals(20000.0, ir.getPrimaryResult().getScore());
        Assert.assertEquals(10000.0, ir.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(2, ir.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, ir.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, ir.getRawPrimaryResults().size());
        Assert.assertEquals(2, ir.getRawSecondaryResults().get("sec").size());

        BenchmarkResult br = new BenchmarkResult(null, Arrays.asList(ir, ir));
        br.addBenchmarkResult(new ThroughputResult(ResultRole.SECONDARY, "bench", 3000, 1, TimeUnit.NANOSECONDS));
        Assert.assertEquals(20000.0, br.getPrimaryResult().getScore());
        Assert.assertEquals(10000.0, br.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, br.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(2, br.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, br.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(1, br.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, br.getIterationResults().size());

        RunResult rr = new RunResult(null, Arrays.asList(br, br));
        Assert.assertEquals(20000.0, rr.getPrimaryResult().getScore());
        Assert.assertEquals(10000.0, rr.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, rr.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(4, rr.getPrimaryResult().getSampleCount());
        Assert.assertEquals(4, rr.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, rr.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, rr.getBenchmarkResults().size());
    }

    @Test
    public void testAverageTime() {
        IterationResult ir = new IterationResult(null, null, null);
        ir.addResult(new AverageTimeResult(ResultRole.PRIMARY, "", 1, 10000, TimeUnit.NANOSECONDS));
        ir.addResult(new AverageTimeResult(ResultRole.PRIMARY, "", 1, 10000, TimeUnit.NANOSECONDS));
        ir.addResult(new AverageTimeResult(ResultRole.SECONDARY, "sec", 1, 5000, TimeUnit.NANOSECONDS));
        ir.addResult(new AverageTimeResult(ResultRole.SECONDARY, "sec", 1, 5000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, ir.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, ir.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(2, ir.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, ir.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, ir.getRawPrimaryResults().size());
        Assert.assertEquals(2, ir.getRawSecondaryResults().get("sec").size());

        BenchmarkResult br = new BenchmarkResult(null, Arrays.asList(ir, ir));
        br.addBenchmarkResult(new AverageTimeResult(ResultRole.SECONDARY, "bench", 1, 3000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, br.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, br.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, br.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(2, br.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, br.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(1, br.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, br.getIterationResults().size());

        RunResult rr = new RunResult(null, Arrays.asList(br, br));
        Assert.assertEquals(10000.0, rr.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, rr.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, rr.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(4, rr.getPrimaryResult().getSampleCount());
        Assert.assertEquals(4, rr.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, rr.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, rr.getBenchmarkResults().size());
    }

    @Test
    public void testSampleTime() {
        SampleBuffer sb10000 = new SampleBuffer();
        sb10000.add(10000);

        SampleBuffer sb5000 = new SampleBuffer();
        sb5000.add(5000);

        SampleBuffer sb3000 = new SampleBuffer();
        sb3000.add(3000);

        IterationResult ir = new IterationResult(null, null, null);
        ir.addResult(new SampleTimeResult(ResultRole.PRIMARY, "", sb10000, TimeUnit.NANOSECONDS));
        ir.addResult(new SampleTimeResult(ResultRole.PRIMARY, "", sb10000, TimeUnit.NANOSECONDS));
        ir.addResult(new SampleTimeResult(ResultRole.SECONDARY, "sec", sb5000, TimeUnit.NANOSECONDS));
        ir.addResult(new SampleTimeResult(ResultRole.SECONDARY, "sec", sb5000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, ir.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, ir.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(2, ir.getRawPrimaryResults().size());
        Assert.assertEquals(2, ir.getRawSecondaryResults().get("sec").size());
        Assert.assertEquals(2, ir.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, ir.getSecondaryResults().get("sec").getSampleCount());

        BenchmarkResult br = new BenchmarkResult(null, Arrays.asList(ir, ir));
        br.addBenchmarkResult(new SampleTimeResult(ResultRole.SECONDARY, "bench", sb3000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, br.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, br.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, br.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(4, br.getPrimaryResult().getSampleCount());
        Assert.assertEquals(4, br.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(1, br.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, br.getIterationResults().size());

        RunResult rr = new RunResult(null, Arrays.asList(br, br));
        Assert.assertEquals(10000.0, rr.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, rr.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, rr.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(8, rr.getPrimaryResult().getSampleCount());
        Assert.assertEquals(8, rr.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, rr.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, rr.getBenchmarkResults().size());
    }

    @Test
    public void testSingleShot() {
        IterationResult ir = new IterationResult(null, null, null);
        ir.addResult(new SingleShotResult(ResultRole.PRIMARY, "", 10000, TimeUnit.NANOSECONDS));
        ir.addResult(new SingleShotResult(ResultRole.PRIMARY, "", 10000, TimeUnit.NANOSECONDS));
        ir.addResult(new SingleShotResult(ResultRole.SECONDARY, "sec", 5000, TimeUnit.NANOSECONDS));
        ir.addResult(new SingleShotResult(ResultRole.SECONDARY, "sec", 5000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, ir.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, ir.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(2, ir.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, ir.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, ir.getRawPrimaryResults().size());
        Assert.assertEquals(2, ir.getRawSecondaryResults().get("sec").size());

        BenchmarkResult br = new BenchmarkResult(null, Arrays.asList(ir, ir));
        br.addBenchmarkResult(new SingleShotResult(ResultRole.SECONDARY, "bench", 3000, TimeUnit.NANOSECONDS));
        Assert.assertEquals(10000.0, br.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, br.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, br.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(2, br.getPrimaryResult().getSampleCount());
        Assert.assertEquals(2, br.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(1, br.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, br.getIterationResults().size());

        RunResult rr = new RunResult(null, Arrays.asList(br, br));
        Assert.assertEquals(10000.0, rr.getPrimaryResult().getScore());
        Assert.assertEquals(5000.0, rr.getSecondaryResults().get("sec").getScore());
        Assert.assertEquals(3000.0, rr.getSecondaryResults().get("bench").getScore());
        Assert.assertEquals(4, rr.getPrimaryResult().getSampleCount());
        Assert.assertEquals(4, rr.getSecondaryResults().get("sec").getSampleCount());
        Assert.assertEquals(2, rr.getSecondaryResults().get("bench").getSampleCount());
        Assert.assertEquals(2, rr.getBenchmarkResults().size());
    }


}
