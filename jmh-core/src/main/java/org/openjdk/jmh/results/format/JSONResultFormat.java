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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.util.Statistics;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

class JSONResultFormat implements ResultFormat {

    private final PrintStream out;

    public JSONResultFormat(PrintStream out) {
        this.out = out;
    }

    @Override
    public void writeOut(Collection<RunResult> results) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        boolean first = true;

        pw.println("[");
        for (RunResult runResult : results) {
            BenchmarkParams params = runResult.getParams();

            if (first) {
                first = false;
            } else {
                pw.println(",");
            }

            pw.println("{");
            pw.println("\"benchmark\" : \"" + params.getBenchmark() + "\",");
            pw.println("\"mode\" : \"" + params.getMode().shortLabel() + "\",");
            pw.println("\"threads\" : " + params.getThreads() + ",");
            pw.println("\"forks\" : " + params.getForks() + ",");
            pw.println("\"warmupIterations\" : " + params.getWarmup().getCount() + ",");
            pw.println("\"warmupTime\" : \"" + params.getWarmup().getTime() + "\",");
            pw.println("\"warmupBatchSize\" : " + params.getWarmup().getBatchSize() + ",");
            pw.println("\"measurementIterations\" : " + params.getMeasurement().getCount() + ",");
            pw.println("\"measurementTime\" : \"" + params.getMeasurement().getTime() + "\",");
            pw.println("\"measurementBatchSize\" : " + params.getMeasurement().getBatchSize() + ",");

            if (!params.getParamsKeys().isEmpty()) {
                pw.println("\"params\" : {");
                pw.println(emitParams(params));
                pw.println("},");
            }

            Result primaryResult = runResult.getPrimaryResult();
            pw.println("\"primaryMetric\" : {");
            pw.println("\"score\" : " + emit(primaryResult.getScore()) + ",");
            pw.println("\"scoreError\" : " + emit(primaryResult.getScoreError()) + ",");
            pw.println("\"scoreConfidence\" : " + emit(primaryResult.getScoreConfidence()) + ",");
            pw.println(emitPercentiles(primaryResult.getStatistics()));
            pw.println("\"scoreUnit\" : \"" + primaryResult.getScoreUnit() + "\",");
            pw.println("\"rawData\" :");

            {
                Collection<String> l1 = new ArrayList<String>();
                for (BenchmarkResult benchmarkResult : runResult.getBenchmarkResults()) {
                    Collection<String> scores = new ArrayList<String>();
                    for (IterationResult r : benchmarkResult.getIterationResults()) {
                        scores.add(emit(r.getPrimaryResult().getScore()));
                    }
                    l1.add(printMultiple(scores, "[", "]"));
                }
                pw.println(printMultiple(l1, "[", "]"));
                pw.println("},");
            }

            Collection<String> secondaries = new ArrayList<String>();
            for (Map.Entry<String, Result> e : runResult.getSecondaryResults().entrySet()) {
                String secondaryName = e.getKey();
                Result result = e.getValue();

                StringBuilder sb = new StringBuilder();
                sb.append("\"").append(secondaryName).append("\" : {");
                sb.append("\"score\" : ").append(emit(result.getScore())).append(",");
                sb.append("\"scoreError\" : ").append(emit(result.getScoreError())).append(",");
                sb.append("\"scoreConfidence\" : ").append(emit(result.getScoreConfidence())).append(",");
                sb.append(emitPercentiles(result.getStatistics()));
                sb.append("\"scoreUnit\" : \"").append(result.getScoreUnit()).append("\",");
                sb.append("\"rawData\" : ");

                Collection<String> l2 = new ArrayList<String>();
                for (BenchmarkResult benchmarkResult : runResult.getBenchmarkResults()) {
                    Collection<String> scores = new ArrayList<String>();
                    for (IterationResult r : benchmarkResult.getIterationResults()) {
                        Result rr = r.getSecondaryResults().get(secondaryName);
                        if (rr != null) {
                            scores.add(emit(rr.getScore()));
                        }
                    }
                    l2.add(printMultiple(scores, "[", "]"));
                }

                sb.append(printMultiple(l2, "[", "]"));
                sb.append("}");
                secondaries.add(sb.toString());
            }
            pw.println("\"secondaryMetrics\" : {");
            pw.println(printMultiple(secondaries, "", ""));
            pw.println("}");

            pw.println("}");

        }
        pw.println("]");

        out.println(tidy(sw.toString()));
    }

    private String emitParams(BenchmarkParams params) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String k : params.getParamsKeys()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append("\"").append(k).append("\" : ");
            sb.append("\"").append(params.getParam(k)).append("\"");
        }
        return sb.toString();
    }

    private String emitPercentiles(Statistics stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"scorePercentiles\" : {");
        boolean firstPercentile = true;
        for (double p : new double[]{0.00, 50.0, 90, 95, 99, 99.9, 99.99, 99.999, 99.9999, 100}) {
            if (firstPercentile) {
                firstPercentile = false;
            } else {
                sb.append(",");
            }

            double v = stats.getPercentile(p);
            sb.append("\"").append(emit(p)).append("\" : ");
            sb.append(emit(v));
        }
        sb.append("},");
        return sb.toString();
    }

    private String emit(double[] ds) {
        StringBuilder sb = new StringBuilder();

        boolean isFirst = true;
        sb.append("[");
        for (double d : ds) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(emit(d));
        }
        sb.append("]");
        return sb.toString();
    }

    private String emit(double d) {
        if (d != d)
            return "\"NaN\"";
        if (d == Double.NEGATIVE_INFINITY)
            return "\"-INF\"";
        if (d == Double.POSITIVE_INFINITY)
            return "\"+INF\"";
        return String.valueOf(d);
    }

    private String tidy(String s) {
        s = s.replaceAll("\r", "");
        s = s.replaceAll("\n", " ");
        s = s.replaceAll(",", ",\n");
        s = s.replaceAll("\\{", "{\n");
        s = s.replaceAll("\\[", "[\n");
        s = s.replaceAll("\\}", "\n}\n");
        s = s.replaceAll("\\]", "\n]\n");
        s = s.replaceAll("\\]\n,\n", "],\n");
        s = s.replaceAll("\\}\n,\n", "},\n");
        s = s.replaceAll("\n( *)\n", "\n");

        String[] lines = s.split("\n");

        StringBuilder sb = new StringBuilder();

        int ident = 0;
        String prevL = null;
        for (String l : lines) {
            if (prevL != null && (prevL.endsWith("{") || prevL.endsWith("["))) {
                ident++;
            }
            if (l.endsWith("}") || l.endsWith("]") || l.endsWith("},") || l.endsWith("],")) {
                ident--;
            }

            for (int c = 0; c < ident; c++) {
                sb.append("    ");
            }
            sb.append(l.trim());
            sb.append("\n");
            prevL = l;
        }

        return sb.toString();
    }

    private String printMultiple(Collection<String> elements, String leftBracket, String rightBracket) {
        StringBuilder sb = new StringBuilder();
        sb.append(leftBracket);
        boolean isFirst = true;
        for (String e : elements) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(e);
        }
        sb.append(rightBracket);
        return sb.toString();
    }

}
