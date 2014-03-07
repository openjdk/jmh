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

import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.runner.ActualParams;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.util.internal.Statistics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

class JSONResultFormat implements ResultFormat {

    private final PrintWriter out;

    public JSONResultFormat(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void writeOut(Map<BenchmarkRecord, RunResult> results) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        boolean first = true;

        pw.println("[");
        for (BenchmarkRecord br : results.keySet()) {
            if (first) {
                first = false;
            } else {
                pw.println(",");
            }

            RunResult runResult = results.get(br);

            pw.println("{");
            pw.println("\"benchmark\" : \"" + br.getUsername() + "\",");
            pw.println("\"mode\" : \"" + br.getMode().shortLabel() + "\",");
            pw.println("\"threads\" : " + runResult.getParams().getThreads() + ",");
            pw.println("\"forks\" : " + runResult.getParams().getForks() + ",");
            pw.println("\"warmupIterations\" : " + runResult.getParams().getWarmup().getCount() + ",");
            pw.println("\"warmupTime\" : \"" + runResult.getParams().getWarmup().getTime() + "\",");
            pw.println("\"measurementIterations\" : " + runResult.getParams().getMeasurement().getCount() + ",");
            pw.println("\"measurementTime\" : \"" + runResult.getParams().getMeasurement().getTime() + "\",");

            if (!br.getActualParams().isEmpty()) {
                pw.println("\"params\" : {");
                pw.println(emitParams(br.getActualParams()));
                pw.println("},");
            }

            pw.println("\"primaryMetric\" : {");
            pw.println("\"score\" : " + emit(runResult.getPrimaryResult().getScore()) + ",");
            pw.println("\"scoreError\" : " + emit(runResult.getPrimaryResult().getStatistics().getMeanErrorAt(0.999)) + ",");
            pw.println("\"scoreConfidence\" : " + emit(runResult.getPrimaryResult().getStatistics().getConfidenceIntervalAt(0.999)) + ",");
            pw.println(emitPercentiles(runResult.getPrimaryResult().getStatistics()));
            pw.println("\"scoreUnit\" : \"" + runResult.getPrimaryResult().getScoreUnit() + "\",");
            pw.println("\"rawData\" :");

            {
                Collection<String> l1 = new ArrayList<String>();
                for (BenchResult benchResult : runResult.getRawBenchResults()) {
                    Collection<String> scores = new ArrayList<String>();
                    for (Result r : benchResult.getRawPrimaryResults()) {
                        scores.add(emit(r.getScore()));
                    }
                    l1.add(printMultiple(scores, "[", "]"));
                }
                pw.println(printMultiple(l1, "[", "]"));
                pw.println("},");
            }

            Collection<String> secondaries = new ArrayList<String>();
            for (String secondaryName : runResult.getSecondaryResults().keySet()) {
                Result result = runResult.getSecondaryResults().get(secondaryName);

                StringBuilder sb = new StringBuilder();
                sb.append("\"").append(secondaryName).append("\" : {");
                sb.append("\"score\" : ").append(emit(result.getScore())).append(",");
                sb.append("\"scoreError\" : ").append(emit(result.getStatistics().getMeanErrorAt(0.999))).append(",");
                sb.append("\"scoreConfidence\" : ").append(emit(result.getStatistics().getConfidenceIntervalAt(0.999))).append(",");
                sb.append(emitPercentiles(result.getStatistics()));
                sb.append("\"scoreUnit\" : \"").append(result.getScoreUnit()).append("\",");
                sb.append("\"rawData\" :");

                Collection<String> l2 = new ArrayList<String>();
                for (BenchResult benchResult : runResult.getRawBenchResults()) {
                    Collection<String> scores = new ArrayList<String>();
                    for (Result r : benchResult.getRawSecondaryResults().get(secondaryName)) {
                        scores.add(emit(r.getScore()));
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

    private String emitParams(ActualParams params) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String k : params.keys()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append("\"").append(k).append("\" : ");
            sb.append("\"").append(params.get(k)).append("\"");
        }
        return sb.toString();
    }

    private String emitPercentiles(Statistics stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"scorePercentiles\" : {");
        boolean firstPercentile = true;
        for (double p : new double[]{0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 0.99999, 0.999999, 1.0}) {
            if (firstPercentile) {
                firstPercentile = false;
            } else {
                sb.append(",");
            }

            double v = stats.getPercentile(p * 100);
            sb.append(String.format("\"%.4f\" : %.3f", p * 100, v));
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
        return String.format("%.3f", d);
    }

    private String tidy(String s) {
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
