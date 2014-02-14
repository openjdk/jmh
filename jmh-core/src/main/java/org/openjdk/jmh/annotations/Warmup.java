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
package org.openjdk.jmh.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for a micro benchmark method which allows the setting of default
 * warmup parameters for the benchmark. Any parameter set with the annotation
 * can be overridden by specifying the corresponding command line option.
 *
 * @author sergey.kuksenko@oracle.com
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Warmup {

    public static final int BLANK_ITERATIONS = -1;
    public static final long BLANK_TIME = -1L;
    public static final int BLANK_BATCHSIZE = -1;

    /** Amount of iterations */
    int iterations() default BLANK_ITERATIONS;

    /** time of each iteration */
    long time() default BLANK_TIME;

    /** time unit of the time value */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** Batch size: number of benchmark method calls per operation (some benchmark modes can ignore this setting) */
    int batchSize() default BLANK_BATCHSIZE;

}

