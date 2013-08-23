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
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.Statistics;
import org.openjdk.jmh.util.internal.TreeMultimap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * PrettyPrint implementation of OutputFormat.
 *
 * @author anders.astrand@oracle.com
 */
public class PrettyPrintFormat extends AbstractOutputFormat {

    public PrettyPrintFormat(PrintStream out, boolean verbose) {
        super(out, verbose);
    }

    @Override
    public void iterationResult(BenchmarkRecord name, int iteration, IterationType type, int thread, IterationResult result, Collection<ProfilerResult> profiles) {
        out.print(String.format("%s", result.toPrintable()));

        // also print out profiler information
        if (type == IterationType.MEASUREMENT) {
            boolean firstProfiler = true;
            for (ProfilerResult profRes : profiles) {
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
    public void startBenchmark(BenchmarkRecord name, MicroBenchmarkParameters mbParams, boolean verbose) {
        if (verbose) {
            out.println("# Starting run at: " + new Date());
        }

        out.println("# Runtime (per iteration): " + mbParams.getIteration().getTime());
        out.println("# Iterations: " + mbParams.getIteration().getCount());
        out.println("# Thread count: " + mbParams.getThreads());

        if (mbParams.shouldSynchIterations()) {
            out.println("# Threads will synchronize iterations");
        }

        out.println("# Benchmark mode: " + name.getMode().longLabel());
        out.println("# Running: " + name.getUsername());
    }

    private final Multimap<BenchmarkRecord, RunResult> benchmarkResults = new TreeMultimap<BenchmarkRecord, RunResult>();

    @Override
    public void endBenchmark(BenchmarkRecord name, RunResult result) {
        benchmarkResults.put(name, result);

        out.println();
        for (Result r : result.getResult().values()) {
            out.println(r.extendedInfo());
        }
        out.println();
    }

    @Override
    public void startRun() {
        // do nothing
    }

    @Override
    public void endRun() {
        for (BenchmarkRecord key : benchmarkResults.keys()) {
            Collection<RunResult> forkedResults = benchmarkResults.get(key);
            if (forkedResults.size() > 1) {
                out.println("\"" + key.getUsername() + "\", aggregate over forked runs:");

                List<Result> iResults = new ArrayList<Result>();
                for (RunResult res : forkedResults) {
                    iResults.addAll(res.getResult().values());
                }
                RunResult runResult = new RunResult(iResults);

                for (Result r : runResult.getResult().values()) {
                    out.println(r.extendedInfo());
                }
            }
        }
    }

    @Override
    public void iteration(BenchmarkRecord benchmark, int iteration, IterationType type, int threads, TimeValue timeValue) {
        switch (type) {
            case WARMUP:
                out.print(String.format("# Warmup Iteration %3d (%s in %d %s): ", iteration,
                        timeValue, threads, getThreadsString(threads)));
                break;
            case MEASUREMENT:
                out.print(String.format("Iteration %3d (%s in %d %s): ", iteration,
                        timeValue, threads, getThreadsString(threads)));
                break;
            default:
                throw new IllegalStateException("Unknown iteration type: " + type);
        }
        out.flush();
    }

    @Override
    public void detailedResults(BenchmarkRecord name, int iteration, int threads, IterationResult results) {
        out.print("Results per thread: [");

        boolean first = true;
        Multimap<String, Result> subresults = results.getSubresults();
        for (String label : subresults.keys()) {
            for (Result result : subresults.get(label)) {
                if (!first) {
                    out.print(", ");
                }

                out.printf("%.1f", result.getScore());
                first = false;
            }
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
}
