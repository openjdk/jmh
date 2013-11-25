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
import org.openjdk.jmh.runner.BenchmarkRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class JSONResultFormat implements ResultFormat {

    private final String output;

    public JSONResultFormat(String output) {
        this.output = output;
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
                pw.println("\"iterations\" : " + runResult.getParams().getIteration().getCount() + ",");
                pw.println("\"iterationTime\" : \"" + runResult.getParams().getIteration().getTime() + "\",");
                pw.println("\"primaryMetric\" : {");
                pw.println("\"score\" : " + runResult.getPrimaryResult().getScore() + ",");
                pw.println("\"scoreError\" : " + runResult.getPrimaryResult().getStatistics().getMeanErrorAt(0.999) + ",");
                pw.println("\"scoreStdev\" : " + runResult.getPrimaryResult().getStatistics().getStandardDeviation() + ",");
                pw.println("\"scoreConfidence95\" : " + Arrays.toString(runResult.getPrimaryResult().getStatistics().getConfidenceIntervalAt(0.95)) + ",");
                pw.println("\"scoreConfidence99\" : " + Arrays.toString(runResult.getPrimaryResult().getStatistics().getConfidenceIntervalAt(0.99)) + ",");
                pw.println("\"scoreConfidence999\" : " + Arrays.toString(runResult.getPrimaryResult().getStatistics().getConfidenceIntervalAt(0.999)) + ",");
                pw.println("\"scoreUnit\" : \"" + runResult.getPrimaryResult().getScoreUnit() + "\",");
                pw.println("\"rawData\" :");

                {
                    Collection<String> l1 = new ArrayList<String>();
                    for (BenchResult benchResult : runResult.getRawBenchResults()) {
                        Collection<String> scores = new ArrayList<String>();
                        for (Result r : benchResult.getRawPrimaryResults()) {
                            scores.add(String.valueOf(r.getScore()));
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
                    sb.append("\"score\" : ").append(result.getScore()).append(",");
                    sb.append("\"scoreError\" : ").append(runResult.getPrimaryResult().getStatistics().getMeanErrorAt(0.999)).append(",");
                    sb.append("\"scoreStdev\" : ").append(result.getStatistics().getStandardDeviation()).append(",");
                    sb.append("\"scoreConfidence95\" : ").append(Arrays.toString(result.getStatistics().getConfidenceIntervalAt(0.95))).append(",");
                    sb.append("\"scoreConfidence99\" : ").append(Arrays.toString(result.getStatistics().getConfidenceIntervalAt(0.99))).append(",");
                    sb.append("\"scoreConfidence999\" : ").append(Arrays.toString(result.getStatistics().getConfidenceIntervalAt(0.999))).append(",");
                    sb.append("\"scoreUnit\" : \"").append(result.getScoreUnit()).append("\",");
                    sb.append("\"rawData\" :");

                    Collection<String> l2 = new ArrayList<String>();
                    for (BenchResult benchResult : runResult.getRawBenchResults()) {
                        Collection<String> scores = new ArrayList<String>();
                        for (Result r : benchResult.getRawSecondaryResults().get(secondaryName)) {
                            scores.add(String.valueOf(r.getScore()));
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

        try {
            PrintWriter out = new PrintWriter(output);
            out.println(tidy(sw.toString()));
            out.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
