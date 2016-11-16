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

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.benchmarks.ThreadScalingBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.TreeSet;

public class ThreadScalingTest implements ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- THREAD SCALING TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test verifies the performance when scaling in multiple threads. " +
                        "In " + Mode.Throughput + " mode, the benchmark should scale almost linearly, at least before " +
                        "the number of physical cores is reached. In other modes, the timings for individual ops should " +
                        "stay roughly the same, at least before the number of physical cores is reached. The departure " +
                        "from the expected behavior might be indicative of scheduling irregularities, power saving " +
                        "features being enabled, process affinity enforced in virtualized environments, etc. -- these may " +
                        "potentially disrupt multi-threaded benchmarks correctness.",
                80, 2);
        pw.println();

        pw.println("  Scores are relative to a single-threaded case.");
        pw.println("  Threads are scaled from 1 to the number of hardware threads.");
        pw.println();

        SortedSet<Integer> threads = new TreeSet<>();
        int max = Utils.figureOutHotCPUs();
        for (int t = max; t > 0; t /= 2) {
            threads.add(t);
        }
        threads.add(1);
        threads.add(2);

        pw.printf("%20s", "");
        for (int delay : threads) {
            pw.printf("%16s", delay);
        }
        pw.println();

        for (Mode m : Mode.values()) {
            if (m == Mode.All) continue;

            Result r;

            pw.printf("%20s", m + ": ");

            double base = 0.0;
            double baseError = 0.0;
            for (int t : threads) {
                Options opts = new OptionsBuilder()
                        .parent(parent)
                        .mode(m)
                        .include(ThreadScalingBench.class.getCanonicalName())
                        .verbosity(VerboseMode.SILENT)
                        .threads(t)
                        .build();

                RunResult result = new Runner(opts).runSingle();
                r = result.getPrimaryResult();

                double score = r.getScore();
                double error = r.getScoreError();
                if (t == 1) {
                    base = score;
                    baseError = error;
                }

                // https://en.wikipedia.org/wiki/Propagation_of_uncertainty#Simplification
                //
                // For f(x, y) = x/y, boldly assuming x and y are independent,
                //   f_err(x, y) = sqrt(x_err^2 + f(x, y)^2 * y_err^2) / y

                double f = score / base;
                double f_err = Math.sqrt(Math.pow(error, 2) + Math.pow(f, 2) * Math.pow(baseError, 2)) / base;

                pw.printf("%16s", String.format("%.2fx \u00b1 %.2fx", f, f_err));
                pw.flush();
            }
            pw.println();
        }

        pw.println();
    }
}
