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
package org.openjdk.jmh.runner.options;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.TimeUnit;

public interface ChainedOptionsBuilder {

    /**
     * Produce the final Options
     * @return options object.
     */
    Options build();

    /**
     * Override the defaults from the given option.
     * You may use this only once.
     *
     * @param other options to base on
     * @return builder
     */
    ChainedOptionsBuilder parent(Options other);

    /**
     * Include benchmark in the run
     * (Can be used multiple times)
     *
     * @param regexp to match benchmarks against
     * @return builder
     */
    ChainedOptionsBuilder include(String regexp);

    /**
     * Exclude benchmarks from the run
     * (Can be used multiple times)
     *
     * @param regexp to match benchmark against
     * @return builder
     */
    ChainedOptionsBuilder exclude(String regexp);

    /**
     * ResultFormatType to use in the run
     * @param type resultformat type
     * @return builder
     */
    ChainedOptionsBuilder resultFormat(ResultFormatType type);

    /**
     * Output filename to write the run log to
     * @param filename file name
     * @return builder
     */
    ChainedOptionsBuilder output(String filename);

    /**
     * Output filename to write the result to
     * @param filename file name
     * @return builder
     */
    ChainedOptionsBuilder result(String filename);

    /**
     * Should do GC between measurementIterations?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder shouldDoGC(boolean value);

    /**
     * Add the profiler in the run
     * @param prof profiler type
     * @return builder
     */
    ChainedOptionsBuilder addProfiler(ProfilerType prof);

    /**
     * Control verbosity level.
     * @param mode flag
     * @return builder
     */
    ChainedOptionsBuilder verbosity(VerboseMode mode);

    /**
     * Should fail on first benchmark error?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder shouldFailOnError(boolean value);

    /**
     * Number of threads to run the benchmark in
     * @param count number of threads
     * @return builder
     * @see org.openjdk.jmh.annotations.Threads
     */
    ChainedOptionsBuilder threads(int count);

    /**
     * Subgroups thread distribution.
     * @param groups thread distribution
     * @return builder
     * @see org.openjdk.jmh.annotations.Group
     * @see org.openjdk.jmh.annotations.GroupThreads
     */
    ChainedOptionsBuilder threadGroups(int... groups);

    /**
     * Should synchronize measurementIterations?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder syncIterations(boolean value);

    /**
     * How many warmup iterations to do?
     * @param value flag
     * @return builder
     * @see org.openjdk.jmh.annotations.Warmup
     */
    ChainedOptionsBuilder warmupIterations(int value);

    /**
     * How large warmup batchSize should be?
     * @param value batch size
     * @return builder
     * @see org.openjdk.jmh.annotations.Warmup
     */
    ChainedOptionsBuilder warmupBatchSize(int value);

    /**
     * How long each warmup iteration should take?
     * @param value time
     * @return builder
     * @see org.openjdk.jmh.annotations.Warmup
     */
    ChainedOptionsBuilder warmupTime(TimeValue value);

    /**
     * Warmup mode to use
     * @param mode to use
     * @return builder
     */
    ChainedOptionsBuilder warmupMode(WarmupMode mode);

    /**
     * What other benchmarks to warmup along the way
     * @param regexp to match benchmarks against
     * @return builder
     */
    ChainedOptionsBuilder includeWarmup(String regexp);

    /**
     * How many measurement measurementIterations to do
     * @param count number of iterations
     * @return builder
     * @see org.openjdk.jmh.annotations.Measurement
     */
    ChainedOptionsBuilder measurementIterations(int count);

    /**
     * How large measurement batchSize should be?
     * @param value batch size
     * @return builder
     * @see org.openjdk.jmh.annotations.Measurement
     */
    ChainedOptionsBuilder measurementBatchSize(int value);

    /**
     * How long each measurement iteration should take?
     * @param value time
     * @return builder
     * @see org.openjdk.jmh.annotations.Measurement
     */
    ChainedOptionsBuilder measurementTime(TimeValue value);

    /**
     * Benchmark mode.
     * (Can be used multiple times)
     *
     * @param mode benchmark mode
     * @return builder
     * @see org.openjdk.jmh.annotations.BenchmarkMode
     */
    ChainedOptionsBuilder mode(Mode mode);

    /**
     * Timeunit to use in results
     * @param tu time unit
     * @return builder
     * @see org.openjdk.jmh.annotations.OutputTimeUnit
     */
    ChainedOptionsBuilder timeUnit(TimeUnit tu);

    /**
     * Operations per invocation.
     * @param value operations per invocation.
     * @return builder
     * @see org.openjdk.jmh.annotations.OperationsPerInvocation
     */
    ChainedOptionsBuilder operationsPerInvocation(long value);

    /**
     * Number of forks to use in the run
     * @param value number of forks
     * @return builder
     * @see org.openjdk.jmh.annotations.Fork
     */
    ChainedOptionsBuilder forks(int value);

    /**
     * Number of ignored forks
     * @param value number of ignored forks
     * @return builder
     * @see org.openjdk.jmh.annotations.Fork
     */
    ChainedOptionsBuilder warmupForks(int value);

    /**
     * Forked JVM to use.
     *
     * @param path path to /bin/java
     * @return builder
     */
    ChainedOptionsBuilder jvm(String path);

    /**
     * Forked JVM arguments.
     *
     * @param value arguments to add to the run
     * @return builder
     * @see org.openjdk.jmh.annotations.Fork
     */
    ChainedOptionsBuilder jvmArgs(String... value);

    /**
     * Append forked JVM arguments:
     * These options go after other options.
     *
     * @param value arguments to add to the run
     * @return builder
     * @see org.openjdk.jmh.annotations.Fork
     */
    ChainedOptionsBuilder jvmArgsAppend(String... value);

    /**
     * Prepend forked JVM arguments:
     * These options go before any other options.
     *
     * @param value arguments to add to the run
     * @return builder
     * @see org.openjdk.jmh.annotations.Fork
     */
    ChainedOptionsBuilder jvmArgsPrepend(String... value);

    /**
     * Autodetect forked JVM arguments from the parent VM.
     * Overrides the jvmArgs(...) value.
     *
     * @return builder
     */
    ChainedOptionsBuilder detectJvmArgs();

    /**
     * Set benchmark parameter.
     * @param name parameter
     * @param values list of values to set
     * @return builder
     * @see org.openjdk.jmh.annotations.Param
     */
    ChainedOptionsBuilder param(String name, String... values);

}
