/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.runner.CompilerHints;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class CompilerControlPlugin {

    private final SortedSet<String> lines = new TreeSet<>();

    private final Set<MethodInfo> defaultForceInlineMethods = new TreeSet<>(new Comparator<MethodInfo>() {
        @Override
        public int compare(MethodInfo o1, MethodInfo o2) {
            return o1.getQualifiedName().compareTo(o2.getQualifiedName());
        }
    });

    private final Set<String> alwaysDontInlineMethods = new TreeSet<>();

    public void defaultForceInline(MethodInfo methodInfo) {
        defaultForceInlineMethods.add(methodInfo);
    }

    public void alwaysDontInline(String className, String methodName) {
        alwaysDontInlineMethods.add(getName(className, methodName));
    }

    public void process(GeneratorSource source, GeneratorDestination destination) {
        try {
            for (MethodInfo element : BenchmarkGeneratorUtils.getMethodsAnnotatedWith(source, CompilerControl.class)) {
                CompilerControl ann = element.getAnnotation(CompilerControl.class);
                if (ann == null) {
                    throw new IllegalStateException("No annotation");
                }

                CompilerControl.Mode command = ann.value();
                lines.add(command.command() + "," + getName(element));
            }

            for (MethodInfo element : defaultForceInlineMethods) {
                // Skip methods annotated explicitly
                if (element.getAnnotation(CompilerControl.class) != null) continue;

                // Skip methods in classes that are annotated explicitly
                if (element.getDeclaringClass().getAnnotation(CompilerControl.class) != null) continue;

                lines.add(CompilerControl.Mode.INLINE.command() + "," + getName(element));
            }

            for (String element : alwaysDontInlineMethods) {
                lines.add(CompilerControl.Mode.DONT_INLINE.command() + "," + element);
            }

            for (ClassInfo element : BenchmarkGeneratorUtils.getClassesAnnotatedWith(source, CompilerControl.class)) {
                CompilerControl ann = element.getAnnotation(CompilerControl.class);
                if (ann == null) {
                    throw new IllegalStateException("No annotation");
                }

                CompilerControl.Mode command = ann.value();
                lines.add(command.command() + "," + getName(element));
            }

        } catch (Throwable t) {
            destination.printError("Compiler control generators had thrown the unexpected exception", t);
        }
    }

    public void finish(GeneratorSource source, GeneratorDestination destination) {
        try {
            PrintWriter writer = new PrintWriter(destination.newResource(CompilerHints.LIST.substring(1)));
            for (String line : lines) {
                writer.println(line);
            }
            writer.close();
        } catch (IOException ex) {
            destination.printError("Error writing compiler hint list ", ex);
        } catch (Throwable t) {
            destination.printError("Compiler control generators had thrown the unexpected exception", t);
        }
    }

    private static String getName(String className, String methodName) {
        return className.replaceAll("\\.", "/") + "." + methodName;
    }

    private static String getName(MethodInfo mi) {
       return getName(getClassName(mi.getDeclaringClass()), mi.getName());
    }

    private static String getName(ClassInfo ci) {
        return getName(getClassName(ci), "*");
    }

    private static String getClassName(ClassInfo ci) {
        return ci.getPackageName() + "." + BenchmarkGeneratorUtils.getNestedNames(ci);
    }

}
