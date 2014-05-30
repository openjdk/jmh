/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.infra.results.AggregationPolicy;
import org.openjdk.jmh.infra.results.Aggregator;
import org.openjdk.jmh.infra.results.Result;
import org.openjdk.jmh.infra.results.ResultRole;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class DummyExternalProfiler implements ExternalProfiler {
    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial() {
        // do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(File stdOut, File stdErr) {
        return Collections.singleton(new MyResult());
    }

    @Override
    public Collection<String> checkSupport() {
        return Collections.emptyList();
    }

    @Override
    public String label() {
        return "dummy";
    }

    @Override
    public String getDescription() {
        return "Dummy External Profiler";
    }

    static class MyResult extends Result<MyResult> {
        MyResult() {
            super(ResultRole.SECONDARY, "dummy", of(42), "dummyunit", AggregationPolicy.AVG);
        }

        @Override
        public Aggregator<MyResult> getIterationAggregator() {
            return new MyResultAggregator();
        }

        @Override
        public Aggregator<MyResult> getRunAggregator() {
            return new MyResultAggregator();
        }
    }

    static class MyResultAggregator implements Aggregator<MyResult> {

        @Override
        public Result aggregate(Collection<MyResult> results) {
            return new MyResult();
        }
    }

}
