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

/**
 * Marks the configurable parameter in the benchmark.
 * <p/>
 * {@link Param} fields should be non-final instance fields,
 * and should only reside in in {@link State} classes. JMH will inject
 * the value into the annotated field before any {@link Setup} method
 * is called. It is *not* guaranteed the field value would be accessible
 * in any instance initializer of {@link State}.
 * <p/>
 * Parameters are acceptable on any primitive type, primitive wrapper type,
 * or a String. The annotation value is given in String, and will be
 * valueOf-ed as required to match the field type.
 * <p/>
 * When multiple {@link Param}-s are needed for the benchmark run,
 * JMH will compute the outer product of all the parameters in the run.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
public @interface Param {

    public static final String BLANK_ARGS = "blank_blank_blank_2014";

    /**
     * Default values for the parameter.
     * @return values sequence to follow.
     */
    String[] value() default { BLANK_ARGS };

}
