/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.util.ClassUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

class TextResultFormat implements ResultFormat {
    private final PrintWriter out;

    public TextResultFormat(PrintWriter writer) {
        this.out = writer;
    }

    @Override
    public void writeOut(Collection<RunResult> runResults) {
        final int COLUMN_PAD = 2;

        Collection<String> benchNames = new ArrayList<String>();
        for (RunResult runResult : runResults) {
            benchNames.add(runResult.getParams().getBenchmark());
            for (String label : runResult.getSecondaryResults().keySet()) {
                benchNames.add(runResult.getParams().getBenchmark() + ":" + label);
            }
        }

        Map<String, String> benchPrefixes = ClassUtils.denseClassNames(benchNames);

        // determine name column length
        int nameLen = "Benchmark".length();
        for (String prefix : benchPrefixes.values()) {
            nameLen = Math.max(nameLen, prefix.length());
        }
        nameLen += COLUMN_PAD;

        // determine param lengths
        Map<String, Integer> paramLengths = new HashMap<String, Integer>();
        SortedSet<String> params = new TreeSet<String>();
        for (RunResult runResult : runResults) {
            BenchmarkParams bp = runResult.getParams();
            for (String k : bp.getParamsKeys()) {
                params.add(k);
                Integer len = paramLengths.get(k);
                if (len == null) {
                    len = ("(" + k + ")").length() + COLUMN_PAD;
                }
                paramLengths.put(k, Math.max(len, bp.getParam(k).length() + COLUMN_PAD));
            }
        }

        // determine column lengths for other columns
        int modeLen     = "Mode".length();
        int samplesLen  = "Samples".length();
        int scoreLen    = "Score".length();
        int scoreErrLen = "Error".length();
        int unitLen     = "Units".length();

        for (RunResult res : runResults) {
            Result primRes = res.getPrimaryResult();

            modeLen     = Math.max(modeLen,     res.getParams().getMode().shortLabel().length());
            samplesLen  = Math.max(samplesLen,  String.format("%d",   primRes.getSampleCount()).length());
            scoreLen    = Math.max(scoreLen,    String.format("%.3f", primRes.getScore()).length());
            scoreErrLen = Math.max(scoreErrLen, String.format("%.3f", primRes.getScoreError()).length());
            unitLen     = Math.max(unitLen,     primRes.getScoreUnit().length());

            for (Result subRes : res.getSecondaryResults().values()) {
                samplesLen  = Math.max(samplesLen,  String.format("%d",   subRes.getSampleCount()).length());
                scoreLen    = Math.max(scoreLen,    String.format("%.3f", subRes.getScore()).length());
                scoreErrLen = Math.max(scoreErrLen, String.format("%.3f", subRes.getScoreError()).length());
                unitLen     = Math.max(unitLen,     subRes.getScoreUnit().length());
            }
        }
        modeLen     += COLUMN_PAD;
        samplesLen  += COLUMN_PAD;
        scoreLen    += COLUMN_PAD;
        scoreErrLen += COLUMN_PAD - 1; // digest a single character for +- separator
        unitLen     += COLUMN_PAD;

        out.print(String.format("%-" + nameLen + "s", "Benchmark"));
        for (String k : params) {
            out.print(String.format("%" + paramLengths.get(k) + "s", "(" + k + ")"));
        }

        out.print(String.format("%" + modeLen + "s",     "Mode"));
        out.print(String.format("%" + samplesLen + "s",  "Samples"));
        out.print(String.format("%" + scoreLen + "s",    "Score"));
        out.print("  ");
        out.print(String.format("%" + scoreErrLen + "s", "Error"));
        out.print(String.format("%" + unitLen + "s",     "Units"));
        out.println();

        for (RunResult res : runResults) {
            {
                out.print(String.format("%-" + nameLen + "s",
                        benchPrefixes.get(res.getParams().getBenchmark())));

                for (String k : params) {
                    String v = res.getParams().getParam(k);
                    out.print(String.format("%" + paramLengths.get(k) + "s", (v == null) ? "N/A" : v));
                }

                Result pRes = res.getPrimaryResult();
                out.print(String.format("%" + modeLen + "s",        res.getParams().getMode().shortLabel()));
                out.print(String.format("%" + samplesLen + "d",     pRes.getSampleCount()));
                out.print(String.format("%" + scoreLen + ".3f",     pRes.getScore()));
                out.print(" \u00B1");
                out.print(String.format("%" + scoreErrLen + ".3f",  pRes.getScoreError()));
                out.print(String.format("%" + unitLen + "s",        pRes.getScoreUnit()));
                out.println();
            }

            for (Map.Entry<String, Result> e : res.getSecondaryResults().entrySet()) {
                String label = e.getKey();
                Result subRes = e.getValue();

                out.print(String.format("%-" + nameLen + "s",
                        benchPrefixes.get(res.getParams().getBenchmark() + ":" + label)));

                for (String k : params) {
                    String v = res.getParams().getParam(k);
                    out.print(String.format("%" + paramLengths.get(k) + "s", (v == null) ? "N/A" : v));
                }

                out.print(String.format("%" + modeLen + "s",        res.getParams().getMode().shortLabel()));
                out.print(String.format("%" + samplesLen + "d",     subRes.getSampleCount()));
                out.print(String.format("%" + scoreLen + ".3f",     subRes.getScore()));
                out.print(" \u00B1");
                out.print(String.format("%" + scoreErrLen + ".3f",  subRes.getScoreError()));
                out.print(String.format("%" + unitLen + "s",        subRes.getScoreUnit()));
                out.println();
            }
        }

    }
}
