/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.validation;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

public class IterationScoresFormatter implements OutputFormat {

    private final PrintWriter pw;

    public IterationScoresFormatter(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void iteration(BenchmarkParams benchParams, IterationParams params, int iteration) {

    }

    @Override
    public void iterationResult(BenchmarkParams benchParams, IterationParams params, int iteration, IterationResult data) {
        Result r = data.getPrimaryResult();
        pw.println(String.format("   %.2f ± %.2f %s", r.getScore(), r.getScoreError(), r.getScoreUnit()));
    }

    @Override
    public void startBenchmark(BenchmarkParams benchParams) {

    }

    @Override
    public void endBenchmark(BenchmarkResult result) {

    }

    @Override
    public void startRun() {

    }

    @Override
    public void endRun(Collection<RunResult> result) {

    }

    @Override
    public void print(String s) {

    }

    @Override
    public void println(String s) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public void verbosePrintln(String s) {

    }

    @Override
    public void write(int b) {

    }

    @Override
    public void write(byte[] b) throws IOException {

    }
}
