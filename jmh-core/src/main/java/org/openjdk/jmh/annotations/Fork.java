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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a micro benchmark method which allows the setting of default
 * fork parameters for the benchmark, means the benchmark should be started in
 * new (forked) JVM.
 *
 * @author sergey.kuksenko@oracle.com
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fork {

    static final String PARAM_NOT_SET = "NOT_SET_20122012"; // random String marking the not set value

    /** specifies number of times harness should fork, zero means "no fork" */
    public int value() default 1;

    /** enforce strict JVM args, replaces any implicit jvm args */
    public String jvmArgs() default PARAM_NOT_SET;

    /** prepend these arguments in the command line */
    public String jvmArgsPrepend() default PARAM_NOT_SET;

    /** append these arguments in the command line */
    public String jvmArgsAppend() default PARAM_NOT_SET;

    /** ignore results first warmups forks */
    public int warmups() default 0;

}

