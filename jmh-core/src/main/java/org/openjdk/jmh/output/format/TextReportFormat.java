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

import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.internal.IterationResult;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.IterationParams;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.Statistics;
import org.openjdk.jmh.util.internal.TreeMultimap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * TextReportFormat implementation of OutputFormat.
 *
 * @author Brian Doherty
 * @author Aleksey Shipilev
 */
public class TextReportFormat extends PrettyPrintFormat {

    private final Multimap<BenchmarkIdentifier, IterationResult> benchmarkResults;
    private final Map<BenchmarkRecord, IterationParams> benchmarkSettings;

    public TextReportFormat(PrintStream out, boolean verbose) {
        super(out, verbose);
        benchmarkResults = new TreeMultimap<BenchmarkIdentifier, IterationResult>();
        benchmarkSettings = new TreeMap<BenchmarkRecord, IterationParams>();
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, MicroBenchmarkParameters mbParams, boolean verbose) {
        super.startBenchmark(name, mbParams, verbose);
        benchmarkSettings.put(name, mbParams.getIteration());
    }

    @Override
    public void iterationResult(BenchmarkRecord name, int iteration, IterationType type, int thread, IterationResult result, Collection<ProfilerResult> profiles) {
        super.iterationResult(name, iteration, type, thread, result, profiles);
        if (type == IterationType.MEASUREMENT) {
            benchmarkResults.put(new BenchmarkIdentifier(name, thread), result);
        }
    }

    @Override
    public void endRun(String message) {
        super.endRun(null);

        // generate the full report
        //

        if (benchmarkResults.isEmpty()) {
            return;
        }

        Collection<String> benchNames = new ArrayList<String>();
        for (BenchmarkIdentifier key : benchmarkResults.keys()) {
            Collection<IterationResult> results = benchmarkResults.get(key);
            if (results != null) {
                List<Result> iResults = new ArrayList<Result>();
                for (IterationResult res : results) {
                    iResults.addAll(res.getResult().values());
                }

                if (!iResults.isEmpty()) {
                    RunResult runResult = new RunResult(iResults);

                    boolean onlyResult = runResult.getStatistics().size() <= 1;
                    for (String label : runResult.getStatistics().keySet()) {
                        benchNames.add(key.benchmark.getUsername() + (onlyResult ? "" : ":" + label));
                    }
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

            boolean resultOK = false;
            if (results != null) {
                List<Result> iResults = new ArrayList<Result>();
                for (IterationResult res : results) {
                    iResults.addAll(res.getResult().values());
                }

                if (!iResults.isEmpty()) {
                    RunResult runResult = new RunResult(iResults);

                    boolean onlyResult = runResult.getStatistics().size() <= 1;
                    for (String label : runResult.getStatistics().keySet()) {
                        Statistics stats = runResult.getStatistics().get(label);
                        if (stats.getN() > 2) {
                            interval = stats.getConfidenceInterval(0.01);
                        }

                        out.printf("%-" + nameLen + "s %6s %3d %6d %4d %12.3f %12.3f %8s%n",
                                benchPrefixes.get(key.benchmark.getUsername() + (onlyResult ? "" : ":" + label)),
                                key.benchmark.getMode().shortLabel(),
                                key.threads, stats.getN(),
                                settings.getTime().convertTo(TimeUnit.SECONDS),
                                stats.getMean(), (interval[1] - interval[0]) / 2,
                                runResult.getScoreUnit());
                    }
                    resultOK = true;
                }
            }

            if (!resultOK) {
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
