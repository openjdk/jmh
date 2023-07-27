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

import org.openjdk.jmh.benchmarks.CoreStabilityBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.validation.AffinitySupport;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;

public class CoreStabilityTest extends ValidationTest {

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- CORE STABILITY TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test verifies the performance for a single test by running it on different CPUs. " +
                        "For perfectly symmetric machines, the performance should be the same across all CPUs. " +
                        "If there is a significant difference between the CPUs, this is usually " +
                        "indicative of asymmetric machine, making the benchmarks that do not explicitly control " +
                        "affinity less reliable.",
                80, 2);
        pw.println();

        if (!AffinitySupport.isSupported()) {
            pw.println("  Affinity control is not available on this machine, skipping the test.");
            pw.println();
            return;
        }

        int threads = Utils.figureOutHotCPUs();

        for (int p = 0; p < threads; p++) {
            pw.printf("  CPU %3d: ", p);
            pw.flush();

            Options opts = new OptionsBuilder()
                    .parent(parent)
                    .include(CoreStabilityBench.class.getCanonicalName())
                    .param("p", String.valueOf(p))
                    .verbosity(VerboseMode.SILENT)
                    .build();

            RunResult result = new Runner(opts).runSingle();
            Result r = result.getPrimaryResult();
            pw.printf(" %16s", String.format("%.2f \u00b1 %.2f %s%n", r.getScore(), r.getScoreError(), r.getScoreUnit()));
            pw.flush();
        }
    }
}
