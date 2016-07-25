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

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.options.WarmupMode;

import java.util.concurrent.TimeUnit;

/**
 * JMH global defaults: these are used when no other values are available.
 */
public class Defaults {

    /**
     * Number of warmup iterations.
     */
    public static final int WARMUP_ITERATIONS = 20;

    /**
     * Number of warmup iterations in {@link org.openjdk.jmh.annotations.Mode#SingleShotTime} mode.
     */
    public static final int WARMUP_ITERATIONS_SINGLESHOT = 0;

    /**
     * The batch size in warmup mode.
     */
    public static final int WARMUP_BATCHSIZE = 1;

    /**
     * The duration of warmup iterations.
     */
    public static final TimeValue WARMUP_TIME = TimeValue.seconds(1);

    /**
     * Number of measurement iterations.
     */
    public static final int MEASUREMENT_ITERATIONS = 20;

    /**
     * Number of measurement iterations in {@link org.openjdk.jmh.annotations.Mode#SingleShotTime} mode.
     */
    public static final int MEASUREMENT_ITERATIONS_SINGLESHOT = 1;

    /**
     * The batch size in measurement mode.
     */
    public static final int MEASUREMENT_BATCHSIZE = 1;

    /**
     * The duration of measurement iterations.
     */
    public static final TimeValue MEASUREMENT_TIME = TimeValue.seconds(1);

    /**
     * Number of measurement threads.
     */
    public static final int THREADS = 1;

    /**
     * Number of forks in which we measure the workload.
     */
    public static final int MEASUREMENT_FORKS = 10;

    /**
     * Number of warmup forks we discard.
     */
    public static final int WARMUP_FORKS = 0;

    /**
     * Should JMH fail on benchmark error?
     */
    public static final boolean FAIL_ON_ERROR = false;

    /**
     * Should JMH synchronize iterations?
     */
    public static final boolean SYNC_ITERATIONS = true;

    /**
     * Should JMH do GC between iterations?
     */
    public static final boolean DO_GC = false;

    /**
     * The default {@link org.openjdk.jmh.results.format.ResultFormatType} to use.
     */
    public static final ResultFormatType RESULT_FORMAT = ResultFormatType.CSV;

    /**
     * Default prefix of the result file.
     */
    public static final String RESULT_FILE_PREFIX = "jmh-result";

    /**
     * Default {@link org.openjdk.jmh.runner.options.WarmupMode}.
     */
    public static final WarmupMode WARMUP_MODE = WarmupMode.INDI;

    /**
     * Default {@link org.openjdk.jmh.runner.options.VerboseMode}.
     */
    public static final VerboseMode VERBOSITY = VerboseMode.NORMAL;

    /**
     * Default running mode.
     */
    public static final Mode BENCHMARK_MODE = Mode.Throughput;

    /**
     * Default output time unit.
     */
    public static final TimeUnit OUTPUT_TIMEUNIT = TimeUnit.SECONDS;

    /**
     * Default operations per invocation.
     */
    public static final Integer OPS_PER_INVOCATION = 1;

    /**
     * Default timeout.
     */
    public static final TimeValue TIMEOUT = TimeValue.minutes(10);

    /**
     * Default benchmarks to include.
     */
    public static final String INCLUDE_BENCHMARKS = ".*";

}
