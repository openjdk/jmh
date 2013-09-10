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
package org.openjdk.jmh.benchmarks;

import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

public class MainValidation {

    public static void main(String[] args) throws RunnerException {

        PrintWriter pw = new PrintWriter(System.out, true);

        pw.println("JMH Infrastructure Validation Tests");
        pw.println("-------------------------------------------------------------------------");
        pw.println();

        timingMeasurements(pw);

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
                    .outputFormat(OutputFormatType.Silent)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%40s", "System.nanoTime latency: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getStatistics().getMeanError(0.95));

            latency = (long) r.getScore();
        }

        long granularity;
        {
            Options opts = new OptionsBuilder()
                    .include(NanoTimerBench.class.getCanonicalName() + ".*granularity")
                    .outputFormat(OutputFormatType.Silent)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%40s", "System.nanoTime granularity: ");
            pw.flush();
            pw.printf("%.2f +- %.2f ns\n", r.getScore(), r.getStatistics().getMeanError(0.95));
            granularity = (long) r.getScore();
        }

        pw.println();
        pw.printf("   Verdict: ");
        if (latency < 100 && granularity < 100) {
            pw.println("OK!");
        } else {
            pw.println("WARNING, latency and/or granularity is suspiciously high.");
        }
    }

}
