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
 * THIS IS AN EXPERIMENTAL API.
 *  (That means it can be modified, deprecated and removed in future)
 * <p>
 * This annotation can be used to mark {@link State} objects as the bearers of
 * auxiliary secondary results. Marking the class with this annotation will enable
 * JMH to look for {int, long} fields, as well as methods returning {int, long}
 * values, and treat their values as the operation counts in current iteration.
 * <p>
 * NOTE: You have to explicitly reset the state if you don't want the counters
 * to be shared across the iterations.
 * <p>
 * NOTE: This functionality is not available for all {@link BenchmarkMode}-s.
 */
public @interface AuxCounters {
}
