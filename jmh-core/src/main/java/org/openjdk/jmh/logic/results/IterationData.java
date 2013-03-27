/**
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

import org.openjdk.jmh.logic.results.internal.IterationResult;
import org.openjdk.jmh.profile.ProfilerResult;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class contains all info returned by microbenchmark iteration or/and collected during microbenchmark iteration.
 *
 * @author sergey.kuksenko@oracle.com
 */
public class IterationData {

    private final String benchmark;
    private final List<Result> perThreadResults;
    private final List<ProfilerResult> profilerResults;
    private final int numThreads;
    private final TimeValue runTime;
    private IterationResult aggregated;
    private boolean isWarmup;

    public IterationData(String benchmark, int threadCount, TimeValue runTime) {
        this.benchmark = benchmark;
        this.numThreads = threadCount;
        this.runTime = runTime;
        this.perThreadResults = new ArrayList<Result>(threadCount);
        this.profilerResults = new ArrayList<ProfilerResult>();
    }

    public void addResult(Result result) {
        perThreadResults.add(result);
        aggregated = null;
    }

    public List<Result> getRawResults() {
        return perThreadResults;
    }

    public boolean isResultsEmpty() {
        return perThreadResults.isEmpty();
    }

    public IterationResult getAggregatedResult() {
        if (aggregated == null) {
            aggregated = new IterationResult(perThreadResults);
        }
        return aggregated;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public TimeValue getRuntime() {
        return runTime;
    }

    public void clearResults() {
        perThreadResults.clear();
    }

    public boolean isWarmup() {
        return isWarmup;
    }

    public IterationData setWarmup() {
        isWarmup = true;
        return this;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public void addProfileResult(ProfilerResult profilerResult) {
        profilerResults.add(profilerResult);
    }

    public List<ProfilerResult> getProfilerResults() {
        return profilerResults;
    }

}
