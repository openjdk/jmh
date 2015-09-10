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

import org.openjdk.jmh.benchmarks.BlackholePipelineBench;
import org.openjdk.jmh.benchmarks.BlackholePipelinePayloadBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;
import java.util.Arrays;

public class BlackholePipelinedTest implements ValidationTest {
    private final boolean payload;
    private final boolean inlined;

    public BlackholePipelinedTest(boolean payload, boolean inlined) {
        this.payload = payload;
        this.inlined = inlined;
    }

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- BLACKHOLE PIPELINED TEST" + (payload ? " + REAL PAYLOAD" : "") + (!inlined ? " (NORMAL)" : " (INLINE HINTS BROKEN)"));
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test shows the Blackhole performance in a loop with a given number of iterations. We should normally " +
                        "see the uniform numbers across most data types and number of iterations. If the numbers are wildly " +
                        "non-uniform across the number of iteration, this is indicative of Blackhole failure, and may point " +
                        "to a serious JMH issue. Scores are nanoseconds per loop iteration.",
                80, 2);
        pw.println();

        if (payload) {
            pw.println("  Real payload is being injected into the benchmark.");
            pw.println();
        }
        if (inlined) {
            org.openjdk.jmh.util.Utils.reflow(pw,
                    "This particular test mode forces the inline of Blackhole methods, and so demolishes one of the layers " +
                            "in defence in depth. If this layer is broken, Blackhole should also survive. If it isn't, then " +
                            "JMH will have to provide more contingencies.",
                    80, 2);
            pw.println();
        }
        pw.println();

        String[] types = new String[]  {
                "boolean", "byte",   "short",
                "char",    "int",    "float",
                "long",    "double", "Object",
                "Array",
        };

        int[] ss = {1, 10, 100, 1000, 10000};

        pw.println("  Scores are nanoseconds per (normalized) benchmark op.");
        pw.println("  Trying loops with " + Arrays.toString(ss) + " iterations.");
        pw.println();

        String canonicalName =
                (payload ? BlackholePipelinePayloadBench.class : BlackholePipelineBench.class).getCanonicalName();

        pw.printf("%12s", "");
        for (int steps : ss) {
            pw.printf("%16s", steps);
        }
        pw.println();

        for (String type : types) {
            pw.printf("%12s", type + ": ");
            for (int steps : ss) {

                Options opts = new OptionsBuilder()
                        .parent(parent)
                        .include(canonicalName + ".test_" + type)
                        .param("steps", String.valueOf(steps))
                        .verbosity(VerboseMode.SILENT)
                        .build();

                RunResult result = new Runner(opts).runSingle();
                Result r = result.getPrimaryResult();
                pw.printf("%16s", String.format("%.2f ± %.2f", r.getScore() / steps, r.getScoreError() / steps));
                pw.flush();
            }
            pw.println();
        }

        pw.println();
    }
}
