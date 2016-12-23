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
package org.openjdk.jmh.runner.format;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TextReportFormat implementation of OutputFormat.
 */
class TextReportFormat extends AbstractOutputFormat {

    public TextReportFormat(PrintStream out, VerboseMode verbose) {
        super(out, verbose);
    }

    @Override
    public void startBenchmark(BenchmarkParams params) {
        IterationParams warmup = params.getWarmup();
        if (warmup.getCount() > 0) {
            out.println("# Warmup: " + warmup.getCount() + " iterations, " +
                    warmup.getTime() + " each" +
                    (warmup.getBatchSize() <= 1 ? "" : ", " + warmup.getBatchSize() + " calls per op"));
        } else {
            out.println("# Warmup: <none>");
        }

        IterationParams measurement = params.getMeasurement();
        if (measurement.getCount() > 0) {
            out.println("# Measurement: " + measurement.getCount() + " iterations, " +
                    measurement.getTime() + " each" +
                    (measurement.getBatchSize() <= 1 ? "" : ", " + measurement.getBatchSize() + " calls per op"));
        } else {
            out.println("# Measurement: <none>");
        }

        TimeValue timeout = params.getTimeout();
        boolean timeoutWarning = (timeout.convertTo(TimeUnit.NANOSECONDS) <= measurement.getTime().convertTo(TimeUnit.NANOSECONDS)) ||
                (timeout.convertTo(TimeUnit.NANOSECONDS) <= warmup.getTime().convertTo(TimeUnit.NANOSECONDS));
        out.println("# Timeout: " + timeout + " per iteration" + (timeoutWarning ? ", ***WARNING: The timeout might be too low!***" : ""));

        out.print("# Threads: " + params.getThreads() + " " + getThreadsString(params.getThreads()));

        if (!params.getThreadGroupLabels().isEmpty()) {
            int[] tg = params.getThreadGroups();

            // TODO: Make params.getThreadGroupLabels return List
            List<String> labels = new ArrayList<>(params.getThreadGroupLabels());
            String[] ss = new String[tg.length];
            for (int cnt = 0; cnt < tg.length; cnt++) {
                ss[cnt] = tg[cnt] + "x \"" + labels.get(cnt) + "\"";
            }

            int groupCount = params.getThreads() / Utils.sum(tg);
            out.print(" (" + groupCount + " " + getGroupsString(groupCount) + "; " + Utils.join(ss, ", ") + " in each group)");
        }

        out.println(params.shouldSynchIterations() ?
                ", will synchronize iterations" :
                (params.getMode() == Mode.SingleShotTime) ? "" : ", ***WARNING: Synchronize iterations are disabled!***");


        out.println("# Benchmark mode: " + params.getMode().longLabel());
        out.println("# Benchmark: " + params.getBenchmark());
        if (!params.getParamsKeys().isEmpty()) {
            String s = "";
            boolean isFirst = true;
            for (String k : params.getParamsKeys()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    s += ", ";
                }
                s += k + " = " + params.getParam(k);
            }
            out.println("# Parameters: (" + s + ")");
        }
    }

    @Override
    public void iteration(BenchmarkParams benchmarkParams, IterationParams params, int iteration) {
        switch (params.getType()) {
            case WARMUP:
                out.print(String.format("# Warmup Iteration %3d: ", iteration));
                break;
            case MEASUREMENT:
                out.print(String.format("Iteration %3d: ", iteration));
                break;
            default:
                throw new IllegalStateException("Unknown iteration type: " + params.getType());
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

    protected static String getGroupsString(int g) {
        if (g > 1) {
            return "groups";
        } else {
            return "group";
        }
    }

    @Override
    public void iterationResult(BenchmarkParams benchmParams, IterationParams params, int iteration, IterationResult data) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getPrimaryResult().toString());

        if (params.getType() == IterationType.MEASUREMENT) {
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
    public void endBenchmark(BenchmarkResult result) {
        out.println();
        if (result != null) {
            {
                Result r = result.getPrimaryResult();
                String s = r.extendedInfo();
                if (!s.trim().isEmpty()) {
                    out.println("Result \"" + result.getParams().getBenchmark() + "\":");
                    out.println(s);
                }
            }
            for (Result r : result.getSecondaryResults().values()) {
                String s = r.extendedInfo();
                if (!s.trim().isEmpty()) {
                    out.println("Secondary result \"" + result.getParams().getBenchmark() + ":" + r.getLabel() + "\":");
                    out.println(s);
                }
            }
            out.println();
        }
    }

    @Override
    public void startRun() {
        // do nothing
    }

    @Override
    public void endRun(Collection<RunResult> runResults) {
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, out).writeOut(runResults);
    }

}
