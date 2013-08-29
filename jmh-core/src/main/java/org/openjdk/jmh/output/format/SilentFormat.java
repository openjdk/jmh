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

import org.openjdk.jmh.logic.results.internal.IterationResult;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;

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
    public void startRun() {
    }

    @Override
    public void endRun() {
    }

    @Override
    public void startBenchmark(BenchmarkRecord name, BenchmarkParams mbParams, boolean verbose) {

    }

    @Override
    public void endBenchmark(BenchmarkRecord name, RunResult result) {

    }

    @Override
    public void detailedResults(BenchmarkRecord name, IterationParams params, int iteration, IterationResult results) {

    }

    @Override
    public void iteration(BenchmarkRecord benchmark, IterationParams params, int iteration, IterationType type) {

    }

    @Override
    public void iterationResult(BenchmarkRecord name, IterationParams params, int iteration, IterationType type, IterationResult result, Collection<ProfilerResult> profiles) {

    }

}
