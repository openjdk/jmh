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
import java.util.Arrays;

/**
 * @author sergey.kuksenko@oracle.com
 */
public class IterationParams implements Serializable {

    /**
     * amount of iterations
     */
    private final int count;

    /**
     * iteration runtime
     */
    private final TimeValue timeValue;

    /**
     * Thread count
     */
    private final int threads;

    /**
     * Subgroups distribution
     */
    private final int[] threadGroups;

    public IterationParams(int count, TimeValue time, int threads, int... threadGroups) {
        this.count = count;
        this.timeValue = time;
        this.threads = threads;
        this.threadGroups = threadGroups;
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
        if (threads != that.threads) return false;
        if (!Arrays.equals(threadGroups, that.threadGroups)) return false;
        if (timeValue != null ? !timeValue.equals(that.timeValue) : that.timeValue != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + (timeValue != null ? timeValue.hashCode() : 0);
        result = 31 * result + threads;
        result = 31 * result + (threadGroups != null ? Arrays.hashCode(threadGroups) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IterationParams("+ getCount()+", "+ getTime()+")";
    }

    public int getThreads() {
        return threads;
    }

    public int[] getThreadGroups() {
        return threadGroups;
    }
}
