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

import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.internal.Statistics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * TextReportFormat implementation of OutputFormat.
 *
 * @author Brian Doherty
 * @author Aleksey Shipilev
 */
public class TextReportFormat extends AbstractOutputFormat {

    public TextReportFormat(PrintStream out, VerboseMode verbose) {
        super(out, verbose);
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, BenchmarkParams mbParams) {
        out.println("# Warmup: " + mbParams.getWarmup().getCount() + " iterations, " + mbParams.getWarmup().getTime() + " each");
        out.println("# Measurement: " + mbParams.getMeasurement().getCount() + " iterations, " + mbParams.getMeasurement().getTime() + " each");
        out.println("# Threads: " + mbParams.getThreads() + " " + getThreadsString(mbParams.getThreads()) + (mbParams.shouldSynchIterations() ? ", will synchronize iterations" : ""));
        out.println("# Benchmark mode: " + name.getMode().longLabel());
        out.println("# Benchmark: " + name.getUsername());
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
                for (Map.Entry<String, Result> res : secondary.entrySet()) {
                    // rough estimate
                    int threads = data.getRawSecondaryResults().get(res.getKey()).size();

                    sb.append(String.format("%" + prefixLen + "s", ""));
                    sb.append("  \"").append(res.getKey()).append("\": ");
                    sb.append(res.getValue().toString());
                    sb.append(" (").append(threads).append(" threads)");
                    sb.append("\n");
                }
            }
        }

        out.print(String.format("%s", sb.toString()));

        // also print out profiler information
        if (type == IterationType.MEASUREMENT) {
            boolean firstProfiler = true;
            for (ProfilerResult profRes : data.getProfilerResults()) {
                if (profRes.hasData()) {
                    if (firstProfiler) {
                        out.println("");
                        firstProfiler = false;
                    }
                    String prefix = profRes.getProfilerName();
                    for (String line : profRes.toString().split("\n")) {
                        out.print(String.format("%12s | %s\n", prefix, line));
                        prefix = "";
                    }
                    out.print(String.format("%12s |\n", ""));
                }
            }
        }

        out.println("");
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
        Collection<String> benchNames = new ArrayList<String>();
        for (BenchmarkRecord key : runResults.keySet()) {
            RunResult runResult = runResults.get(key);
            benchNames.add(key.getUsername());
            for (String label : runResult.getSecondaryResults().keySet()) {
                benchNames.add(key.getUsername() + ":" + label);
            }
        }

        Map<String, String> benchPrefixes = ClassUtils.denseClassNames(benchNames);

        // determine max name
        int nameLen = 1;
        for (String prefix : benchPrefixes.values()) {
            nameLen = Math.max(nameLen, prefix.length());
        }
        nameLen += 2;

        out.printf("%-" + nameLen + "s %6s %9s %12s %12s %8s%n",
                "Benchmark", "Mode", "Samples", "Mean", "Mean error", "Units");
        for (BenchmarkRecord key : runResults.keySet()) {
            RunResult res = runResults.get(key);
            {
                Statistics stats = res.getPrimaryResult().getStatistics();
                out.printf("%-" + nameLen + "s %6s %9d %12.3f %12.3f %8s%n",
                        benchPrefixes.get(key.getUsername()),
                        key.getMode().shortLabel(),
                        stats.getN(),
                        stats.getMean(), stats.getMeanErrorAt(0.999),
                        res.getScoreUnit());
            }

            for (String label : res.getSecondaryResults().keySet()) {
                Statistics stats = res.getSecondaryResults().get(label).getStatistics();

                out.printf("%-" + nameLen + "s %6s %9d %12.3f %12.3f %8s%n",
                        benchPrefixes.get(key.getUsername() + ":" + label),
                        key.getMode().shortLabel(),
                        stats.getN(),
                        stats.getMean(), stats.getMeanErrorAt(0.999),
                        res.getScoreUnit());
            }
        }
    }


}
