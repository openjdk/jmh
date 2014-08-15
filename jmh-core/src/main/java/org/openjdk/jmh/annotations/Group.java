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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Execution group.</p>
 *
 * <p>Multiple {@link Benchmark} methods can be bound in the execution group
 * to produce the asymmetric benchmark. Each execution group contains of one
 * or more threads. Each thread within a particular execution group executes
 * one of {@link Group}-annotated {@link Benchmark} methods. The number of
 * threads executing a particular {@link Benchmark} defaults to a single thread,
 * and can be overridden by {@link GroupThreads}.</p>
 *
 * <p>Multiple copies of an execution group may participate in the run, and
 * the number of groups depends on the number of worker threads requested.
 * JMH will take the requested number of worker threads, round it up to execution
 * group size, and then distribute the threads among the (multiple) groups.
 * Among other things, this guarantees fully-populated execution groups.</p>

 * <p>For example, running {@link Group} with two {@link Benchmark} methods,
 * each having {@link GroupThreads}(4), will run 8*N threads, where N is an
 * integer.</p>
 *
 * <p>The group tag is used as the generated benchmark name. The result of each
 * benchmark method in isolation is recorded as secondary result named by the
 * original method name.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {

    /**
     * Group tag. Should be a valid Java identifier.
     * @return group tag
     */
    String value() default "group";
}
