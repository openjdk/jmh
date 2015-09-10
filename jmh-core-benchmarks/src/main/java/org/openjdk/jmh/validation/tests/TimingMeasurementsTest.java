/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.validation.tests;

import org.openjdk.jmh.benchmarks.NanoTimerBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class TimingMeasurementsTest implements ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- TIMING MEASUREMENTS TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test shows the minimal individual timings possible to measure. " +
                        "This normally affects only SampleTime and SingleShot benchmark modes. " +
                        "Throughput/AverageTime tests can do better since they do only a few " +
                        "timestamps before and after the complete iteration.",
                80, 2);
        pw.println();

        {
            Options opts = new OptionsBuilder()
                    .parent(parent)
                    .include(NanoTimerBench.class.getCanonicalName() + ".latency$")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "System.nanoTime latency: ");
            pw.flush();
            pw.printf("%.2f \u00b1 %.2f ns\n", r.getScore(), r.getScoreError());
        }

        {
            Options opts = new OptionsBuilder()
                    .parent(parent)
                    .include(NanoTimerBench.class.getCanonicalName() + ".granularity$")
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%50s", "System.nanoTime granularity: ");
            pw.flush();
            pw.printf("%.2f \u00b1 %.2f ns\n", r.getScore(), r.getScoreError());
        }

        pw.println();
    }
}
