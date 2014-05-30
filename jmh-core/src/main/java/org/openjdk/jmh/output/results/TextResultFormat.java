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
package org.openjdk.jmh.output.results;

import org.openjdk.jmh.infra.results.Result;
import org.openjdk.jmh.infra.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
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
    public void writeOut(Map<BenchmarkRecord, RunResult> runResults) {
        Collection<String> benchNames = new ArrayList<String>();
        for (BenchmarkRecord key : runResults.keySet()) {
            RunResult runResult = runResults.get(key);
            benchNames.add(key.getUsername());
            for (String label : runResult.getSecondaryResults().keySet()) {
                benchNames.add(key.getUsername() + ":" + label);
            }
        }

        Map<String, String> benchPrefixes = ClassUtils.denseClassNames(benchNames);

        // determine max name
        int nameLen = 1;
        for (String prefix : benchPrefixes.values()) {
            nameLen = Math.max(nameLen, prefix.length());
        }
        nameLen += 2;

        Map<String, Integer> paramLengths = new HashMap<String, Integer>();
        SortedSet<String> params = new TreeSet<String>();
        for (BenchmarkRecord br : runResults.keySet()) {
            for (String k : br.getActualParams().keys()) {
                params.add(k);
                Integer len = paramLengths.get(k);
                if (len == null) {
                    len = ("(" + k + ")").length();
                }
                paramLengths.put(k, Math.max(len, br.getActualParam(k).length()));
            }
        }

        out.print(String.format("%-" + nameLen + "s ", "Benchmark"));
        for (String k : params) {
            out.print(String.format("%" + paramLengths.get(k) + "s ", "(" + k + ")"));
        }

        out.print(String.format("%6s %9s %12s %12s %8s%n",
                "Mode", "Samples", "Score", "Score error", "Units"));
        for (BenchmarkRecord key : runResults.keySet()) {
            RunResult res = runResults.get(key);
            {
                out.print(String.format("%-" + nameLen + "s ",
                        benchPrefixes.get(key.getUsername())));

                for (String k : params) {
                    String v = key.getActualParam(k);
                    out.print(String.format("%" + paramLengths.get(k) + "s ", (v == null) ? "N/A" : v));
                }

                out.print(String.format("%6s %9d %12.3f %12.3f %8s%n",
                        key.getMode().shortLabel(),
                        res.getPrimaryResult().getSampleCount(),
                        res.getPrimaryResult().getScore(), res.getPrimaryResult().getScoreError(),
                        res.getScoreUnit()));
            }

            for (String label : res.getSecondaryResults().keySet()) {
                Result subRes = res.getSecondaryResults().get(label);

                out.print(String.format("%-" + nameLen + "s ",
                        benchPrefixes.get(key.getUsername() + ":" + label)));

                for (String k : params) {
                    String v = key.getActualParam(k);
                    out.print(String.format("%" + paramLengths.get(k) + "s ", (v == null) ? "N/A" : v));
                }

                out.print(String.format("%6s %9d %12.3f %12.3f %8s%n",
                        key.getMode().shortLabel(),
                        subRes.getSampleCount(),
                        subRes.getScore(), subRes.getScoreError(),
                        subRes.getScoreUnit()));
            }
        }

    }
}
