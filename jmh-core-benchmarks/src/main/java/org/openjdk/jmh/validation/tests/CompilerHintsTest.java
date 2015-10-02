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

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.benchmarks.CompilerHintsBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class CompilerHintsTest implements ValidationTest {
    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- COMPILER HINTS TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This tests verifies compiler hints are working as expected. Two baseline tests run the workload " +
                        "in inlined and non-inlined regiments. When the workload is inlined, the optimizations should "  +
                        "kill the workload body. Compiler hints should successfully survive in both regiments: " +
                        CompilerControl.Mode.INLINE + " should always inline, and " + CompilerControl.Mode.DONT_INLINE + " " +
                        "should always break inlining. " + CompilerControl.Mode.EXCLUDE + " should be neutral to inlining " +
                        "policy completely.",
                80, 2);
        pw.println();

        doWith(parent, pw, "baseI_baseline",    "Default inline policy");
        doWith(parent, pw, "baseI_inline",      "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.INLINE + ")");
        doWith(parent, pw, "baseI_dontInline",  "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.DONT_INLINE + ")");
        doWith(parent, pw, "baseI_exclude",     "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.EXCLUDE + ")");

        pw.println();

        doWith(parent, pw, "baseNI_baseline", "Default no inline policy");
        doWith(parent, pw, "baseNI_inline",     "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.INLINE + ")");
        doWith(parent, pw, "baseNI_dontInline", "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.DONT_INLINE + ")");
        doWith(parent, pw, "baseNI_exclude",    "  + @" + CompilerControl.class.getSimpleName() + "(" + CompilerControl.Mode.EXCLUDE + ")");

        pw.println();
    }

    private void doWith(Options parent, PrintWriter pw, String test, String descr) throws RunnerException {
        Options opts = new OptionsBuilder()
                .parent(parent)
                .include(CompilerHintsBench.class.getCanonicalName() + "." + test + "$")
                .verbosity(VerboseMode.SILENT)
                .build();

        RunResult result = new Runner(opts).runSingle();
        Result r = result.getPrimaryResult();

        pw.printf("%50s", descr + ": ");
        pw.flush();
        pw.printf("%.2f \u00b1 %.2f ns\n", r.getScore(), r.getScoreError());
    }
}
