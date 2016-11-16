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
package org.openjdk.jmh.runner.format;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;

import java.io.IOException;
import java.util.Collection;

/**
 * Internal interface for OutputFormat.
 */
public interface OutputFormat {

    /**
     * Format for iteration start.
     *
     * @param benchParams benchmark parameters
     * @param params iteration params in use
     * @param iteration iteration-number
     */
    void iteration(BenchmarkParams benchParams, IterationParams params, int iteration);

    /**
     * Format for end-of-iteration.
     *
     * @param benchParams      name of benchmark
     * @param params    iteration params in use
     * @param iteration iteration-number
     * @param data    result of iteration
     */
    void iterationResult(BenchmarkParams benchParams, IterationParams params, int iteration, IterationResult data);

    /**
     * Format for start-of-benchmark output.
     * @param benchParams benchmark params
     */
    void startBenchmark(BenchmarkParams benchParams);

    /**
     * Format for end-of-benchmark.
     *
     * @param result statistics of the run
     */
    void endBenchmark(BenchmarkResult result);

    /**
     * Format for start-of-benchmark output.
     */
    void startRun();

    /**
     * Format for end-of-benchmark.
     * @param result benchmark results
     */
    void endRun(Collection<RunResult> result);

    /* ------------- RAW OUTPUT METHODS ------------------- */

    void print(String s);

    void println(String s);

    void flush();

    void close();

    void verbosePrintln(String s);

    void write(int b);

    void write(byte[] b) throws IOException;

}
