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
package org.openjdk.jmh.output.format;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.results.ResultFormatFactory;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * TextReportFormat implementation of OutputFormat.
 */
class TextReportFormat extends AbstractOutputFormat {

    public TextReportFormat(PrintStream out, VerboseMode verbose) {
        super(out, verbose);
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, BenchmarkParams mbParams) {
        if (mbParams.getWarmup().getCount() > 0) {
            out.println("# Warmup: " + mbParams.getWarmup().getCount() + " iterations, " +
                    mbParams.getWarmup().getTime() + " each" +
                    (mbParams.getWarmup().getBatchSize() <= 1 ? "" : ", " + mbParams.getWarmup().getBatchSize() + " calls per batch"));
        } else {
            out.println("# Warmup: <none>");
        }

        if (mbParams.getMeasurement().getCount() > 0) {
            out.println("# Measurement: " + mbParams.getMeasurement().getCount() + " iterations, " +
                    mbParams.getMeasurement().getTime() + " each" +
                    (mbParams.getMeasurement().getBatchSize() <= 1 ? "" : ", " + mbParams.getMeasurement().getBatchSize() + " calls per batch"));
        } else {
            out.println("# Measurement: <none>");
        }

        out.println("# Threads: " + mbParams.getThreads() + " " + getThreadsString(mbParams.getThreads()) +
                (mbParams.shouldSynchIterations() ?
                        ", will synchronize iterations" :
                        (name.getMode() == Mode.SingleShotTime) ? "" : ", ***WARNING: Synchronize iterations are disabled!***"));
        out.println("# Benchmark mode: " + name.getMode().longLabel());
        out.println("# Benchmark: " + name.getUsername());
        if (!name.getActualParams().isEmpty()) {
            out.println("# Parameters: " + name.getActualParams());
        }
    }

    @Override
    public void iteration(BenchmarkRecord benchmark, IterationParams params, int iteration, IterationType type) {
        switch (type) {
            case WARMUP:
                out.print(String.format("# Warmup Iteration %3d: ", iteration));
                break;
            case MEASUREMENT:
                out.print(String.format("Iteration %3d: ", iteration));
                break;
            default:
                throw new IllegalStateException("Unknown iteration type: " + type);
        }
        out.flush();
    }

    protected static String getThreadsString(int t) {
        if (t > 1) {
            return "threads";
        } else {
            return "thread";
        }
    }

    @Override
    public void iterationResult(BenchmarkRecord name, IterationParams params, int iteration, IterationType type, IterationResult data) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getPrimaryResult().toString());

        if (type == IterationType.MEASUREMENT) {
            int prefixLen = String.format("Iteration %3d: ", iteration).length();

            Map<String, Result> secondary = data.getSecondaryResults();
            if (!secondary.isEmpty()) {
                sb.append("\n");

                int maxKeyLen = 0;
                for (Map.Entry<String, Result> res : secondary.entrySet()) {
                    maxKeyLen = Math.max(maxKeyLen, res.getKey().length());
                }

                for (Map.Entry<String, Result> res : secondary.entrySet()) {
                    sb.append(String.format("%" + prefixLen + "s", ""));
                    sb.append(String.format("  %-" + (maxKeyLen + 1) + "s %s", res.getKey() + ":", res.getValue()));
                    sb.append("\n");
                }
            }
        }

        out.print(String.format("%s%n", sb.toString()));
        out.flush();
    }

    @Override
    public void endBenchmark(BenchmarkRecord name, BenchResult result) {
        out.println();
        out.println(result.getPrimaryResult().extendedInfo(null));
        for (Result r : result.getSecondaryResults().values()) {
            out.println(r.extendedInfo(r.getLabel()));
        }
        out.println();
    }

    @Override
    public void startRun() {
        // do nothing
    }

    @Override
    public void endRun(Map<BenchmarkRecord, RunResult> runResults) {
        PrintWriter pw = new PrintWriter(out);
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, pw).writeOut(runResults);
        pw.flush();
        pw.close();
    }

}
