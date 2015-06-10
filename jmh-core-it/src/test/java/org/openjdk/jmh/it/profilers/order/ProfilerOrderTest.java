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
package org.openjdk.jmh.it.profilers.order;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProfilerOrderTest {

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void bench() {
      // intentionally left blank
    }

    @Test
    public void testInternal_API() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(InternalProfiler1.class)
                .addProfiler(InternalProfiler2.class)
                .addProfiler(InternalProfiler3.class)
                .build();

        runWith(opts);
    }

    @Test
    public void testInternal_CLI() throws RunnerException, CommandLineOptionException {
        CommandLineOptions opts = new CommandLineOptions(
                Fixtures.getTestMask(this.getClass()),
                "-prof", InternalProfiler1.class.getCanonicalName(),
                "-prof", InternalProfiler2.class.getCanonicalName(),
                "-prof", InternalProfiler3.class.getCanonicalName()
        );
        runWith(opts);
    }

    @Test
    public void testExternal_API() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(ExternalProfiler1.class)
                .addProfiler(ExternalProfiler2.class)
                .addProfiler(ExternalProfiler3.class)
                .build();

        runWith(opts);
    }

    @Test
    public void testExternal_CLI() throws RunnerException, CommandLineOptionException {
        CommandLineOptions opts = new CommandLineOptions(
                Fixtures.getTestMask(this.getClass()),
                "-prof", ExternalProfiler1.class.getCanonicalName(),
                "-prof", ExternalProfiler2.class.getCanonicalName(),
                "-prof", ExternalProfiler3.class.getCanonicalName()
                );
        runWith(opts);
    }

    private void runWith(Options opts) throws RunnerException {Collection<RunResult> results = new Runner(opts).run();
        for (RunResult r : results) {
            Map<String, Result> sec = r.getSecondaryResults();
            double prof1start = sec.get("prof1start").getScore();
            double prof2start = sec.get("prof2start").getScore();
            double prof3start = sec.get("prof3start").getScore();

            double prof1stop = sec.get("prof1stop").getScore();
            double prof2stop = sec.get("prof2stop").getScore();
            double prof3stop = sec.get("prof3stop").getScore();

            Assert.assertTrue("start(1) < start(2)", prof1start < prof2start);
            Assert.assertTrue("start(2) < start(3)", prof2start < prof3start);

            Assert.assertTrue("stop(3) < stop(2)", prof3stop < prof2stop);
            Assert.assertTrue("stop(2) < stop(1)", prof2stop < prof1stop);
        }
    }

}
