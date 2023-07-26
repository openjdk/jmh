/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

import org.openjdk.jmh.benchmarks.LongStabilityBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;

public class LongStabilityTest extends ValidationTest {
    private final int tries;

    public LongStabilityTest(int tries) {
        this.tries = tries;
    }

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- LONG STABILITY TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test verifies the performance for a single test by running it several times with some " +
                        "delays between the runs. The performance should be the same across all runs. " +
                        "If there is a significant difference between the runs, this is usually " +
                        "indicative of noisy environment, e.g. a busy virtualized node, or background processes " +
                        "interfering with the run, making the benchmarks unreliable.",
                80, 2);
        pw.println();

        for (int t = 0; t < tries; t++) {
            int ms = (t == 0) ? 0 : ThreadLocalRandom.current().nextInt(5_000, 30_000);

            pw.printf("  Sleeping for %6d ms...", ms);
            pw.flush();
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                // Do nothing.
            }

            pw.print("  Run: ");
            pw.flush();

            Options opts = new OptionsBuilder()
                    .parent(parent)
                    .include(LongStabilityBench.class.getCanonicalName())
                    .warmupIterations(5)
                    .warmupTime(TimeValue.seconds(1))
                    .measurementIterations(5)
                    .measurementTime(TimeValue.seconds(1))
                    .forks(1)
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();
            pw.printf(" %16s", String.format("%.2f \u00b1 %.2f %s%n", r.getScore(), r.getScoreError(), r.getScoreUnit()));
            pw.flush();
        }
    }
}
