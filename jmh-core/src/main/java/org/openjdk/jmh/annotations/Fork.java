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
 * <b>Fork annotation allows to set the default forking parameters for the benchmark.</b>
 *
 * <p>This annotation may be put at {@link Benchmark} method to have effect on that
 * method only, or at the enclosing class instance to have the effect over all
 * {@link Benchmark} methods in the class. This annotation may be overridden with
 * the runtime options.</p>
 */
@Inherited
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fork {

    int BLANK_FORKS = -1;

    String BLANK_ARGS = "blank_blank_blank_2014";

    /** @return number of times harness should fork, zero means "no fork" */
    int value() default BLANK_FORKS;

    /** @return number of times harness should fork and ignore the results */
    int warmups() default BLANK_FORKS;

    /** @return JVM executable to run with */
    String jvm() default BLANK_ARGS;

    /** @return JVM arguments to replace in the command line */
    String[] jvmArgs() default { BLANK_ARGS };

    /** @return JVM arguments to prepend in the command line */
    String[] jvmArgsPrepend() default { BLANK_ARGS };

    /** @return JVM arguments to append in the command line */
    String[] jvmArgsAppend() default { BLANK_ARGS };

}
