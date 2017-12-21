/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.it.profilers;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class CountingExternalProfilerTest {

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void bench() {
      // intentionally left blank
    }

    @Test
    public void test() throws RunnerException {
        for (int warmupForks : new int[]{0, 1, 5}) {
            for (int forks : new int[]{1, 5}) {
                CountingExternalProfiler.reset();

                Options opts = new OptionsBuilder()
                        .include(Fixtures.getTestMask(this.getClass()))
                        .addProfiler(CountingExternalProfiler.class)
                        .forks(forks)
                        .warmupForks(warmupForks)
                        .build();
                new Runner(opts).run();

                Assert.assertEquals("jvmOpts count is correct for warmupForks = " + warmupForks + ", and forks = " + forks,
                        warmupForks + forks, CountingExternalProfiler.jvmOpts.get());
                Assert.assertEquals("jvmInvokeOpts count is correct for warmupForks = " + warmupForks + ", and forks = " + forks,
                        warmupForks + forks, CountingExternalProfiler.jvmInvokeOpts.get());
                Assert.assertEquals("afterTrial count is correct for warmupForks = " + warmupForks + ", and forks = " + forks,
                        warmupForks + forks, CountingExternalProfiler.afterTrial.get());
                Assert.assertEquals("beforeTrial count is correct for warmupForks = " + warmupForks + ", and forks = " + forks,
                        warmupForks + forks, CountingExternalProfiler.beforeTrial.get());
            }
        }
    }

}
