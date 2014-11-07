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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

class XSVResultFormat implements ResultFormat {

    private final PrintWriter pw;
    private final String delimiter;

    public XSVResultFormat(PrintWriter pw, String delimiter) {
        this.pw = pw;
        this.delimiter = delimiter;
    }

    @Override
    public void writeOut(Collection<RunResult> results) {
        SortedSet<String> params = new TreeSet<String>();
        for (RunResult res : results) {
            params.addAll(res.getParams().getParamsKeys());
        }

        printHeader(params);

        for (RunResult rr : results) {
            BenchmarkParams benchParams = rr.getParams();
            Result res = rr.getPrimaryResult();

            printLine(benchParams.getBenchmark(), benchParams, params, res);

            for (String label : rr.getSecondaryResults().keySet()) {
                Result subRes = rr.getSecondaryResults().get(label);
                printLine(benchParams.getBenchmark() + ":" + subRes.getLabel(), benchParams, params, subRes);
            }
        }
    }

    private void printHeader(SortedSet<String> params) {
        pw.write("\"Benchmark\"");
        pw.write(delimiter);
        pw.write("\"Mode\"");
        pw.write(delimiter);
        pw.write("\"Threads\"");
        pw.write(delimiter);
        pw.write("\"Samples\"");
        pw.write(delimiter);
        pw.write("\"Score\"");
        pw.write(delimiter);
        pw.write("\"Score Error (99.9%)\"");
        pw.write(delimiter);
        pw.write("\"Unit\"");
        for (String k : params) {
            pw.write(delimiter);
            pw.write("\"Param: " + k + "\"");
        }
        pw.write("\r\n");
    }

    private void printLine(String label, BenchmarkParams benchmarkParams, SortedSet<String> params, Result result) {
        pw.write("\"");
        pw.write(label);
        pw.write("\"");
        pw.write(delimiter);
        pw.write("\"");
        pw.write(benchmarkParams.getMode().shortLabel());
        pw.write("\"");
        pw.write(delimiter);
        pw.write(String.valueOf(benchmarkParams.getThreads()));
        pw.write(delimiter);
        pw.write(String.valueOf(result.getSampleCount()));
        pw.write(delimiter);
        pw.write(String.valueOf(result.getScore()));
        pw.write(delimiter);
        pw.write(String.valueOf(result.getScoreError()));
        pw.write(delimiter);
        pw.write("\"");
        pw.write(result.getScoreUnit());
        pw.write("\"");

        for (String p : params) {
            pw.write(delimiter);
            pw.write("\"");
            String v = benchmarkParams.getParam(p);
            if (v != null) {
                pw.write(v);
            }
            pw.write("\"");
        }

        pw.write("\r\n");
    }

}
