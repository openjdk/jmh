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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Multiset;
import org.openjdk.jmh.util.Multisets;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Very basic and naive stack profiler.
 */
public class StackProfiler implements InternalProfiler {

    /** Number of stack lines to save */
    private static final int SAMPLE_STACK_LINES = Integer.getInteger("jmh.stack.lines", 1);

    /** Number of top stacks to show */
    private static final int SAMPLE_TOP_STACKS =  Integer.getInteger("jmh.stack.top", 10);

    /** Sampling period */
    private static final int SAMPLE_PERIOD_MSEC = Integer.getInteger("jmh.stack.period", 10);

    /** Record detailed line info */
    private static final boolean SAMPLE_LINE =    Boolean.getBoolean("jmh.stack.detailLine");

    /** Threads to ignore (known system and harness threads) */
    private static final String[] IGNORED_THREADS = {
            "Finalizer",
            "Signal Dispatcher",
            "Reference Handler",
            "LoopTimer",
            "main",
            "Sampling Thread",
            "Attach Listener"
    };

    private volatile SamplingTask samplingTask;

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        samplingTask = new SamplingTask();
        samplingTask.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        samplingTask.stop();
        return Arrays.asList(new StackResult(samplingTask.stacks));
    }

    @Override
    public Collection<String> checkSupport() {
        return Collections.emptyList();
    }


    @Override
    public String label() {
        return "stack";
    }

    @Override
    public String getDescription() {
        return "Simple and naive Java stack profiler";
    }

    public static class SamplingTask implements Runnable {

        private final Thread thread;
        private final Multiset<StackRecord> stacks;

        public SamplingTask() {
            stacks = new HashMultiset<StackRecord>();
            thread = new Thread(this);
            thread.setName("Sampling Thread");
        }

        @Override
        public void run() {

            while (!Thread.interrupted()) {
                ThreadInfo[] infos = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
                info: for (ThreadInfo info : infos) {

                    // filter out ignored threads
                    for (String ignore : IGNORED_THREADS) {
                        if (info.getThreadName().equalsIgnoreCase(ignore)) {
                            continue info;
                        }
                    }

                    StackTraceElement[] stack = info.getStackTrace();
                    String[] stackLines = new String[Math.min(stack.length, SAMPLE_STACK_LINES)];
                    for (int i = 0; i < Math.min(stack.length, SAMPLE_STACK_LINES); i++) {
                        stackLines[i] =
                            stack[i].getClassName() +
                                    '.' + stack[i].getMethodName()
                                    + (SAMPLE_LINE ? ":" + stack[i].getLineNumber() : "");
                    }
                    stacks.add(new StackRecord(info.getThreadState(), stackLines));
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(SAMPLE_PERIOD_MSEC);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void start() {
            thread.start();
        }

        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private static class StackRecord implements Serializable {
        public final Thread.State threadState;
        public final String[] lines;

        private StackRecord(Thread.State threadState, String[] lines) {
            this.threadState = threadState;
            this.lines = lines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StackRecord that = (StackRecord) o;

            if (!Arrays.equals(lines, that.lines)) return false;
            if (threadState != that.threadState) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = threadState != null ? threadState.hashCode() : 0;
            result = 31 * result + (lines != null ? Arrays.hashCode(lines) : 0);
            return result;
        }
    }

    public static class StackResult extends Result<StackResult> {
        private final Multiset<StackRecord> stacks;

        public StackResult(Multiset<StackRecord> stacks) {
            super(ResultRole.SECONDARY, "@stack", new ListStatistics(), "none", AggregationPolicy.AVG);
            this.stacks = stacks;
        }

        @Override
        public Aggregator<StackResult> getThreadAggregator() {
            return new StackResultAggregator();
        }

        @Override
        public Aggregator<StackResult> getIterationAggregator() {
            return new StackResultAggregator();
        }

        @Override
        public String toString() {
            return "<delayed till summary>";
        }

        @Override
        public String extendedInfo(String label) {
            return getStack(stacks);
        }

        public String getStack(Multiset<StackRecord> stacks) {
            Collection<StackRecord> cut = Multisets.countHighest(stacks, SAMPLE_TOP_STACKS);
            StringBuilder builder = new StringBuilder();

            builder.append("Stack profiler:\n");

            int totalDisplayed = 0;
            int count = 0;
            for (StackRecord s : cut) {
                if (count++ > SAMPLE_TOP_STACKS) {
                    break;
                }

                String[] lines = s.lines;
                if (lines.length > 0) {
                    totalDisplayed += stacks.count(s);
                    builder.append(String.format("%5.1f%% %10s %s\n", stacks.count(s) * 100.0 / stacks.size(), s.threadState, lines[0]));
                    if (lines.length > 1) {
                        for (int i = 1; i < lines.length; i++) {
                            builder.append(String.format("%5s  %10s %s\n", "", "", lines[i]));
                        }
                        builder.append("\n");
                    }
                }
            }
            builder.append(String.format("%5.1f%% %10s %s\n", (stacks.size() - totalDisplayed) * 100.0 / stacks.size(), "", "(other)"));

            return builder.toString();
        }
    }

    public static class StackResultAggregator implements Aggregator<StackResult> {
        @Override
        public Result aggregate(Collection<StackResult> results) {
            Multiset<StackRecord> sum = new HashMultiset<StackRecord>();
            for (StackResult r : results) {
                for (StackRecord rec : r.stacks.keys()) {
                    sum.add(rec, r.stacks.count(rec));
                }
            }
            return new StackResult(sum);
        }
    }

}
