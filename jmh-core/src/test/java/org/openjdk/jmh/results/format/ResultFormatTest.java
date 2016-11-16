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
package org.openjdk.jmh.results.format;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * These tests seal the machine-readable format.
 * Any change to these tests should be discussed with the maintainers first!
 */
public class ResultFormatTest {

    private Collection<RunResult> getStub() {
        Collection<RunResult> results = new TreeSet<>(RunResult.DEFAULT_SORT_COMPARATOR);

        Random r = new Random(12345);
        Random ar = new Random(12345);
        for (int b = 0; b < r.nextInt(10); b++) {
            WorkloadParams ps = new WorkloadParams();
            for (int p = 0; p < 5; p++) {
                ps.put("param" + p, "value" + p, p);
            }
            BenchmarkParams params = new BenchmarkParams(
                    "benchmark_" + b,
                    JSONResultFormat.class.getName() + ".benchmark_" + b + "_" + Mode.Throughput,
                    false,
                    r.nextInt(1000),
                    new int[]{ r.nextInt(1000) },
                    Collections.<String>emptyList(),
                    r.nextInt(1000),
                    r.nextInt(1000),
                    new IterationParams(IterationType.WARMUP,      r.nextInt(1000), TimeValue.seconds(r.nextInt(1000)), 1),
                    new IterationParams(IterationType.MEASUREMENT, r.nextInt(1000), TimeValue.seconds(r.nextInt(1000)), 1),
                    Mode.Throughput,
                    ps,
                    TimeUnit.SECONDS, 1,
                    Utils.getCurrentJvm(),
                    Collections.<String>emptyList(),
                    TimeValue.days(1));

            Collection<BenchmarkResult> benchmarkResults = new ArrayList<>();
            for (int f = 0; f < r.nextInt(10); f++) {
                Collection<IterationResult> iterResults = new ArrayList<>();
                for (int c = 0; c < r.nextInt(10); c++) {
                    IterationResult res = new IterationResult(params, params.getMeasurement(), null);
                    res.addResult(new ThroughputResult(ResultRole.PRIMARY, "test", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary1", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary2", r.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    if (ar.nextBoolean()) {
                        res.addResult(new ThroughputResult(ResultRole.SECONDARY, "secondary3", ar.nextInt(1000), 1000 * 1000, TimeUnit.MILLISECONDS));
                    }
                    iterResults.add(res);
                }
                benchmarkResults.add(new BenchmarkResult(params, iterResults));
            }
            results.add(new RunResult(params, benchmarkResults));
        }
        return results;
    }

    private void compare(String actualFile, String goldenFile) throws IOException {
        BufferedReader actualReader = new BufferedReader(new FileReader(actualFile));
        BufferedReader goldenReader = new BufferedReader(new InputStreamReader(ResultFormatTest.class.getResourceAsStream("/org/openjdk/jmh/results/format/" + goldenFile)));

        String actualLines = Utils.join(FileUtils.readAllLines(actualReader), "\n");
        String goldenLines = Utils.join(FileUtils.readAllLines(goldenReader), "\n");
        Assert.assertEquals("Mismatch", goldenLines, actualLines);
    }

    public void test(ResultFormatType type, Locale locale, String suffix) throws IOException {
        Locale prevLocale = Locale.getDefault();
        Locale.setDefault(locale);

        String actualFileName = "test." + type.toString().toLowerCase() + suffix;
        String goldenFileName = "output-golden." + type.toString().toLowerCase() + suffix;

        try {
            String actualFile = FileUtils.tempFile(actualFileName).getAbsolutePath();
            ResultFormatFactory.getInstance(type, actualFile).writeOut(getStub());
            compare(actualFile, goldenFileName);

            PrintStream ps = new PrintStream(actualFile, "UTF-8");
            ResultFormatFactory.getInstance(type, ps).writeOut(getStub());
            ps.close();

            compare(actualFile, goldenFileName);
        } finally {
            Locale.setDefault(prevLocale);
        }
    }

    /*
     * JSON has a strict format for numbers, the results should be Locale-agnostic.
     */

    @Test
    public void jsonTest_ROOT() throws IOException {
        test(ResultFormatType.JSON, Locale.ROOT, "");
    }

    @Test
    public void jsonTest_US() throws IOException {
        test(ResultFormatType.JSON, Locale.US, "");
    }

    @Test
    public void jsonTest_RU() throws IOException {
        test(ResultFormatType.JSON, new Locale("RU"), "");
    }

    /*
     * CSV and SCSV data should conform to the Locale.
     */

    @Test
    public void csvTest_ROOT() throws IOException {
        test(ResultFormatType.CSV, Locale.ROOT, ".root");
    }

    @Test
    public void csvTest_US() throws IOException {
        test(ResultFormatType.CSV, Locale.US, ".us");
    }

    @Test
    public void csvTest_RU() throws IOException {
        test(ResultFormatType.CSV, new Locale("RU"), ".ru");
    }

    @Test
    public void scsvTest_ROOT() throws IOException {
        test(ResultFormatType.SCSV, Locale.ROOT, ".root");
    }

    @Test
    public void scsvTest_US() throws IOException {
        test(ResultFormatType.SCSV, Locale.US, ".us");
    }

    @Test
    public void scsvTest_RU() throws IOException {
        test(ResultFormatType.SCSV, new Locale("RU"), ".ru");
    }

    /*
     * LaTeX output should conform to the Locale.
     */

    @Test
    public void latexTest_ROOT() throws IOException {
        test(ResultFormatType.LATEX, Locale.ROOT, ".root");
    }

    @Test
    public void latexTest_US() throws IOException {
        test(ResultFormatType.LATEX, Locale.US, ".us");
    }

    @Test
    public void latexTest_RU() throws IOException {
        test(ResultFormatType.LATEX, new Locale("RU"), ".ru");
    }

    /*
     * Text output should conform to the Locale.
     */

    @Test
    public void textTest_ROOT() throws IOException {
        test(ResultFormatType.TEXT, Locale.ROOT, ".root");
    }

    @Test
    public void textTest_US() throws IOException {
        test(ResultFormatType.TEXT, Locale.US, ".us");
    }

    @Test
    public void textTest_RU() throws IOException {
        test(ResultFormatType.TEXT, new Locale("RU"), ".ru");
    }

}
