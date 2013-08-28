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
package org.openjdk.jmh;

import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.internal.RunResult;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.Map;

public class SimpleTest {

    /*
     * This sample uses VERY EXPERIMENTAL API; use with care!
     */

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(".*")
                .warmupTime(TimeValue.milliseconds(100))
                .measurementTime(TimeValue.milliseconds(100))
                .jvmArgs("-server")
                .outputFormat(OutputFormatType.TextReport)
                .build();

        Map<BenchmarkRecord, RunResult> results = new Runner(opts).run();
        RunResult runResult = extractSingleResult(results);
        Result result = extractSingleMetric(runResult);

        System.out.println("API replied benchmark score: " + result.getScore() + " " + result.getScoreUnit());
    }

    private static Result extractSingleMetric(RunResult result) {
        Map<String,Result> results = result.getResults();
        if (results.size() != 1) {
            throw new IllegalArgumentException("More than one metric: " + results);
        }
        return results.values().iterator().next();
    }

    public static RunResult extractSingleResult(Map<BenchmarkRecord, RunResult> results) {
        if (results.size() != 1) {
            throw new IllegalArgumentException("More than one result: " + results);
        }

        return results.values().iterator().next();
    }
}
