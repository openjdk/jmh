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
 * Marks the payload method as the target for microbenchmark generation.
 * <p>
 * JMH generators will synthesize the benchmarking code for this method.
 * <p>
 * the parameters for run control are available as separate annotations
 * (e.g. {@link Measurement}, {@link Threads}, and {@link Fork}),
 * which can be used both for concrete {@link GenerateMicroBenchmark}-annotated methods,
 * as well as for the classes containing target methods. Class-level annotations will
 * be honored first, then any method-level annotations.
 * <p>
 * Target method requirements:
 * <ul>
 *     <li>The arguments should be zero or more {@link org.openjdk.jmh.annotations.State}-bearing classes. See
 *     {@link org.openjdk.jmh.annotations.State} docs for the exact contract.
 *     <li>Target method should public</li>
 * </ul>
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
 * </pre></blockquote>
 *
 * @author Staffan Friberg
 * @author Anders Astrand
 * @author Aleksey Shipilev
 * @author Sergey Kuksenko
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateMicroBenchmark {

}
