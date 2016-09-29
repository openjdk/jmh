/*
 * Copyright (c) 2016, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.it.auxcounters;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;


@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class AuxCountersOperationsTest {

    @AuxCounters(AuxCounters.Type.OPERATIONS)
    @State(Scope.Thread)
    public static class Counters {
        public int cnt;
    }

    @Benchmark
    public void test(Counters cnt) {
        cnt.cnt++;
        Fixtures.work();
    }

    @Test
    public void invokeAPI() throws RunnerException {
        for (Mode mode : Mode.values()) {
            if (mode == Mode.All) continue;

            Options opts = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .mode(mode)
                    .shouldFailOnError(true)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result prim = result.getPrimaryResult();
            Result scnd = result.getSecondaryResults().get("cnt");

            switch (mode) {
                case Throughput:
                case AverageTime: {
                    Assert.assertNotNull("@AuxCounter result exists for " + mode, scnd);

                    // Should match primary metric exactly.
                    Assert.assertEquals(prim.getSampleCount(), scnd.getSampleCount());
                    Assert.assertEquals(prim.getScoreUnit(), scnd.getScoreUnit());
                    Assert.assertEquals(prim.getScore(), scnd.getScore(), 0.001);
                    Assert.assertEquals(prim.getScoreError(), scnd.getScoreError(), 0.001);
                    break;
                }
                default: {
                    Assert.assertNull("@AuxCounter does not exist for " + mode, scnd);
                }
            }
        }
    }

}
