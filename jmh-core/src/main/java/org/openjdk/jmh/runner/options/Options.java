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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Options {

    /**
     * Which benchmarks to execute?
     * @return list of regexps matching the requested benchmarks
     */
    List<String> getRegexps();

    /**
     * Which benchmarks to omit?
     * @return list of regexps matching the ignored benchmarks
     */
    List<String> getExcludes();

    /**
     * Output format to use
     * TODO: Rework for null => default
     * @return format type
     */
    OutputFormatType getOutputFormat();

    /**
     * Which file to use for dumping the output
     * @return file name; null if not defined
     */
    String getOutput();

    /**
     * Should force GC between iterations?
     * @return should GC?
     */
    boolean shouldDoGC();

    /**
     * Profilers to use for the run.
     * @return profilers to use; empty set if no profilers are required
     */
    Collection<ProfilerType> getProfilers();

    /**
     * Should be extra verbose?
     * @return should be verbose?
     */
    boolean isVerbose();

    /**
     * Should harness terminate on first error encountered?
     * @return should terminate?
     */
    boolean shouldFailOnError();

    /**
     * Should harness output extra details for the run?
     * @return should it?
     */
    boolean shouldOutputDetailedResults();

    /**
     * Number of threads to run
     * @return number of threads; 0 to use maximum number of threads; -1 to use default
     */
    int getThreads();

    /**
     * Should synchronize iterations?
     * TODO: Rework "null" interface?
     * @return should we? "null" if not defined
     */
    Boolean getSynchIterations();

    /**
     * Number of warmup iterations
     * @return number of warmup iterations; -1 to use default
     */
    int getWarmupIterations();

    /**
     * The duration for warmup iterations
     * @return duration; null, if use default
     */
    TimeValue getWarmupTime();

    /**
     * Warmup mode.
     * @return warmup mode
     */
    WarmupMode getWarmupMode();

    /**
     * Which benchmarks to warmup before doing the run.
     * @return list of regexps matching the relevant benchmarks; null if no benchmarks are defined
     */
    List<String> getWarmupMicros();

    /**
     * Number of measurement iterations
     * @return number of measurement iterations; -1 to use default
     */
    int getIterations();

    /**
     * The duration for measurement iterations
     * @return duration; null, if use default
     */
    TimeValue getRuntime();

    /**
     * Benchmarks modes to execute.
     * @return modes to execute the benchmarks in; null to use the default mode
     */
    Collection<Mode> getBenchModes();

    /**
     * Timeunit to use in units.
     * @return timeunit; null to use the default timeunit
     */
    TimeUnit getTimeUnit();

    /**
     * Fork count
     * @return fork count; 0, to prohibit forking
     */
    int getForkCount();

    /**
     * Number of initial forks to ignore the results for
     * @return initial fork count; 0, to disable
     */
    int getWarmupForkCount();

    /**
     * Additional JVM classpath
     * @return additional JVM classpath to add to forked VM
     */
    String getJvmClassPath();

    /**
     * JVM to use for forks
     * @return JVM binary location
     */
    String getJvm();

    /**
     * JVM parameters to use with forks
     * @return JVM parameters
     */
    String getJvmArgs();

    /**
     * Convert options to command line.
     * TODO: Rework and deprecate this
     * @return the array of command line elements.
     */
    String[] toCommandLine();

    /**
     * Forked VM specific: get the address of control VM
     * @return address of the host VM
     */
    String getHostName();

    /**
     * Forked VM specific: get the port for control VM
     * @return control VM port
     */
    int getHostPort();

    /**
     * Forked VM specific: get the benchmark info to invoke
     * @return which benchmark to execute
     */
    String getBenchmark();
}
