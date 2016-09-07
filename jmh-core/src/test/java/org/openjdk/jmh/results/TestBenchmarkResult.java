/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.util.ListStatistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class TestBenchmarkResult {

    @Test
    public void testMissingSecondaries() {
        IterationResult ir1 = new IterationResult(null, null, null);
        ir1.addResult(new PrimaryResult());
        ir1.addResult(new SecondaryResult("label1", 1));
        IterationResult ir2 = new IterationResult(null, null, null);
        ir2.addResult(new PrimaryResult());
        ir2.addResult(new SecondaryResult("label2", 2));
        IterationResult ir3 = new IterationResult(null, null, null);
        ir3.addResult(new PrimaryResult());
        ir3.addResult(new SecondaryResult("label2", 3));
        BenchmarkResult br = new BenchmarkResult(null, Arrays.asList(ir1, ir2, ir3));

        Map<String, Result> sr = br.getSecondaryResults();
        Assert.assertEquals(2, sr.size());
        Assert.assertEquals(1.0D, sr.get("label1").getScore(), 0.001);
        Assert.assertEquals(5.0D, sr.get("label2").getScore(), 0.001);
    }

    public static class PrimaryResult extends Result<PrimaryResult> {
        public PrimaryResult() {
            super(ResultRole.PRIMARY, "Boo", of(1.0D), "unit", AggregationPolicy.SUM);
        }

        @Override
        protected Aggregator<PrimaryResult> getThreadAggregator() {
            return new PrimaryResultAggregator();
        }

        @Override
        protected Aggregator<PrimaryResult> getIterationAggregator() {
            return new PrimaryResultAggregator();
        }
    }

    public static class SecondaryResult extends Result<SecondaryResult> {
        public SecondaryResult(String label, double val) {
            super(ResultRole.SECONDARY, label, of(val), "unit", AggregationPolicy.SUM);
        }

        protected Aggregator<SecondaryResult> getThreadAggregator() {
            return new SecondaryResultAggregator();
        }

        @Override
        protected Aggregator<SecondaryResult> getIterationAggregator() {
            return new SecondaryResultAggregator();
        }
    }

    public static class PrimaryResultAggregator implements Aggregator<PrimaryResult> {
        @Override
        public PrimaryResult aggregate(Collection<PrimaryResult> results) {
            return new PrimaryResult();
        }
    }

    public static class SecondaryResultAggregator implements Aggregator<SecondaryResult> {
        @Override
        public SecondaryResult aggregate(Collection<SecondaryResult> results) {
            String label = null;
            ListStatistics s = new ListStatistics();
            for (SecondaryResult r : results) {
                if (label == null) {
                    label = r.getLabel();
                } else {
                    Assert.assertEquals(label, r.getLabel());
                }
                s.addValue(r.getScore());
            }
            return new SecondaryResult(label, s.getSum());
        }
    }

}
