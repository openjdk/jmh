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
package org.openjdk.jmh;

import org.openjdk.jmh.benchmarks.EmptyBench;
import org.openjdk.jmh.benchmarks.LevelInvocationBench;
import org.openjdk.jmh.benchmarks.LevelIterationBench;
import org.openjdk.jmh.benchmarks.LevelTrialBench;
import org.openjdk.jmh.benchmarks.NanoTimerBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.PrintWriter;

public class MainValidation {

    public static void main(String[] args) throws RunnerException {

        PrintWriter pw = new PrintWriter(System.out, true);

        pw.println("JMH Infrastructure Validation Tests");
        pw.println("-------------------------------------------------------------------------");
        pw.println();

        timingMeasurements(pw);
        basic(pw);
    }

    private static void basic(PrintWriter pw) throws RunnerException {
        pw.println("Basic benchmark tests:");
        pw.println("  These tests show the basic overheads of calling the benchmark method.");
        pw.println("  Additionally, the overheads of calling empty Fixture methods are measured.");
        pw.println("  ");

        {
            Options opts = new OptionsBuilder()
                    .include(EmptyBench.class.getCanonicalName() + ".*")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "running empty benchmark: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelTrialBench.class.getCanonicalName() + ".*benchmark_setup")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Trial, Scope.Benchmark, @Setup: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelTrialBench.class.getCanonicalName() + ".*benchmark_teardown")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Trial, Scope.Benchmark, @TearDown: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelIterationBench.class.getCanonicalName() + ".*benchmark_setup")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Iteration, Scope.Benchmark, @Setup: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelIterationBench.class.getCanonicalName() + ".*benchmark_teardown")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Iteration, Scope.Benchmark, @TearDown: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelInvocationBench.class.getCanonicalName() + ".*benchmark_setup")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Invocation, Scope.Benchmark, @Setup: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .include(LevelInvocationBench.class.getCanonicalName() + ".*benchmark_teardown")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "Level.Invocation, Scope.Benchmark, @TearDown: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
        }

        pw.println();
    }

    private static void timingMeasurements(PrintWriter pw) throws RunnerException {
        pw.println("Timing measurements test");
        pw.println("  This test shows the minimal individual timing possible to measure.");
        pw.println("  This normally affects only SampleTime and SingleShot benchmark modes.");
        pw.println("  Throughput/AverageTime tests can do better since they do only a few");
        pw.println("  timestamps before and after the complete iteration.");
        pw.println("  ");

        long latency;
        {
            Options opts = new OptionsBuilder()
                    .include(NanoTimerBench.class.getCanonicalName() + ".*latency")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "System.nanoTime latency: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());

            latency = (long) r.getScore();
        }

        long granularity;
        {
            Options opts = new OptionsBuilder()
                    .include(NanoTimerBench.class.getCanonicalName() + ".*granularity")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "System.nanoTime granularity: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getScoreError());
            granularity = (long) r.getScore();
        }

        pw.println();
        pw.printf("   Verdict: ");
        if (latency < 100 && granularity < 100) {
            pw.println("OK!");
        } else {
            pw.println("WARNING, latency and/or granularity is suspiciously high.");
        }
        pw.println();
    }

}
