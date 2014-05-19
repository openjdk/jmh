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
import org.openjdk.jmh.util.internal.Optional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Options extends Serializable {

    /**
     * Which benchmarks to execute?
     * @return list of regexps matching the requested benchmarks
     */
    List<String> getIncludes();

    /**
     * Which benchmarks to omit?
     * @return list of regexps matching the ignored benchmarks
     */
    List<String> getExcludes();

    /**
     * Which file to use for dumping the output
     * @return file name
     */
    Optional<String> getOutput();

    /**
     * Result format to use
     * @return format type
     */
    Optional<ResultFormatType> getResultFormat();

    /**
     * Which file to use for dumping the result
     * @return file name
     */
    Optional<String> getResult();

    /**
     * Should force GC between iterations?
     * @return should GC?
     */
    Optional<Boolean> shouldDoGC();

    /**
     * Profilers to use for the run.
     * @return profilers to use; empty collection if no profilers are required
     */
    Collection<ProfilerType> getProfilers();

    /**
     * How verbose should we be?
     * @return verbosity mode
     */
    Optional<VerboseMode> verbosity();

    /**
     * Should harness terminate on first error encountered?
     * @return should terminate?
     */
    Optional<Boolean> shouldFailOnError();

    /**
     * Number of threads to run
     * @return number of threads; 0 to use maximum number of threads
     */
    Optional<Integer> getThreads();

    /**
     * Thread subgroups distribution.
     * @return array of thread ratios
     */
    Optional<int[]> getThreadGroups();

    /**
     * Should synchronize iterations?
     * @return should we?
     */
    Optional<Boolean> shouldSyncIterations();

    /**
     * Number of warmup iterations
     * @return number of warmup iterations
     */
    Optional<Integer> getWarmupIterations();

    /**
     * The duration for warmup iterations
     * @return duration
     */
    Optional<TimeValue> getWarmupTime();

    /**
     * Number of batch size for warmup
     * @return number of batch size for warmup
     */
    Optional<Integer> getWarmupBatchSize();

    /**
     * Warmup mode.
     * @return warmup mode
     */
    Optional<WarmupMode> getWarmupMode();

    /**
     * Which benchmarks to warmup before doing the run.
     * @return list of regexps matching the relevant benchmarks; empty if no benchmarks are defined
     */
    List<String> getWarmupIncludes();

    /**
     * Number of measurement iterations
     * @return number of measurement iterations
     */
    Optional<Integer> getMeasurementIterations();

    /**
     * The duration for measurement iterations
     * @return duration
     */
    Optional<TimeValue> getMeasurementTime();

    /**
     * Number of batch size for measurement
     * @return number of batch size for measurement
     */
    Optional<Integer> getMeasurementBatchSize();

    /**
     * Benchmarks modes to execute.
     * @return modes to execute the benchmarks in; empty to use the default modes
     */
    Collection<Mode> getBenchModes();

    /**
     * Timeunit to use in units.
     * @return timeunit
     */
    Optional<TimeUnit> getTimeUnit();

    /**
     * Operations per invocation.
     * @return operations per invocation.
     * @see org.openjdk.jmh.annotations.OperationsPerInvocation
     */
    Optional<Long> getOperationsPerInvocation();

    /**
     * Fork count
     * @return fork count; 0, to prohibit forking
     */
    Optional<Integer> getForkCount();

    /**
     * Number of initial forks to ignore the results for
     * @return initial fork count; 0, to disable
     */
    Optional<Integer> getWarmupForkCount();

    /**
     * JVM to use for forks
     * @return JVM binary location
     */
    Optional<String> getJvm();

    /**
     * JVM parameters to use with forks
     * @return JVM parameters
     */
    Optional<Collection<String>> getJvmArgs();

    /**
     * JVM parameters to use with forks (these options will be appended
     * after any other JVM option)
     * @return JVM parameters
     */
    Optional<Collection<String>> getJvmArgsAppend();

    /**
     * JVM parameters to use with forks (these options will be prepended
     * before any other JVM option)
     * @return JVM parameters
     */
    Optional<Collection<String>> getJvmArgsPrepend();

    /**
     * The overridden value of the parameter.
     * @param name parameter name
     * @return parameter
     */
    Optional<Collection<String>> getParameter(String name);

}
