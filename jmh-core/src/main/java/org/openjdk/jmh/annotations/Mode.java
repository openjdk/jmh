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
 * Benchmark type.
 *
 * @author Staffan Friberg
 * @author Sergey Kuksenko
 * @author Aleksey Shipilev
 */
public enum Mode {

    /**
     * Operations per unit of time,
     * {@link org.openjdk.jmh.logic.results.OpsPerTimeUnit}. */
    Throughput("thrpt", "Throughput, ops/time"),

    /**
     * Average time per operation
     * {@link org.openjdk.jmh.logic.results.AverageTimePerOp}.
     */
    AverageTime("avgt", "Average time, time/op"),

    /**
     * Time distribution, percentile estimation
     * {@link org.openjdk.jmh.logic.results.SampleTimePerOp}.
     */
    SampleTime("sample", "Sampling time"),

    /**
     * Time the single execution
     * {@link org.openjdk.jmh.logic.results.SingleShotTime}.
     */
    SingleShotTime("ss", "Single shot invocation time"),

    /**
     * Meta-mode: all the modes.
     * This is mostly useful for testing.
     */
    All("all", "TEST MODE"),

    /**
     * Legacy mode: don't use directly.
     * This is used notably to support legacy {@link MicroBenchmark} annotation.
     */
    Legacy("legacy", "Legacy mode")

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
            res.add(type.toString());
        }
        for (Mode type : Mode.values()) {
            res.add(type.shortLabel());
        }
        return res;
    }

}
