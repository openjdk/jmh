/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class LaTeXResultFormat implements ResultFormat {

    private final PrintWriter pw;

    public LaTeXResultFormat(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void writeOut(Collection<RunResult> results) {
        SortedSet<String> params = new TreeSet<String>();
        Set<String> benchNames = new HashSet<String>();

        Set<String> units = new HashSet<String>();
        for (RunResult rr : results) {
            params.addAll(rr.getParams().getParamsKeys());
            units.add(rr.getPrimaryResult().getScoreUnit());
            benchNames.add(rr.getParams().getBenchmark());
            for (String label : rr.getSecondaryResults().keySet()) {
                benchNames.add(rr.getParams().getBenchmark() + ":" + label);
            }
        }

        boolean singleUnit = (units.size() == 1);
        String unit = singleUnit ? units.iterator().next() : null;

        Map<String, String> prefixes = ClassUtils.denseClassNames(benchNames);

        printHeader(params, singleUnit, unit);

        for (RunResult rr : results) {
            BenchmarkParams benchmarkParams = rr.getParams();
            Result res = rr.getPrimaryResult();

            printLine(benchmarkParams.getBenchmark(), benchmarkParams, params, prefixes, singleUnit, res);

            for (String label : rr.getSecondaryResults().keySet()) {
                Result subRes = rr.getSecondaryResults().get(label);
                printLine(benchmarkParams.getBenchmark() + ":" + label, benchmarkParams, params, prefixes, singleUnit, subRes);
            }
        }

        printFooter();
    }

    private void printHeader(SortedSet<String> params, boolean singleUnit, String unit) {
        pw.write("\\begin{tabular}{r|");
        for (String p : params) {
            pw.write("l|");
        }
        pw.write("rl" + (singleUnit ? "" : "l") + "}\n");
        pw.write(" \\multicolumn{1}{c|}{\\texttt{Benchmark}} & ");
        for (String p : params) {
            pw.printf("\\texttt{%s}", p);
            pw.write(" & ");
        }
        pw.write(" \\multicolumn{" + (singleUnit ? 2 : 3) + "}{c}{\\texttt{Score" + (singleUnit ? ", " + unit : "") + "}} \\\\\n");
        pw.write("\\hline\n");
    }

    private void printFooter() {pw.write("\\end{tabular}");}

    private void printLine(String label, BenchmarkParams benchParams, SortedSet<String> params,
                           Map<String, String> prefixes, boolean singleUnit, Result res) {
        pw.printf("\\texttt{%s} & ", escape(prefixes.get(label)));
        for (String p : params) {
            pw.printf("\\texttt{%s}", escape(benchParams.getParam(p)));
            pw.write(" & ");
        }
        pw.printf("\\texttt{%5.3f} & ", res.getScore());
        pw.printf("\\scriptsize $\\pm$ \\texttt{%5.3f}", res.getScoreError());
        if (!singleUnit) {
            pw.printf(" & \\texttt{%s}", res.getScoreUnit());
        }
        pw.write(" \\\\");

        pw.write("\n");
    }

    private static String escape(String s) {
        return s.replaceAll("_", "\\\\_");
    }

}
