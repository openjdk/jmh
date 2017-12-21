/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.it.profilers;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingExternalProfiler implements ExternalProfiler {

    static final AtomicInteger jvmOpts = new AtomicInteger();
    static final AtomicInteger jvmInvokeOpts = new AtomicInteger();
    static final AtomicInteger beforeTrial = new AtomicInteger();
    static final AtomicInteger afterTrial = new AtomicInteger();

    public static void reset() {
        jvmOpts.set(0);
        jvmInvokeOpts.set(0);
        beforeTrial.set(0);
        afterTrial.set(0);
    }

    @Override
    public boolean allowPrintErr() {
        return true;
    }

    @Override
    public boolean allowPrintOut() {
        return true;
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        jvmOpts.incrementAndGet();
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        jvmInvokeOpts.incrementAndGet();
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        beforeTrial.incrementAndGet();
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        afterTrial.incrementAndGet();
        return Collections.emptyList();
    }

    @Override
    public String getDescription() {
        return "Integration Test External Profiler with Stage Counting";
    }

}
