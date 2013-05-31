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
package org.openjdk.jmh.annotations;

/** Level enumeration for Setup and TearDown */
public enum Level {

    /**
     * Trial level.
     *
     * To be executed each run of the benchmark
     */
    Trial,

    /**
     * Iteration level.
     *
     * To be executued for each iteration in a benchmark execution
     */
    Iteration,

    /**
     * Invocation level.
     *
     * To be executed for each benchmark method execution.
     *
     * <p>WARNING #1: This level will bring the unwanted performance
     * effects for the benchmark runs. The use is mostly warranted
     * with the large benchmarks, when the overhead of making the
     * timestamps for each invocation is negligible. It is almost
     * never a good idea to use this otherwise.
     *
     * <p>WARNING #2: Because JMH can not afford synchronizing the
     * state on which the Level.Invocation helper is being called,
     * the helper method execution may overlap with the work itself.
     */
    Invocation,
}
