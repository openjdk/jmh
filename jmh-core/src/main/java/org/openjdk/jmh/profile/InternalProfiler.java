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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.util.Collection;

/**
 * Internal profiler.
 *
 * <p>Internal profilers run in the benchmark JVM, and may query the internal
 * JVM facilities.</p>
 */
public interface InternalProfiler extends Profiler {

    /**
     * Run this code before starting the next benchmark iteration.
     *
     * @param benchmarkParams benchmark parameters used for current launch
     * @param iterationParams iteration parameters used for current launch
     */
    void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams);

    /**
     * Run this code after a benchmark iteration finished
     *
     * @param benchmarkParams benchmark parameters used for current launch
     * @param iterationParams iteration parameters used for current launch
     * @param result iteration result
     * @return profiler results
     */
    Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result);

}
