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
import org.openjdk.jmh.benchmarks.ScoreStabilityBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class ScoreStabilityTest implements ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- SCORE STABILITY TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test verifies the performance for a large busy benchmark is the same, regardless " +
                        "of the benchmark mode, and delay before the iteration. The performance should be " +
                        "the same across all delay values, and comparable across different benchmark modes. " +
                        "If there is a significant difference on different delay levels, this is usually " +
                        "indicative of power-saving features enabled, making bursty benchmarks unreliable.",
                80, 2);
        pw.println();

        pw.println("  Scores are milliseconds per benchmark operation, or the reciprocal to it.");
        pw.println("  Delays are injected before each iteration, and are measured in milliseconds.");
        pw.println();

        int[] delays = {0, 1, 10, 100, 1000};

        pw.printf("%20s", "");
        for (int delay : delays) {
            pw.printf("%16s", delay);
        }
        pw.println();

        for (Mode m : Mode.values()) {
            if (m == Mode.All) continue;

            Result r = null;

            pw.printf("%20s", m + ": ");
            for (int delay : delays) {
                Options opts = new OptionsBuilder()
                        .parent(parent)
                        .mode(m)
                        .include(ScoreStabilityBench.class.getCanonicalName())
                        .verbosity(VerboseMode.SILENT)
                        .param("delay", String.valueOf(delay))
                        .build();

                RunResult result = new Runner(opts).runSingle();
                r = result.getPrimaryResult();
                pw.printf("%16s", String.format("%.2f \u00b1 %.2f", r.getScore(), r.getScoreError()));
                pw.flush();
            }
            pw.println("   " + r.getScoreUnit());
        }

        pw.println();
    }
}
