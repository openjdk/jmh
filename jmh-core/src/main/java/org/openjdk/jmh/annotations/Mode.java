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
package org.openjdk.jmh.annotations;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark mode.
 */
public enum Mode {

    /**
     * Throughput: operations per unit of time.
     * Runs by continuously calling {@link org.openjdk.jmh.annotations.GenerateMicroBenchmark} methods,
     * counting the total throughput over all worker threads.
     */
    Throughput("thrpt", "Throughput, ops/time"),

    /**
     * Average time: average time per per operation.
     * Runs by continuously calling {@link org.openjdk.jmh.annotations.GenerateMicroBenchmark} methods,
     * counting the average time to call over all worker threads. This is the inverse of {@link Mode#Throughput},
     * but with different aggregation policy.
     */
    AverageTime("avgt", "Average time, time/op"),

    /**
     * Sample time: samples the time for each operation.
     * Runs by continuously calling {@link org.openjdk.jmh.annotations.GenerateMicroBenchmark} methods,
     * and randomly samples the time needed for the call. This mode automatically adjusts the sampling
     * frequency, but may omit some pauses which missed the sampling measurement.
     */
    SampleTime("sample", "Sampling time"),

    /**
     * Single shot time: measures the time for a single operation.
     * Runs by calling {@link org.openjdk.jmh.annotations.GenerateMicroBenchmark} once and measuring its time.
     * This mode is useful to estimate the "cold" performance when you don't want to hide the warmup invocations
     * at all. More warmup/measurement iterations are generally required in this mode.
     */
    SingleShotTime("ss", "Single shot invocation time"),

    /**
     * Meta-mode: all the benchmark modes.
     * This is mostly useful for internal JMH testing.
     */
    All("all", "All benchmark modes"),

    ;

    private final String shortLabel;
    private final String longLabel;

    Mode(String shortLabel, String longLabel) {
        this.shortLabel = shortLabel;
        this.longLabel = longLabel;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public String longLabel() {
        return longLabel;
    }

    public static Mode deepValueOf(String name) {
        try {
            return Mode.valueOf(name);
        } catch (IllegalArgumentException iae) {
            Mode inferred = null;
            for (Mode type : values()) {
                if (type.shortLabel().startsWith(name)) {
                    if (inferred == null) {
                        inferred = type;
                    } else {
                        throw new IllegalStateException("Unable to parse benchmark mode, ambiguous prefix given: \"" + name + "\"\n" +
                                "Known values are " + getKnown());
                    }
                }
            }
            if (inferred != null) {
                return inferred;
            } else {
                throw new IllegalStateException("Unable to parse benchmark mode: \"" + name + "\"\n" +
                        "Known values are " + getKnown());
            }
        }
    }

    public static List<String> getKnown() {
        List<String> res = new ArrayList<String>();
        for (Mode type : Mode.values()) {
            res.add(type.name() + "/" + type.shortLabel());
        }
        return res;
    }

}
