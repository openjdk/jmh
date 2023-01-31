/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmh.validation.tests;

import org.openjdk.jmh.benchmarks.RoundTripLatencyBench;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.validation.AffinitySupport;
import org.openjdk.jmh.validation.SpinWaitSupport;
import org.openjdk.jmh.validation.ValidationTest;

import java.io.PrintWriter;

public class RoundTripLatencyTest extends ValidationTest {

    private boolean spinWaitHints;

    public RoundTripLatencyTest(boolean spinWaitHints) {
        this.spinWaitHints = spinWaitHints;
    }

    @Override
    public void runWith(PrintWriter pw, Options parent) throws RunnerException {
        pw.println("--------- ROUND-TRIP LATENCY TEST" + (spinWaitHints ? " (SPIN-WAIT HINTS)" : ""));
        pw.println();

        org.openjdk.jmh.util.Utils.reflow(pw,
                "This test tries to run latency benchmark across the entire system. " +
                    "For many-core systems, it is normal to see large latency variations between CPU threads pairs. " +
                    "This gives the idea how much the tests with communicating threads would differ when scheduled differently.",
                80, 2);
        pw.println();

        pw.println("  Scores are nanoseconds per round-trip.");
        pw.println("  Axes are CPU numbers as presented by OS.");
        pw.println();

        if (!AffinitySupport.isSupported()) {
            pw.println("  Affinity control is not available on this machine, skipping the test.");
            pw.println();
            return;
        }

        if (spinWaitHints && !SpinWaitSupport.available()) {
            pw.println("  Spin-wait hints are not supported, skipping the test.");
            pw.println();
            return;
        }

        Options basic = new OptionsBuilder()
                .parent(parent)
                .include(RoundTripLatencyBench.class.getCanonicalName())
                .threads(1)
                .jvmArgsAppend("-Xms512m", "-Xmx512m", "-XX:+AlwaysPreTouch", "-XX:+UseParallelGC", "-XX:+UseNUMA", "-DspinWait=" + spinWaitHints)
                .verbosity(VerboseMode.SILENT)
                .build();

        int blockSize = 16;

        int threads = Utils.figureOutHotCPUs();
        int blocks = (threads / blockSize);
        if (blocks*blockSize < threads) blocks++;

        for (int pBlock = 0; pBlock < blocks; pBlock++) {
            int fromP = pBlock*blockSize;
            int toP = Math.min(threads, (pBlock+1)*blockSize);

            for (int cBlock = 0; cBlock < blocks; cBlock++) {
                int fromC = cBlock*blockSize;
                int toC = Math.min(threads, (cBlock+1)*blockSize);
                pw.printf("%5s  ", "");
                for (int c = fromC; c < toC; c++) {
                    pw.printf("%5d:", c);
                }
                pw.println();

                for (int p = fromP; p < toP; p++) {
                    pw.printf("%5d: ", p);

                    for (int c = fromC; c < toC; c++) {
                        if (p == c) {
                            pw.print(" ----,");
                            continue;
                        }
                        Options opts = new OptionsBuilder()
                                .parent(basic)
                                .param("p", String.valueOf(p))
                                .param("c", String.valueOf(c))
                                .build();
                        Result r = new Runner(opts).runSingle().getPrimaryResult();
                        pw.print(String.format("%5.0f,", r.getScore()));
                        pw.flush();
                    }
                    pw.println();
                }
                pw.println();
            }
        }

    }

}
