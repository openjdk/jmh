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

import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.internal.IterationResult;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.PrintStream;
import java.util.Collection;

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
    public void iterationResult(BenchmarkRecord name, int iteration, int thread, IterationResult result, Collection<ProfilerResult> profiles) {
        out.print(name + DELIMITER + iteration + DELIMITER);
        for (Result r : result.getResult().values()) {
            out.print(convertDouble(r.getScore()) + DELIMITER);
        }
        out.print(thread + DELIMITER + result.getScoreUnit() + DELIMITER);
        for (Result r : result.getSubresults().values()) {
            out.print(convertDouble(r.getScore()) + DELIMITER);
        }
        out.println();
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, MicroBenchmarkParameters mbParams, boolean verbose) {
        if (!headerPrinted) {
            headerPrinted = true;

            out.print("Benchmark" + DELIMITER + "Iteration" + DELIMITER + "Score" + DELIMITER + "Threads" + DELIMITER + "Unit");

            for (int i = 1; i < mbParams.getMaxThreads() + 1; i++) {
                out.print(DELIMITER + "Thread " + i);
            }

            out.println(DELIMITER);
        }

    }

    @Override
    public void endBenchmark(BenchmarkRecord name, RunResult result) {
        // don't print anything
    }

    @Override
    public void startRun(String message) {
        // don't print anything
    }

    @Override
    public void endRun(String message) {
        // don't print anything
    }

    @Override
    public void iteration(BenchmarkRecord benchmark, int iteration, int threads, TimeValue runTime) {
        // don't print anything
    }

    @Override
    public void warmupIteration(BenchmarkRecord benchmark, int iteration, int threads, TimeValue warmupTime) {
        // don't print anything
    }

    @Override
    public void warmupIterationResult(BenchmarkRecord benchmark, int iteration, int thread, IterationResult result) {
        // don't print anything
    }

    private static String convertDouble(double d) {
        return String.format("\"%.3f\"", d);
    }

    @Override
    public void detailedResults(BenchmarkRecord name, int iteration, int threads, IterationResult results) {
        int count = 0;

        for (Result result : results.getSubresults().values()) {
            out.print(convertDouble(result.getScore()));
            out.print(DELIMITER);
            count++;
        }

        // print tail
        for (int i = count; i < threads; i++) {
            out.print(DELIMITER);
        }

        out.println(DELIMITER);
    }

    @Override
    public void threadSubStatistics(BenchmarkRecord name, int threads, RunResult result) {

    }
}
