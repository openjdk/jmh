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

import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class CSVResultFormat implements ResultFormat {

    /*
     * Implementation note:
     *    We follow the provisions of http://tools.ietf.org/html/rfc4180
     */

    private final String output;

    public CSVResultFormat(String output) {
        this.output = output;
    }

    @Override
    public void writeOut(Map<BenchmarkRecord, RunResult> results) {
        FileWriter fw = null;
        try  {
            fw = new FileWriter(output);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("\"Benchmark\",\"Mode\",\"Threads\",\"Iterations\",\"Iteration time\",\"Mean\",\"Mean Error (99.9%)\",\"Unit\"");
            bw.write("\r\n");

            for (BenchmarkRecord br : results.keySet()) {
                RunResult runResult = results.get(br);

                bw.write("\"");
                bw.write(br.getUsername());
                bw.write("\",\"");
                bw.write(br.getMode().shortLabel());
                bw.write("\",");
                bw.write(String.valueOf(runResult.getParams().getThreads()));
                bw.write(",");
                bw.write(String.valueOf(runResult.getParams().getMeasurement().getCount()));
                bw.write(",\"");
                bw.write(String.valueOf(runResult.getParams().getMeasurement().getTime()));
                bw.write("\",");
                bw.write(String.valueOf(runResult.getPrimaryResult().getStatistics().getMean()));
                bw.write(",");
                bw.write(String.valueOf(runResult.getPrimaryResult().getStatistics().getMeanErrorAt(0.999)));
                bw.write(",\"");
                bw.write(runResult.getPrimaryResult().getScoreUnit());
                bw.write("\"");
                bw.write("\r\n");
            }
            bw.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

    }

}
