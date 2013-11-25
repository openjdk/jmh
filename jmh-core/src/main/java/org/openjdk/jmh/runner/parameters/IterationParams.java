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
package org.openjdk.jmh.runner.parameters;

import java.io.Serializable;

/**
 * @author sergey.kuksenko@oracle.com
 */
public class IterationParams implements Serializable {

    /**
     * Benchmark parameters
     */
    private final BenchmarkParams benchmarkParams;

    /**
     * amount of iterations
     */
    private final int count;

    /**
     * iteration runtime
     */
    private final TimeValue timeValue;


    public IterationParams(BenchmarkParams params, int count, TimeValue time) {
        this.count = count;
        this.timeValue = time;
        this.benchmarkParams = params;
    }

    public int getCount() {
        return count;
    }

    public TimeValue getTime() {
        return timeValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IterationParams that = (IterationParams) o;

        if (count != that.count) return false;
        if (timeValue != null ? !timeValue.equals(that.timeValue) : that.timeValue != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + (timeValue != null ? timeValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IterationParams("+ getCount()+", "+ getTime()+")";
    }

    public BenchmarkParams getBenchmarkParams() {
        return benchmarkParams;
    }

}
