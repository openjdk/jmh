/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * <p>Marks the configurable parameter in the benchmark.</p>
 *
 * <p>{@link Param} fields should be non-final fields,
 * and should only reside in in {@link State} classes. JMH will inject
 * the value into the annotated field before any {@link Setup} method
 * is called. It is <b>not</b> guaranteed the field value would be accessible
 * in any initializer or any constructor of {@link State}.</p>
 *
 * <p>Parameters are acceptable on any primitive type, primitive wrapper type,
 * a String, or an Enum. The annotation value is given in String, and will be
 * coerced as required to match the field type.</p>
 *
 * <p>Parameters should normally provide the default values which make
 * benchmark runnable even without the explicit parameters set for the run.
 * The only exception is {@link Param} over {@link java.lang.Enum}, which
 * will implicitly have the default value set encompassing all enum constants.</p>
 *
 * <p>When multiple {@link Param}-s are needed for the benchmark run,
 * JMH will compute the outer product of all the parameters in the run.</p>
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    String BLANK_ARGS = "blank_blank_blank_2014";

    /**
     * Default values sequence for the parameter. By default, the parameter
     * values will be traversed during the run in the given order.
     *
     * @return values sequence to follow.
     */
    String[] value() default { BLANK_ARGS };

}
