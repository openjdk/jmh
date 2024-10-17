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
import org.openjdk.jmh.util.ScoreFormatter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

class TextResultFormat implements ResultFormat {
    private static final String MODE_COLUMN_NAME = "Mode";
    private static final String COUNT_COLUMN_NAME = "Cnt";
    private static final String SCORE_COLUMN_NAME = "Score";
    private static final String ERROR_COLUMN_NAME = "Error";
    private static final String UNITS_COLUMN_NAME = "Units";
    private static final String BENCHMARK_COLUMN_NAME = "Benchmark";
    private static final int PADDING = 2;

    private final PrintStream out;

    public TextResultFormat(PrintStream out) {
        this.out = out;
    }

    @Override
    public void writeOut(Collection<RunResult> runResults) {
        Collection<String> benchNames = new ArrayList<>();
        for (RunResult runResult : runResults) {
            benchNames.add(runResult.getParams().getBenchmark());
            for (String label : runResult.getSecondaryResults().keySet()) {
                benchNames.add(runResult.getParams().getBenchmark() + ":" + label);
            }
        }

        Map<String, String> benchPrefixes = ClassUtils.denseClassNames(benchNames);

        Lengths lengths = Lengths.create(runResults)
                .update(benchPrefixes.values())
                .pad(PADDING);

        SortedSet<String> params = new TreeSet<>();
        for (RunResult runResult : runResults) {
            params.addAll(runResult.getParams().getParamsKeys());
        }

        out.printf("%-" + lengths.benchmark + "s", BENCHMARK_COLUMN_NAME);
        for (String k : params) {
            out.printf("%" + lengths.ofParameter(k) + "s", "(" + k + ")");
        }

        out.printf("%" + lengths.mode + "s", MODE_COLUMN_NAME);
        out.printf("%" + lengths.samples + "s", COUNT_COLUMN_NAME);
        out.printf("%" + lengths.score + "s", SCORE_COLUMN_NAME);
        out.print("  ");
        out.printf("%" + lengths.error + "s", ERROR_COLUMN_NAME);
        out.printf("%" + lengths.unit + "s", UNITS_COLUMN_NAME);
        out.println();

        for (RunResult res : runResults) {
            {
                out.printf("%-" + lengths.benchmark + "s", benchPrefixes.get(res.getParams().getBenchmark()));

                for (String k : params) {
                    String v = res.getParams().getParam(k);
                    out.printf("%" + lengths.ofParameter(k) + "s", (v == null) ? "N/A" : v);
                }

                Result pRes = res.getPrimaryResult();
                out.printf("%" + lengths.mode + "s", res.getParams().getMode().shortLabel());

                if (pRes.getSampleCount() > 1) {
                    out.printf("%" + lengths.samples + "d", pRes.getSampleCount());
                } else {
                    out.printf("%" + lengths.samples + "s", "");
                }

                out.print(ScoreFormatter.format(lengths.score, pRes.getScore()));

                if (!Double.isNaN(pRes.getScoreError()) && !ScoreFormatter.isApproximate(pRes.getScore())) {
                    out.print(" \u00B1");
                    out.print(ScoreFormatter.formatError(lengths.error, pRes.getScoreError()));
                } else {
                    out.print("  ");
                    out.printf("%" + lengths.error + "s", "");
                }

                out.printf("%" + lengths.unit + "s", pRes.getScoreUnit());
                out.println();
            }

            for (Map.Entry<String, Result> e : res.getSecondaryResults().entrySet()) {
                String label = e.getKey();
                Result subRes = e.getValue();

                out.printf("%-" + lengths.benchmark + "s",
                        benchPrefixes.get(res.getParams().getBenchmark() + ":" + label));

                for (String k : params) {
                    String v = res.getParams().getParam(k);
                    out.printf("%" + lengths.ofParameter(k) + "s", (v == null) ? "N/A" : v);
                }

                out.printf("%" + lengths.mode + "s", res.getParams().getMode().shortLabel());

                if (subRes.getSampleCount() > 1) {
                    out.printf("%" + lengths.samples + "d", subRes.getSampleCount());
                } else {
                    out.printf("%" + lengths.samples + "s", "");
                }

                out.print(ScoreFormatter.format(lengths.score, subRes.getScore()));

                if (!Double.isNaN(subRes.getScoreError()) && !ScoreFormatter.isApproximate(subRes.getScore())) {
                    out.print(" \u00B1");
                    out.print(ScoreFormatter.formatError(lengths.error, subRes.getScoreError()));
                } else {
                    out.print("  ");
                    out.printf("%" + lengths.error + "s", "");
                }

                out.printf("%" + lengths.unit + "s", subRes.getScoreUnit());
                out.println();
            }
        }
    }

    private static class Lengths {
        private int benchmark = BENCHMARK_COLUMN_NAME.length();
        private int mode = MODE_COLUMN_NAME.length();
        private int samples = COUNT_COLUMN_NAME.length();
        private int score = SCORE_COLUMN_NAME.length();
        private int error = ERROR_COLUMN_NAME.length();
        private int unit = UNITS_COLUMN_NAME.length();
        private final Map<String, Integer> parameters = new HashMap<>();

        public static Lengths create(Collection<RunResult> results) {
            Lengths instance = new Lengths();
            for (RunResult result : results) {
                instance.update(result);
            }
            return instance;
        }

        public Lengths update(Collection<String> names) {
            for (String name : names) {
                benchmark = Math.max(benchmark, name.length());
            }

            return this;
        }

        public Lengths update(RunResult result) {
            mode = Math.max(mode, result.getParams().getMode().shortLabel().length());
            update(result.getPrimaryResult());

            for (Result<?> child : result.getSecondaryResults().values()) {
                update(child);
            }

            BenchmarkParams benchmarkParameters = result.getParams();
            for (String key : benchmarkParameters.getParamsKeys()) {
                Integer current = this.parameters.get(key);

                if (current == null) {
                    current = key.length() + 2; // accounting for parentheses
                }

                this.parameters.put(key, Math.max(current, benchmarkParameters.getParam(key).length()));
            }

            return this;
        }

        public Lengths update(Result<?> result) {
            samples = Math.max(samples, String.format("%d", result.getSampleCount()).length());
            score = Math.max(score, ScoreFormatter.format(result.getScore()).length());
            error = Math.max(error, ScoreFormatter.format(result.getScoreError()).length());
            unit = Math.max(unit, result.getScoreUnit().length());

            return this;
        }

        public Lengths pad(int padding) {
            mode += padding;
            samples += padding;
            score += padding;
            error += padding - 1; // digest a single character for +- separator
            unit += padding;

            for (String parameter : parameters.keySet()) {
                parameters.put(parameter, parameters.get(parameter) + padding);
            }

            return this;
        }

        public int ofParameter(String parameter) {
            return parameters.get(parameter);
        }
    }
}
