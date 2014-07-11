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

/**
 * {@link State} scope.
 */
public enum Scope {

    /**
     * <p>Benchmark state scope.</p>
     *
     * <p>With benchmark scope, all instances of the same type will be shared across all
     * worker threads.</p>
     *
     * <p>{@link Setup} and {@link TearDown} methods on this state object would be performed
     * by one of the worker threads, and only once per {@link Level}.
     * No other threads would ever touch the state object.</p>
     */
    Benchmark,

    /**
     * <p>Group state scope.</p>
     *
     * <p>With group scope, all instances of the same type will be shared across all
     * threads within the same group. Each thread group will be supplied with its own
     * state object.</p>
     *
     * <p>{@link Setup} and {@link TearDown} methods on this state object would be performed
     * by one of the group threads, and only once per {@link Level}.
     * No other threads would ever touch the state object.</p>
     *
     * @see Group
     */
    Group,

    /**
     * <p>Thread state scope.</p>
     *
     * <p>With thread scope, all instances of the same type are distinct, even if multiple
     * state objects are injected in the same benchmark</p>
     *
     * <p>{@link Setup} and {@link TearDown} methods on this state object would be performed
     * by single worker thread exclusively, and only once per {@link Level}.
     * No other threads would ever touch the state object.</p>
     *
     */
    Thread,

}
