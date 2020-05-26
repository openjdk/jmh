/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class LinuxPerfC2CProfiler implements ExternalProfiler {

    protected final TempFile perfBinData;

    public LinuxPerfC2CProfiler() throws ProfilerException {
        try {
            perfBinData = FileUtils.weakTempFile("perf-c2c-bin");
        } catch (IOException e) {
            throw new ProfilerException(e);
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        long delay = TimeUnit.NANOSECONDS.toMillis(params.getWarmup().getCount() *
                params.getWarmup().getTime().convertTo(TimeUnit.NANOSECONDS))
                + TimeUnit.SECONDS.toMillis(1); // loosely account for the JVM lag
        return Arrays.asList("perf", "c2c", "record", "-o", perfBinData.getAbsolutePath(), "--", "--delay", String.valueOf(delay));
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {}

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        try {
            Process process = new ProcessBuilder("perf", "c2c", "report", "--stats", "-i", perfBinData.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();

            Collection<String> lines = FileUtils.readAllLines(process.getInputStream());
            String out = Utils.join(lines, System.lineSeparator());
            return Collections.singleton(new TextResult(out, "perfc2c"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Linux perf c2c profiler";
    }

}

