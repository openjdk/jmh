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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class DuplicateProfilerTest {

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void bench() {
      // intentionally left blank
    }

    @Test
    public void testInternal_API() {
        try {
            Options opts = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .addProfiler(ItInternalProfiler.class)
                    .addProfiler(ItInternalProfiler.class)
                    .build();
            new Runner(opts).run();
            Assert.fail("Expected to fail");
        } catch (RunnerException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg, msg.contains("Cannot instantiate the same profiler"));
        }
    }

    @Test
    public void testExternal_API() {
        try {
            Options opts = new OptionsBuilder()
                    .include(Fixtures.getTestMask(this.getClass()))
                    .addProfiler(ItExternalProfiler.class)
                    .addProfiler(ItExternalProfiler.class)
                    .build();
            new Runner(opts).run();
            Assert.fail("Expected to fail");
        } catch (RunnerException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg, msg.contains("Cannot instantiate the same profiler"));
        }
    }

    @Test
    public void testInternal_CLI() throws  CommandLineOptionException {
        try {
            Options opts = new CommandLineOptions(
                    "-prof", ItInternalProfiler.class.getCanonicalName(),
                    "-prof", ItInternalProfiler.class.getCanonicalName(),
                    Fixtures.getTestMask(this.getClass()));
            new Runner(opts).run();
            Assert.fail("Expected to fail");
        } catch (RunnerException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg, msg.contains("Cannot instantiate the same profiler"));
        }
    }

    @Test
    public void testExternal_CLI() throws CommandLineOptionException {
        try {
            Options opts = new CommandLineOptions(
                    "-prof", ItExternalProfiler.class.getCanonicalName(),
                    "-prof", ItExternalProfiler.class.getCanonicalName(),
                    Fixtures.getTestMask(this.getClass()));
            new Runner(opts).run();
            Assert.fail("Expected to fail");
        } catch (RunnerException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg, msg.contains("Cannot instantiate the same profiler"));
        }
    }

}
