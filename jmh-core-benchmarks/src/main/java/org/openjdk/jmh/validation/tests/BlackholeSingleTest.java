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

import org.openjdk.jmh.benchmarks.BlackholeBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class BlackholeSingleTest implements ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- BLACKHOLE SINGLE INVOCATION TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test shows the Blackhole overheads, when using a single invocation in the method, " +
                        "whether implicitly via return from @Benchmark, or explicitly via consume(). The performance " +
                        "should be the same for implicit and explicit cases, and comparable across all data types. ",
                80, 2);
        pw.println();

        String[] types = new String[]  {
                "boolean", "byte",   "short",
                "char",    "int",    "float",
                "long",    "double", "Object",
                "Array",
        };

        String[] modes = {"implicit", "explicit"};

        pw.println("  Scores are nanoseconds per benchmark op.");
        pw.println();

        pw.printf("%20s", "");
        for (String mode : modes) {
            pw.printf("%20s", mode);
        }
        pw.println();

        for (String type : types) {
            pw.printf("%20s", type + ": ");
            for (String impl : modes) {
                Options opts = new OptionsBuilder()
                        .parent(parent)
                        .include(BlackholeBench.class.getCanonicalName() + "." + impl + "_" + type)
                        .verbosity(VerboseMode.SILENT)
                        .build();

                RunResult result = new Runner(opts).runSingle();
                Result r = result.getPrimaryResult();

                pw.flush();
                pw.printf("%20s", String.format("%.2f ± %.2f ns", r.getScore(), r.getScoreError()));
            }
            pw.println();
        }
        pw.println();
    }
}
