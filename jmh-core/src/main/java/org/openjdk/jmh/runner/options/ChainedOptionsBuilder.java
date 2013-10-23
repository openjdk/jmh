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
import org.openjdk.jmh.output.OutputFormatType;
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
     * OutputFormat to use in the run
     * @param type outputformat type
     * @return builder
     */
    ChainedOptionsBuilder outputFormat(OutputFormatType type);

    /**
     * Output filename to write the run log to
     * @param filename file name
     * @return builder
     */
    ChainedOptionsBuilder output(String filename);

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
     * Be extra verbose?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder verbose(boolean value);

    /**
     * Should fail on first benchmark error?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder failOnError(boolean value);

    /**
     * Number of threads to run the benchmark in
     * @param count number of threads
     * @return builder
     */
    ChainedOptionsBuilder threads(int count);

    /**
     * Should synchronize measurementIterations?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder syncIterations(boolean value);

    /**
     * How many warmup measurementIterations to do?
     * @param value flag
     * @return builder
     */
    ChainedOptionsBuilder warmupIterations(int value);

    /**
     * How long each warmup iteration should take?
     * @param value time
     * @return builder
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
     */
    ChainedOptionsBuilder measurementIterations(int count);

    /**
     * How long each measurement iteration should take?
     * @param value time
     * @return builder
     */
    ChainedOptionsBuilder measurementTime(TimeValue value);

    /**
     * Benchmark mode.
     * (Can be used multiple times)
     *
     * @param mode benchmark mode
     * @return builder
     */
    ChainedOptionsBuilder mode(Mode mode);

    /**
     * Timeunit to use in results
     * @param tu time unit
     * @return builder
     */
    ChainedOptionsBuilder timeUnit(TimeUnit tu);

    /**
     * Number of forks to use in the run
     * @param value number of forks
     * @return builder
     */
    ChainedOptionsBuilder forks(int value);

    /**
     * Number of ignored forks
     * @param value number of ignored forks
     * @return builder
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
     * Forked JVM classpath.
     *
     * @param value classpath to override with
     * @return builder
     */
    ChainedOptionsBuilder jvmClasspath(String value);

    /**
     * Forked JVM arguments.
     *
     * @param value arguments to override with
     * @return builder
     */
    ChainedOptionsBuilder jvmArgs(String value);

}
