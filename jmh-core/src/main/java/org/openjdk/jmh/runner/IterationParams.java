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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.runner.options.TimeValue;

import java.io.Serializable;

public class IterationParams implements Serializable {

    /**
     * iteration type
     */
    private final IterationType type;

    /**
     * amount of iterations
     */
    private final int count;

    /**
     * iteration runtime
     */
    private final TimeValue timeValue;

    /**
     * batch size (method invocations inside the single op)
     */
    private final int batchSize;

    public IterationParams(IterationType type, int count, TimeValue time, int batchSize) {
        this.type = type;
        this.count = count;
        this.timeValue = time;
        this.batchSize = batchSize;
    }

    public IterationType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public TimeValue getTime() {
        return timeValue;
    }

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
        if (timeValue != null ? !timeValue.equals(that.timeValue) : that.timeValue != null) return false;

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
