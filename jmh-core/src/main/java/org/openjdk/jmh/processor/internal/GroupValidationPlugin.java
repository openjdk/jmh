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
package org.openjdk.jmh.processor.internal;

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Threads;

public class GroupValidationPlugin implements Plugin {

    @Override
    public void process(GeneratorSource source) {
        try {
            for (MethodInfo element : BenchmarkGeneratorUtils.getMethodsAnnotatedWith(source, CompilerControl.class)) {
                if (element.getAnnotation(Threads.class) != null) {
                    source.printError("@" + Threads.class.getSimpleName() + " annotation is placed within " +
                                    "the benchmark method with @" + Group.class.getSimpleName() + " annotation. " +
                                    "This has ambiguous behavioral effect, and prohibited. " +
                                    "Did you mean @" + GroupThreads.class.getSimpleName() + " instead?",
                            element);
                }
            }
        } catch (Throwable t) {
            source.printError("Group validation processor had thrown the unexpected exception", t);
        }
    }

    @Override
    public void finish(GeneratorSource source) {
        // do nothing
    }

}
