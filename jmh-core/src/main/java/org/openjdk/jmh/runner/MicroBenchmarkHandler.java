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

import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.runner.parameters.IterationParams;

/**
 * Handler for a single micro benchmark. Handles name and execution information (# iterations, et c). Executes the
 * benchmark according to above parameters.
 *
 * @author staffan.friberg@oracle.com
 * @author anders.astrand@oracle.com
 * @author aleksey.shipilev@oracle.com
 * @author sergey.kuksenko@oracle.com
 */
public interface MicroBenchmarkHandler {

    /**
     * Runs an iteration on the handled benchmark.
     *
     * @param params  Iteration parameters
     * @param last    Should this iteration considered to be the last
     * @return IterationResult
     */
    public IterationResult runIteration(IterationParams params, boolean last);

    /**
     * Do required shutdown actions. Actions may be:
     * - shutdowns the executor-pool (if required)
     * - etc
     */
    public void shutdown();

    /**
     * @return benchmark name
     */
    public BenchmarkRecord getBenchmark();
}



