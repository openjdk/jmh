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

import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.options.WarmupMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Holds the JMH global defaults.
 *
 * @author Sergey Kuksenko
 * @author Aleksey Shipilev
 */
public class Defaults {

    public static final List<String> TRUE_VALUES  = Collections.unmodifiableList(Arrays.asList("true", "on", "yes"));
    public static final List<String> FALSE_VALUES = Collections.unmodifiableList(Arrays.asList("false", "off", "no"));

    public static final int MEASUREMENT_ITERATIONS = 20;
    public static final int SINGLESHOT_MEASUREMENT_ITERATIONS = 1;
    public static final int MEASUREMENT_BATCHSIZE = 1;

    public static final int WARMUP_ITERATIONS = 20;
    public static final int SINGLESHOT_WARMUP_ITERATIONS = 0;
    public static final int WARMUP_BATCHSIZE = 1;

    public static final TimeValue WARMUP_TIME = new TimeValue(1, TimeUnit.SECONDS);
    public static final TimeValue ITERATION_TIME = new TimeValue(1, TimeUnit.SECONDS);

    public static final int THREADS = 1;

    public static final int FORKS = 10;
    public static final int WARMUP_FORKS = 0;

    public static final boolean FAIL_ON_ERROR = false;
    public static final boolean SYNC_ITERATIONS = true;
    public static final boolean DO_GC = false;

    public static final ResultFormatType RESULT_FORMAT = ResultFormatType.NONE;
    public static final String RESULT_FILE = "jmh.out";

    public static final WarmupMode WARMUP_MODE = WarmupMode.INDI;

    public static final VerboseMode VERBOSITY = VerboseMode.NORMAL;

}
