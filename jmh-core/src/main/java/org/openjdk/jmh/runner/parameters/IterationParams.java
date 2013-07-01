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
     * amount of iterations
     */
    private final int count;

    /**
     * iteration runtime
     */
    private final TimeValue timeValue;

    public IterationParams(int count, TimeValue time) {
        this.count = count;
        this.timeValue = time;
    }

    public int getCount() {
        return count;
    }

    public TimeValue getTime() {
        return timeValue;
    }

    public ThreadIterationParams addThreads(int threads) {
        return new ThreadIterationParams(threads, this);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.count;
        hash = 83 * hash + (this.timeValue != null ? this.timeValue.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IterationParams other = (IterationParams) obj;
        if (this.count != other.count) {
            return false;
        }
        if (this.timeValue != other.timeValue && (this.timeValue == null || !this.timeValue.equals(other.timeValue))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "IterationParams("+ getCount()+", "+ getTime()+")";
    }
}
