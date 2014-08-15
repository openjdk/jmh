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
 * <p>Marks the state object.</p>
 *
 * <p>State objects naturally encapsulate the state on which benchmark is working on.
 * The {@link Scope} of state object defines to which extent it is shared among the
 * worker threads.</p>
 *
 * <p>State objects are usually injected into {@link Benchmark} methods as arguments,
 * and JMH takes care of their instantiation and sharing. State objects may also be
 * injected into {@link Setup} and {@link TearDown} methods of other {@link State}
 * objects to get the staged initialization. In that case, the dependency graph
 * between the {@link State}-s should be directed acyclic graph.</p>
 *
 * <p>State objects may be inherited: you can place {@link State} on a super class and
 * use subclasses as states.</p>
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface State {

    /**
     * State scope.
     * @return state scope
     * @see Scope
     */
    Scope value();

}
