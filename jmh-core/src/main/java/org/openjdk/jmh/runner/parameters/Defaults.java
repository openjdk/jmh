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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sergey.kuksenko@oracle.com
 */
public class Defaults {

    public static final List<String> TRUE_VALUES  = Collections.unmodifiableList(Arrays.asList("true", "on", "yes"));
    public static final List<String> FALSE_VALUES = Collections.unmodifiableList(Arrays.asList("false", "off", "no"));

    //--- has annotation
    public static final int ITERATION_TIME_SECS = 5;  // seconds
    public static final int ITERATION_COUNT = 20;
    public static final int SINGLE_SHOT_ITERATION_COUNT = 1;

    public static final int WARMUP_TIME_SECS = 3;     // seconds
    public static final int WARMUP_COUNT = 5;
    public static final int SINGLE_SHOT_WARMUP_COUNT = 0;

    public static final TimeValue WARMUP_TIME = new TimeValue(WARMUP_TIME_SECS, TimeUnit.SECONDS);
    public static final TimeValue ITERATION_TIME = new TimeValue(ITERATION_TIME_SECS, TimeUnit.SECONDS);

    public static final int THREADS = 1;

    public static final int DEFAULT_FORK_TIMES = 1;

//--- no annotation
    public static final boolean SHOULD_SYNCH_ITERATIONS = true;

}
