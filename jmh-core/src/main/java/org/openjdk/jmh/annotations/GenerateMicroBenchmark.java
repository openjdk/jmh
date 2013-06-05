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
 * Marks the payload method as the target for microbenchmark generation.
 * <p>
 * Annotation processors will translate methods marked with this annotation
 * to correct {@link MicroBenchmark}-annotated classes.
 * <p>
 * This annotation only accepts parameters affecting the workload generation, now only {@link Mode}.
 * Other parameters for run control are available as separate annotations
 * (e.g. {@link Measurement}, {@link Threads}, and {@link Fork}),
 * which can be used both for concrete {@link GenerateMicroBenchmark}-annotated methods,
 * as well as for the classes containing target methods. Class-level annotations will
 * be honored first, then any method-level annotations.
 * <p>
 * Target method requirements:
 * <ul>
 *     <li>The arguments should be zero or more {@link org.openjdk.jmh.annotations.State}-bearing classes. See
 *     {@link org.openjdk.jmh.annotations.State} docs for the exact contract.
 *     <li>Target method should be either public or protected</li>
 * </ul>
 * <p>
 * {@link org.openjdk.jmh.logic.BlackHole}
 * provided is guaranteed to be thread-local and have minimal overhead for making
 * side-effects on consumed values. Consider sinking all the values computed in
 * the benchmark to prevent dead-code elimination. Return value is getting black-holed
 * automatically, so you might prefer using explicit black-holing only when
 * multiple values are getting sinked.
 * <b>Extra care</b> should be excersized when sinking multiple values; one should not sink two
 * results to the same black hole, as compiler can leave only the latest store. Use distinct black
 * holes to sink distinct results.
 * <p>
 * Annotated method can optionally throw Exceptions and Throwables. Any uncaught exception
 * is treated as microbenchmark failure.
 * <p>
 * Simple microbenchmark example:
 * <blockquote><pre>
 * public class CharConversion {
 *      &#64;GenerateMicroBenchmark
 *      public void testCharConversion() {...}
 * }
 *
 * @author Staffan Friberg
 * @author Anders Astrand
 * @author Aleksey Shipilev
 * @author Sergey Kuksenko
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateMicroBenchmark {

    /**
     * This value is deprecated.
     * Use {@link BenchmarkMode} instead.
     */
    @Deprecated
    public BenchmarkType[] value() default { BenchmarkType.OpsPerTimeUnit };


}
