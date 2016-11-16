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
package org.openjdk.jmh.it.ccontrol;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class LogConsumeProfiler implements ExternalProfiler {

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Arrays.asList("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCompilation", "-XX:+PrintInlining");
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {

    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        try {
            return Arrays.asList(
                    new LogConsumeResult("out", FileUtils.readAllLines(stdOut)),
                    new LogConsumeResult("err", FileUtils.readAllLines(stdErr))
            );
        } catch (IOException e) {
            // skip
        }
        return Collections.emptyList();
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
        return "Log aggregating profiler";
    }

    public static class LogConsumeResult extends Result<LogConsumeResult> {
        private final Collection<String> lines;

        public LogConsumeResult(String label, Collection<String> lines) {
            super(ResultRole.SECONDARY, "log" + label, of(Double.NaN), "N/A", AggregationPolicy.MAX);
            this.lines = lines;
        }

        public Collection<String> getLines() {
            return lines;
        }

        @Override
        protected Aggregator<LogConsumeResult> getThreadAggregator() {
            return new LogAggregator();
        }

        @Override
        protected Aggregator<LogConsumeResult> getIterationAggregator() {
            return new LogAggregator();
        }

        public static class LogAggregator implements Aggregator<LogConsumeResult> {
            @Override
            public LogConsumeResult aggregate(Collection<LogConsumeResult> results) {
                String label = null;
                Collection<String> allLines = new ArrayList<>();
                for (LogConsumeResult r : results) {
                    label = r.label;
                    allLines.addAll(r.getLines());
                }
                return new LogConsumeResult(label, allLines);
            }
        }

    }
}
