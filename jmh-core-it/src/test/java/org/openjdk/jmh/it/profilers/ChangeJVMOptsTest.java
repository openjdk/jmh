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
package org.openjdk.jmh.it.profilers;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ChangeJVMOptsTest {

    public static final File TMP_FILE = new File(System.getProperty("java.io.tmpdir") + File.separator + "ChangingJVMOpts-list");

    @Setup(Level.Trial)
    public void setup() {
        try {
            FileUtils.appendLines(TMP_FILE,
                    Collections.singleton(ManagementFactory.getRuntimeMXBean().getInputArguments().toString()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(value = 5, warmups = 5)
    public void bench() {
      // intentionally left blank
    }

    @Test
    public void testExternal_API() throws RunnerException, IOException {
        prepare();

        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .warmupForks(5)
                .forks(5)
                .addProfiler(ChangeJVMOptsExternalProfiler.class)
                .build();
        new Runner(opts).run();

        check();
    }

    @Test
    public void testExternal_CLI() throws RunnerException, CommandLineOptionException, IOException {
        prepare();

        Options opts = new CommandLineOptions("-prof", ChangeJVMOptsExternalProfiler.class.getCanonicalName(), Fixtures.getTestMask(this.getClass()));
        new Runner(opts).run();

        check();
    }

    private void prepare() {
        TMP_FILE.delete();
    }

    private void check() throws IOException {
        Set<String> lines = new HashSet<>();
        for (String line : FileUtils.readAllLines(TMP_FILE)) {
            if (!lines.add(line)) {
                Assert.fail("Duplicate line: " + line);
            }
        }
    }

}
