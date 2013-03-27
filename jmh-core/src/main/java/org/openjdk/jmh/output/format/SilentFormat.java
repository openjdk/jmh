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
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.PrintStream;
import java.util.Collection;

/**
 * Silent format, does nothing.
 *
 * @author anders.astrand@oracle.com
 */
public class SilentFormat extends AbstractOutputFormat {

    public SilentFormat(PrintStream out, boolean verbose) {
        super(out, verbose);
    }

    @Override
    public void startRun(String message) {
    }

    @Override
    public void endRun(String message) {
    }

    @Override
    public void warmupIteration(String benchmark, int iteration, int threads, TimeValue warmupTime) {

    }

    @Override
    public void startBenchmark(String name, MicroBenchmarkParameters mbParams, boolean verbose) {

    }

    @Override
    public void endBenchmark(String name, RunResult result) {

    }

    @Override
    public void detailedResults(String name, int iteration, int threads, IterationResult results) {

    }

    @Override
    public void iteration(String benchmark, int iteration, int threads, TimeValue runTime) {

    }

    @Override
    public void traceStartIteration() {
    }

    @Override
    public void traceEndIteration() {
    }

    @Override
    public void iterationResult(String name, int iteration, int thread, IterationResult result, Collection<ProfilerResult> profiles) {

    }

    @Override
    public void warmupIterationResult(String benchmark, int iteration, int thread, IterationResult result) {
    }

    @Override
    public void threadSubStatistics(String name, int threads, RunResult results) {

    }

    @Override
    public void traceHarnessMethodInvoked(String methodName) {
    }

    @Override
    public void traceHarnessMethodReturned(String methodName) {
    }

}
