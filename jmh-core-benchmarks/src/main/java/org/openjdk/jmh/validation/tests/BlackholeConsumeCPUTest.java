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

import org.openjdk.jmh.benchmarks.BlackholeConsumeCPUBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class BlackholeConsumeCPUTest extends ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- BLACKHOLE CONSUME CPU TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test assesses the Blackhole.consumeCPU performance, that should be linear to " +
                        "the number of tokens. The performance can be slightly different on low token " +
                        "counts. Otherwise, the backoffs with consumeCPU are not reliable. ",
                80, 2);
        pw.println();

        pw.println("  Scores are (normalized) nanoseconds per token.");
        pw.println();

        pw.printf("%20s%n", "#Tokens: ");

        for (int delay : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                20, 30, 40, 50, 60, 70, 80, 90, 100, 500, 1_000,
                5_000, 10_000, 50_000, 100_000, 500_000, 1_000_000, 5_000_000, 10_000_000}) {
            Options opts = new OptionsBuilder()
                    .parent(parent)
                    .include(BlackholeConsumeCPUBench.class.getCanonicalName())
                    .verbosity(VerboseMode.SILENT)
                    .param("delay", String.valueOf(delay))
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();

            pw.printf("%20s", delay + ": ");
            pw.flush();
            pw.printf("%.2f ± %.2f ns\n", r.getScore() / delay, r.getScoreError() / delay);
        }

        pw.println();
    }
}
