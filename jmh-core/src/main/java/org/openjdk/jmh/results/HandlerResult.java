/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.results;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;

import java.util.Collection;

public class HandlerResult {

    private final IterationResult res;
    private volatile long allOps;
    private volatile long measuredOps;

    public HandlerResult(BenchmarkParams benchmarkParams, IterationParams params) {
        this.res = new IterationResult(benchmarkParams, params);
    }

    public void add(Result r) {
        res.addResult(r);
    }

    public IterationResult getResult() {
        return res;
    }

    public void addResults(Collection<? extends Result> rs) {
        res.addResults(rs);
    }

    public void addMeasuredOps(long v) {
        measuredOps += v;
    }

    public void addAllOps(long v) {
        allOps += v;
    }

    public long getMeasuredOps() {
        return measuredOps;
    }

    public long getAllOps() {
        return allOps;
    }
}
