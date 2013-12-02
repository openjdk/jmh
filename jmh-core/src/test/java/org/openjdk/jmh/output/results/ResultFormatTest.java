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
package org.openjdk.jmh.output.results;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.ResultRole;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.logic.results.ThroughputResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * These tests seal the machine-readable format.
 * Any change to these tests should be discussed with the maintainers first!
 */
public class ResultFormatTest {

    private Map<BenchmarkRecord, RunResult> getStub() {
        Map<BenchmarkRecord, RunResult> results = new TreeMap<BenchmarkRecord, RunResult>();

        Random r = new Random(12345);
        for (int b = 0; b < r.nextInt(10); b++) {
            BenchmarkRecord record = new BenchmarkRecord("benchmark_" + b, JSONResultFormat.class.getName() + ".benchmark_" + b, Mode.AverageTime);
            BenchmarkParams params = new BenchmarkParams(false,
                    r.nextInt(1000),
                    new int[]{ r.nextInt(1000) },
                    r.nextInt(1000),
                    r.nextInt(1000),
                    r.nextInt(1000),
                    TimeValue.seconds(r.nextInt(1000)),
                    r.nextInt(1000),
                    TimeValue.seconds(r.nextInt(1000))
            );

            Collection<BenchResult> benchResults = new ArrayList<BenchResult>();
            for (int f = 0; f < r.nextInt(10); f++) {
                Collection<IterationResult> iterResults = new ArrayList<IterationResult>();
                for (int c = 0; c < r.nextInt(10); c++) {
                    IterationResult res = new IterationResult(record, params.getMeasurement());
                    res.addResult(new ThroughputResult(ResultRole.PRIMARY, "test", r.nextInt(1000), 1000 * 1000));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary1", r.nextInt(1000), 1000 * 1000));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary2", r.nextInt(1000), 1000 * 1000));
                    iterResults.add(res);
                }
                benchResults.add(new BenchResult(iterResults));
            }
            results.put(record, new RunResult(benchResults));
        }
        return results;
    }

    private void compare(String actualFile, String goldenFile) throws IOException {
        BufferedReader actualReader = new BufferedReader(new FileReader(actualFile));
        BufferedReader goldenReader = new BufferedReader(new InputStreamReader(ResultFormatTest.class.getResourceAsStream("/org/openjdk/jmh/output/results/" + goldenFile)));
        while (true) {
            String goldenLine = goldenReader.readLine();
            String actualLine = actualReader.readLine();
            Assert.assertEquals("Mismatch", goldenLine, actualLine);
            if (goldenLine == null && actualLine == null) break;
        }
    }

    @Test
    public void jsonTest() throws IOException {
        String actualFile = File.createTempFile("jmh", "test").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.JSON,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.json");
    }

    @Test
    public void csvTest() throws IOException {
        String actualFile = File.createTempFile("jmh", "test").getAbsolutePath();

        ResultFormatFactory.getInstance(
                    ResultFormatType.CSV,
                    actualFile)
                .writeOut(getStub());

        compare(actualFile, "output-golden.csv");
    }

}
