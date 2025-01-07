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
package org.openjdk.jmh.infra;

import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.Serializable;
import java.util.Objects;

/**
 * Iteration parameters.
 *
 * <p>Iteration parameters are separated in at least two instances, with different {@link IterationType}-s.
 * The complete benchmark parameters not specific for a particular iteration are available in
 * {@link org.openjdk.jmh.infra.BenchmarkParams}.</p>
 * <p>This class is dual-purpose:</p>
 * <ol>
 *     <li>It acts as the interface between host JVM and forked JVM, so that the latter
 *     would not have to figure out the benchmark configuration again</li>
 *     <li>It can be injected into benchmark methods to access the runtime configuration
 *     info about the benchmark</li>
 * </ol>
 */
public final class IterationParams extends IterationParamsL2 {
    private static final long serialVersionUID = -8111111319033802892L;

    byte b3_00, b3_01, b3_02, b3_03, b3_04, b3_05, b3_06, b3_07, b3_08, b3_09, b3_0a, b3_0b, b3_0c, b3_0d, b3_0e, b3_0f;
    long b3_10, b3_11, b3_12, b3_13, b3_14, b3_15, b3_16, b3_17, b3_18, b3_19, b3_1a, b3_1b, b3_1c, b3_1d, b3_1e, b3_1f;
    long b3_20, b3_21, b3_22, b3_23, b3_24, b3_25, b3_26, b3_27, b3_28, b3_29, b3_2a, b3_2b, b3_2c, b3_2d, b3_2e, b3_2f;

    public IterationParams(IterationType type, int count, TimeValue time, int batchSize) {
        super(type, count, time, batchSize);
    }
}

abstract class IterationParamsL2 extends IterationParamsL1 implements Serializable {
    private static final long serialVersionUID = -6138850517953881052L;

    /**
     * iteration type
     */
    protected final IterationType type;

    /**
     * amount of iterations
     */
    protected final int count;

    /**
     * iteration runtime
     */
    protected final TimeValue timeValue;

    /**
     * batch size (method invocations inside the single op)
     */
    protected final int batchSize;

    public IterationParamsL2(IterationType type, int count, TimeValue time, int batchSize) {
        this.type = type;
        this.count = count;
        this.timeValue = time;
        this.batchSize = batchSize;
    }

    /**
     * Iteration type: separates warmup iterations vs. measurement iterations.
     * @return iteration type.
     */
    public IterationType getType() {
        return type;
    }

    /**
     * Number of iterations.
     * @return number of iterations of given type.
     */
    public int getCount() {
        return count;
    }

    /**
     * Time for iteration.
     * @return time
     */
    public TimeValue getTime() {
        return timeValue;
    }

    /**
     * Batch size for iteration.
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IterationParams that = (IterationParams) o;

        if (count != that.count) return false;
        if (batchSize != that.batchSize) return false;
        if (!Objects.equals(timeValue, that.timeValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + batchSize;
        result = 31 * result + (timeValue != null ? timeValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IterationParams("+ getCount()+", "+ getTime()+", "+ getBatchSize()+")";
    }

}

abstract class IterationParamsL1 {
    byte b1_00, b1_01, b1_02, b1_03, b1_04, b1_05, b1_06, b1_07, b1_08, b1_09, b1_0a, b1_0b, b1_0c, b1_0d, b1_0e, b1_0f;
    long b1_10, b1_11, b1_12, b1_13, b1_14, b1_15, b1_16, b1_17, b1_18, b1_19, b1_1a, b1_1b, b1_1c, b1_1d, b1_1e, b1_1f;
    long b1_20, b1_21, b1_22, b1_23, b1_24, b1_25, b1_26, b1_27, b1_28, b1_29, b1_2a, b1_2b, b1_2c, b1_2d, b1_2e, b1_2f;
}