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

import org.openjdk.jmh.benchmarks.BlackholeConsecutiveBench;
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

public class BlackholeConsecutiveTest implements ValidationTest {
    private final boolean inlined;

    public BlackholeConsecutiveTest(boolean inlined) {
        this.inlined = inlined;
    }

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- BLACKHOLE MERGING TEST" + (!inlined ? " (NORMAL)" : " (INLINE HINTS BROKEN)"));
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test verifies that calling the Blackhole.consume with the same result is not susceptible for " +
                        "merging. We expect the similar performance across all data types, and the number of consecutive " +
                        "calls. If there are significant differences, this is indicative of Blackhole failure, and it is " +
                        "a serious JMH issue.",
                80, 2);
        pw.println();

        if (inlined) {
            org.openjdk.jmh.util.Utils.reflow(pw,
                    "This particular test mode forces the inline of Blackhole methods, and so demolishes one of the layers " +
                            "in defence in depth. If this layer is broken, Blackhole should also survive. If it isn't, then " +
                            "JMH will have to provide more contingencies.",
                    80, 2);
            pw.println();
        }

        String[] types = new String[]  {
                "boolean", "byte",   "short",
                "char",    "int",    "float",
                "long",    "double", "Object",
                "Array",
        };

        int[] ss = new int[] {1, 4, 8};

        pw.println("  Scores are nanoseconds per Blackhole call.");
        pw.println("  Trying " + Arrays.toString(ss) + " consecutive Blackhole calls.");
        pw.println();

        pw.printf("%12s", "");
        for (int steps : ss) {
            pw.printf("%20s", steps);
        }
        pw.println();

        for (String type : types) {
            pw.printf("%12s", type + ": ");
            for (int steps : ss) {
                Options opts = new OptionsBuilder()
                        .parent(parent)
                        .include(BlackholeConsecutiveBench.class.getCanonicalName() + ".test_" + type + "_" + steps)
                        .param("steps", String.valueOf(steps))
                        .verbosity(VerboseMode.SILENT)
                        .build();

                RunResult result = new Runner(opts).runSingle();
                Result r = result.getPrimaryResult();
                pw.printf("%20s", String.format("%.2f ± %.2f", r.getScore() / steps, r.getScoreError() / steps));
                pw.flush();
            }
            pw.println();
        }

        pw.println();
    }
}
