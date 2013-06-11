/**
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

import org.openjdk.jmh.logic.results.internal.IterationResult;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.IOException;
import java.util.Collection;

/**
 * Internal interface for OutputFormat.
 * <p/>
 * TODO: This interface might need touchups for formats that require symmetric headers or recursion (XML etc).
 *
 * @author anders.astrand@oracle.com
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public interface OutputFormat {

    /**
     * Format for warmup output.
     *
     * @param benchmark benchmark name
     * @param iteration Warmup Iteration Number (1..N)
     * @param threads   Number of threads we executed on
     * @param warmupTime Time to spend per iteration
     */
    public void warmupIteration(BenchmarkRecord benchmark, int iteration, int threads, TimeValue warmupTime);

    /**
     * Format for end-of-warmup-iteration.
     *
     * @param benchmark benchmark name
     * @param iteration iteration-number
     * @param thread    amount of threads used
     * @param result    result of iteration
     */
    public void warmupIterationResult(BenchmarkRecord benchmark, int iteration, int thread, IterationResult result);

    /**
     * Format for iteration start.
     *
     * @param benchmark benchmark name
     * @param iteration iteration-number
     * @param threads threads to run
     * @param runTime time to spend per iteration
     */
    public void iteration(BenchmarkRecord benchmark, int iteration, int threads, TimeValue runTime);

    /**
     * Format for end-of-iteration.
     *
     * @param name      name of benchmark
     * @param iteration iteration-number
     * @param thread    amount of threads used
     * @param result    result of iteration
     * @param profiles  profiler results
     */
    public void iterationResult(BenchmarkRecord name, int iteration, int thread, IterationResult result, Collection<ProfilerResult> profiles);

    /**
     * Format for start-of-benchmark output.
     *
     * @param verbose Should we output verbose info?
     */
    public void startBenchmark(BenchmarkRecord name, MicroBenchmarkParameters mbParams, boolean verbose);

    /**
     * Format for end-of-benchmark.
     *
     * @param name       benchmark name
     * @param result statistics of the run
     */
    public void endBenchmark(BenchmarkRecord name, RunResult result);

    /**
     * Format for start-of-benchmark output.
     *
     * @param message message to dump
     */
    public void startRun(String message);

    /**
     * Format for end-of-benchmark.
     *
     * @param message message to dump
     */
    public void endRun(String message);

    /**
     * Format for detailed results output.
     *
     * @param name      benchmark name
     * @param iteration iteration number
     * @param threads   thread count
     * @param results   AggregatedResults with detailed run results
     */
    public void detailedResults(BenchmarkRecord name, int iteration, int threads, IterationResult results);

    /**
     * Format for sub-thread statistics.
     *
     * @param name      benchmark name
     * @param threads   thread count
     * @param result    result of iterations of the current threadcount
     */
    public void threadSubStatistics(BenchmarkRecord name, int threads, RunResult result);

    /* ------------- SPECIAL TRACING METHODS -------------------- */

    void exception(Throwable ex);

    /* ------------- RAW OUTPUT METHODS ------------------- */

    void println(String s);

    void flush();

    void close();

    void verbosePrintln(String s);

    void write(int b);

    void write(byte[] b) throws IOException;

}
