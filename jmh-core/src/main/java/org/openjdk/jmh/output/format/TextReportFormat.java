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

import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.Statistics;
import org.openjdk.jmh.util.internal.TreeMultimap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * TextReportFormat implementation of OutputFormat.
 *
 * @author Brian Doherty
 * @author Aleksey Shipilev
 */
public class TextReportFormat extends AbstractOutputFormat {

    private final Map<BenchmarkRecord, IterationParams> benchmarkSettings;
    private final Multimap<BenchmarkIdentifier, IterationResult> benchmarkResults;
    private final Multimap<BenchmarkRecord, RunResult> benchmarkRunResults;

    public TextReportFormat(PrintStream out, boolean verbose) {
        super(out, verbose);
        benchmarkSettings = new TreeMap<BenchmarkRecord, IterationParams>();
        benchmarkResults = new TreeMultimap<BenchmarkIdentifier, IterationResult>();
        benchmarkRunResults = new TreeMultimap<BenchmarkRecord, RunResult>();
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, BenchmarkParams mbParams, boolean verbose) {
        if (verbose) {
            out.println("# Starting run at: " + new Date());
        }

        out.println("# Warmup: " + mbParams.getWarmup().getCount() + " iterations, " + mbParams.getWarmup().getTime() + " each");
        out.println("# Measurement: " + mbParams.getIteration().getCount() + " iterations, " + mbParams.getIteration().getTime() + " each");
        out.println("# Threads: " + mbParams.getThreads() + " " + getThreadsString(mbParams.getThreads()) + (mbParams.shouldSynchIterations() ? ", will synchronize iterations" : ""));
        out.println("# Benchmark mode: " + name.getMode().longLabel());
        out.println("# Running: " + name.getUsername());

        benchmarkSettings.put(name, mbParams.getIteration());
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

    @Override
    public void detailedResults(BenchmarkRecord name, IterationParams params, int iteration, IterationResult data) {
        out.print("Results per thread: [");

        boolean first = true;
        for (Result result : data.getRawPrimaryResults()) {
            if (!first) {
                out.print(", ");
            }

            out.printf("%.1f", result.getScore());
            first = false;
        }

        out.println("]");
        out.println();
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
        if (type == IterationType.MEASUREMENT) {
            benchmarkResults.put(new BenchmarkIdentifier(name, params.getThreads()), data);
        }
    }

    @Override
    public void endBenchmark(BenchmarkRecord name, RunResult result) {
        benchmarkRunResults.put(name, result);

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
    public void endRun() {
        for (BenchmarkRecord key1 : benchmarkRunResults.keys()) {
            Collection<RunResult> forkedResults = benchmarkRunResults.get(key1);
            if (forkedResults.size() > 1) {
                out.println("\"" + key1.getUsername() + "\", aggregate over forked runs:");
                out.println();

                RunResult runResult1 = RunResult.merge(forkedResults);

                out.println(runResult1.getPrimaryResult().extendedInfo(null));
                for (Result r : runResult1.getSecondaryResults().values()) {
                    out.println(r.extendedInfo(r.getLabel()));
                }
            }
        }

        // generate the full report
        //

        if (benchmarkResults.isEmpty()) {
            return;
        }

        Collection<String> benchNames = new ArrayList<String>();
        for (BenchmarkIdentifier key : benchmarkResults.keys()) {
            Collection<IterationResult> results = benchmarkResults.get(key);
            if (results != null && !results.isEmpty()) {
                RunResult runResult = new RunResult(results);

                benchNames.add(key.benchmark.getUsername());
                for (String label : runResult.getSecondaryResults().keySet()) {
                    benchNames.add(key.benchmark.getUsername() + ":" + label);
                }
            }
        }

        Map<String, String> benchPrefixes = ClassUtils.denseClassNames(benchNames);

        // determine max name
        int nameLen = 1;
        for (String prefix : benchPrefixes.values()) {
            nameLen = Math.max(nameLen, prefix.length());
        }
        nameLen += 2;

        out.printf("%-" + nameLen + "s %6s %3s %6s %4s %12s %12s %8s%n",
                "Benchmark", "Mode", "Thr", "Cnt", "Sec",
                "Mean", "Mean error", "Units");
        for (BenchmarkIdentifier key : benchmarkResults.keys()) {

            double[] interval = new double[]{Double.NaN, Double.NaN};

            IterationParams settings = benchmarkSettings.get(key.benchmark);
            Collection<IterationResult> results = benchmarkResults.get(key);

            if (results != null && !results.isEmpty()) {
                RunResult runResult = new RunResult(results);

                {
                    Statistics stats = runResult.getPrimaryResult().getStatistics();
                    if (stats.getN() > 2) {
                        interval = stats.getConfidenceInterval(0.01);
                    }

                    out.printf("%-" + nameLen + "s %6s %3d %6d %4d %12.3f %12.3f %8s%n",
                            benchPrefixes.get(key.benchmark.getUsername()),
                            key.benchmark.getMode().shortLabel(),
                            key.threads, stats.getN(),
                            settings.getTime().convertTo(TimeUnit.SECONDS),
                            stats.getMean(), (interval[1] - interval[0]) / 2,
                            runResult.getScoreUnit());
                }

                for (String label : runResult.getSecondaryResults().keySet()) {
                    Statistics stats = runResult.getSecondaryResults().get(label).getStatistics();
                    if (stats.getN() > 2) {
                        interval = stats.getConfidenceInterval(0.01);
                    }

                    out.printf("%-" + nameLen + "s %6s %3d %6d %4d %12.3f %12.3f %8s%n",
                            benchPrefixes.get(key.benchmark.getUsername() + ":" + label),
                            key.benchmark.getMode().shortLabel(),
                            key.threads, stats.getN(),
                            settings.getTime().convertTo(TimeUnit.SECONDS),
                            stats.getMean(), (interval[1] - interval[0]) / 2,
                            runResult.getScoreUnit());
                }
            } else {
                out.printf("%-" + nameLen + "s %6s, %3d %6d %4d %12.3f %12.3f %8s%n",
                        benchPrefixes.get(key.benchmark.getUsername()),
                        key.benchmark.getMode().shortLabel(),
                        key.threads, 0,
                        settings.getTime().convertTo(TimeUnit.SECONDS),
                        Double.NaN, Double.NaN,
                        "N/A");
            }

        }
        benchmarkResults.clear();
        benchmarkSettings.clear();
    }

    private static class BenchmarkIdentifier implements Comparable<BenchmarkIdentifier> {
        final BenchmarkRecord benchmark;
        final int threads;

        BenchmarkIdentifier(BenchmarkRecord benchmark, int threads) {
            this.benchmark = benchmark;
            this.threads = threads;
        }

        public BenchmarkIdentifier(BenchmarkIdentifier copy) {
            this.benchmark = copy.benchmark;
            this.threads = copy.threads;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BenchmarkIdentifier that = (BenchmarkIdentifier) o;

            if (threads != that.threads) {
                return false;
            }
            if (benchmark != null ? !benchmark.equals(that.benchmark) : that.benchmark != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = benchmark != null ? benchmark.hashCode() : 0;
            result = 31 * result + threads;
            return result;
        }

        @Override
        public int compareTo(BenchmarkIdentifier o) {
            int c1 = benchmark.compareTo(o.benchmark);
            if (c1 == 0) {
                return ((Integer) threads).compareTo(o.threads);
            } else {
                return c1;
            }
        }
    }

}
