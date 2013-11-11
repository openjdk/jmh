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
package org.openjdk.jmh.logic.results;

import java.util.concurrent.TimeUnit;

public class RawResults {

    private final long opsPerInv;
    public long operations;
    public long realTime;
    public long startTime;
    public long stopTime;

    public long getOperations() {
        return opsPerInv * operations;
    }

    public long getTime() {
        return (realTime > 0) ? realTime : (stopTime - startTime);
    }

    public RawResults(long opsPerInv) {
        this.opsPerInv = opsPerInv;
    }

    public void printOut() {
        System.err.printf("(stop-start)=%10d, real=%10d, ops=%10d, thr=%10d, thrReal=%10d%n",
                TimeUnit.NANOSECONDS.toMicros(stopTime - startTime), TimeUnit.NANOSECONDS.toMicros(realTime),
                operations, (operations*1000*1000/(stopTime - startTime)), (operations*1000*1000/realTime));
    }

}
