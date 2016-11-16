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

import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.benchmarks.ThermalRundownBench;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.validation.IterationScoresFormatter;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class ThermalRundownTest implements ValidationTest {

    private final int iterations;

    public ThermalRundownTest(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- THERMAL RUNDOWN TEST");
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test tries to heat the machine up, trying to kick in the thermal throttling. If you see the diminishing " +
                        "performance over time, then your system throttles, and many benchmark experiments are unreliable. ",
                80, 2);
        pw.println();

        Options opts = new OptionsBuilder()
                .parent(parent)
                .include(ThermalRundownBench.class.getCanonicalName())
                .warmupIterations(0)
                .measurementIterations(iterations)
                .measurementTime(TimeValue.seconds(10))
                .threads(Threads.MAX)
                .forks(1)
                .verbosity(VerboseMode.SILENT)
                .build();

        new Runner(opts, new IterationScoresFormatter(pw)).runSingle();

        pw.println();
    }
}
