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
package org.openjdk.jmh.generators.core;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.generators.source.GeneratorSource;
import org.openjdk.jmh.generators.source.MethodInfo;

public class HelperMethodValidationPlugin implements Plugin {

    @Override
    public void process(GeneratorSource source) {
        try {
            for (MethodInfo element : BenchmarkGeneratorUtils.getMethodsAnnotatedWith(source, Setup.class)) {
                // OK to have these annotation for @State objects
                if (BenchmarkGeneratorUtils.getAnnSuper(element.getDeclaringClass(), State.class) == null) {
                    if (!element.getDeclaringClass().isAbstract()) {
                        source.printError(
                                "@" + Setup.class.getSimpleName() + " annotation is placed within " +
                                        "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                        "This has no behavioral effect, and prohibited.",
                                element);
                    }
                }

                if (!element.isPublic()) {
                    source.printError(
                            "@" + Setup.class.getSimpleName() + " method should be public.",
                            element);
                }
            }

            for (MethodInfo element : BenchmarkGeneratorUtils.getMethodsAnnotatedWith(source, TearDown.class)) {
                // OK to have these annotation for @State objects
                if (BenchmarkGeneratorUtils.getAnnSuper(element.getDeclaringClass(), State.class) == null) {
                    if (!element.getDeclaringClass().isAbstract()) {
                        source.printError(
                                "@" + TearDown.class.getSimpleName() + " annotation is placed within " +
                                        "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                        "This has no behavioral effect, and prohibited.",
                                element);
                    }
                }

                if (!element.isPublic()) {
                    source.printError(
                            "@" + TearDown.class.getSimpleName() + " method should be public.",
                            element);
                }
            }

        } catch (Throwable t) {
            source.printError("Helper method validation generators had thrown the unexpected exception.", t);
        }
    }

    @Override
    public void finish(GeneratorSource source) {
        // do nothing
    }

}
