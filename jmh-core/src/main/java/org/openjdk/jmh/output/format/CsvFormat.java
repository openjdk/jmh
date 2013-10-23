/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.output.format;

import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;

import java.io.PrintStream;
import java.util.Map;

/**
 * CSV implementation of OutputFormat.
 *
 * @author anders.astrand@oracle.com
 */
public class CsvFormat extends AbstractOutputFormat {

    /** We're delimiting with a semicolon */
    public static final String DELIMITER = ", ";

    private boolean headerPrinted;

    public CsvFormat(PrintStream out, boolean verbose) {
        super(out, verbose);
    }

    @Override
    public void iterationResult(BenchmarkRecord name, IterationParams params, int iteration, IterationType type, IterationResult data) {
        if (type != IterationType.MEASUREMENT) return;

        out.print(name + DELIMITER + iteration + DELIMITER);
        out.print(convertDouble(data.getPrimaryResult().getScore()) + DELIMITER);
        out.print(params.getThreads() + DELIMITER + data.getScoreUnit() + DELIMITER);

        for (Result r : data.getRawPrimaryResults()) {
            out.print(convertDouble(r.getScore()) + DELIMITER);
        }
        out.println();
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, BenchmarkParams mbParams, boolean verbose) {
        if (!headerPrinted) {
            headerPrinted = true;

            out.print("Benchmark" + DELIMITER + "Iteration" + DELIMITER + "Score" + DELIMITER + "Threads" + DELIMITER + "Unit");

            for (int i = 1; i < mbParams.getThreads() + 1; i++) {
                out.print(DELIMITER + "Thread " + i);
            }

            out.println(DELIMITER);
        }

    }

    @Override
    public void endBenchmark(BenchmarkRecord name, BenchResult result) {
        // do nothing
    }

    @Override
    public void startRun() {
        // don't print anything
    }

    @Override
    public void endRun(Map<BenchmarkRecord, RunResult> result) {
        // do nothing
    }

    @Override
    public void iteration(BenchmarkRecord benchmark, IterationParams params, int iteration, IterationType type) {
        // don't print anything
    }

    private static String convertDouble(double d) {
        return String.format("\"%.3f\"", d);
    }

}
