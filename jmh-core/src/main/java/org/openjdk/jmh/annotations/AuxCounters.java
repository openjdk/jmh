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
 * <p>{@link AuxCounters} annotation can be used to mark {@link State} objects
 * as the bearers of auxiliary secondary results. Marking the class with this annotation
 * will make JMH to treat its public fields and result-returning public methods
 * as the base for the secondary benchmark metrics.</p>
 *
 * <p>Properties:</p>
 *
 * <ul>
 *     <li>Auxiliary counters are not available for every {@link BenchmarkMode},
 *     because not every mode counts time or operations. {@link Mode#AverageTime}
 *     and {@link Mode#Throughput} are always supported.</li>
 *
 *     <li>{@link AuxCounters} annotation is only available for {@link Scope#Thread}
 *     state objects. It is a compile-time error to use it with other states. This means
 *     the counters are thread-local in nature.</li>
 *
 *     <li>Only public fields and methods are considered as metrics. If you don't want
 *     a field/method to be captured as metric, do not make it public.</li>
 *
 *     <li>Only numeric fields and numeric-returning methods are considered as
 *     metrics. These include all primitives and their corresponding boxed counterTypes,
 *     except {@code boolean}/{@link Boolean} and {@code char}/{@link Character}.
 *     It is a compile-time error to use the public field/method with incompatible type.</li>
 *
 *     <li>Methods with {@code void} return type are exempted from type checking.
 *     This means helper {@link Setup} and {@link TearDown} methods are fine in
 *     {@link AuxCounters}.</li>
 *
 *     <li>Public fields in {@link AuxCounters} instances would be reset before
 *     starting the iteration, and read back at the end of iteration. This allows
 *     benchmark code to avoid complicated lifecycle handling for these objects.</li>
 *
 *     <li>The counter names are generated from field/method names. The namespace for
 *     counters is shared across all states participating in the run. JMH will fail to
 *     compile the benchmark if there is an ambiguity around what counter comes from
 *     what {@link AuxCounters} class.
 *     </li>
 * </ul>
 *
 * <p><b>CAVEAT: THIS IS AN EXPERIMENTAL API, it may be changed or removed in future
 * without prior warning. This is a sharp tool, use with care.</b></p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuxCounters {

    Type value() default Type.OPERATIONS;

    enum Type {
        /**
         * Counts "operations", which should be relevant to the number of
         * times {@link Benchmark} method was executed. If this counter is
         * incremented on each {@link Benchmark} method invocation, then it
         * will produce a metric similar to the primary benchmark result.
         * This counter will be normalized to benchmark time by the harness.
         */
        OPERATIONS,

        /**
         * Counts "events", the one-off things in the lifetime of workload.
         * This counter would not get normalized to time.
         */
        EVENTS,

        ;
    }
}
