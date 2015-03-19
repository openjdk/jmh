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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.util.Collection;

/**
 * External profiler: profilers to be run outside of JVM.
 *
 * <p>External profilers usually call external tools to get the performance data.
 * It is futile to query any internal JVM facilities in external profiler
 * Java code, because it may not be executed in the benchmarked VM at all.</p>
 */
public interface ExternalProfiler extends Profiler {

    /**
     * Prepend JVM invocation with these commands.
     *
     * @param params benchmark parameters used for current launch
     * @return commands to prepend for JVM launch
     */
    Collection<String> addJVMInvokeOptions(BenchmarkParams params);

    /**
     * Add JVM these options to the run.
     *
     * @param params benchmark parameters used for current launch
     * @return options to add to JVM launch
     */
    Collection<String> addJVMOptions(BenchmarkParams params);

    /**
     * Run this code before starting the trial. This method will execute
     * before starting the benchmark JVM.
     *
     * @param benchmarkParams benchmark parameters used for current launch
     */
    void beforeTrial(BenchmarkParams benchmarkParams);

    /**
     * Run this code after the trial is done. This method will execute
     * after benchmark JVM had stopped.
     *
     * @param br benchmark result that was the result of the trial
     * @param stdOut file containing the standard output from the benchmark JVM
     * @param stdErr file containing the standard error from the benchmark JVM
     * @return profiler results
     */
    Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr);

    /**
     * If target VM communicates with profiler with standard output, this method
     * can be used to shun the output to console. Profiler is responsible for consuming
     * the standard output and printing the relevant data from there.
     *
     * @return returns true, if profiler allows harness to print out the standard output
     */
    boolean allowPrintOut();

    /**
     * If target VM communicates with profiler with standard error, this method
     * can be used to shun the output to console. Profiler is responsible for consuming
     * the standard error and printing the relevant data from there.
     *
     * @return returns true, if profiler allows harness to print out the standard errpr
     */
    boolean allowPrintErr();
}
