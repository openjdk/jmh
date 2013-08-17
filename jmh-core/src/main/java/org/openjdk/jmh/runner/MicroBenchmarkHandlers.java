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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.annotations.MicroBenchmark;
import org.openjdk.jmh.logic.InfraControl;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.BaseOptions;
import org.openjdk.jmh.runner.parameters.MicroBenchmarkParameters;
import org.openjdk.jmh.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * Utility class for MicroBenchmarkHandlers.
 */
public class MicroBenchmarkHandlers {

    // Suppresses default constructor, ensuring non-instantiability.
    private MicroBenchmarkHandlers() {
    }

    public static Method findBenchmarkMethod(BenchmarkRecord benchmark) {
        Class<?> clazz = ClassUtils.loadClass(benchmark.generatedClass());
        return findBenchmarkMethod(clazz, benchmark.generatedMethod());
    }

    public static Method findBenchmarkMethod(Class<?> clazz, String methodName) {
        Method method = null;
        for (Method m : ClassUtils.enumerateMethods(clazz)) {
            if (m.getName().equals(methodName)) {
                if (isValidBenchmarkSignature(m)) {
                    if (method != null) {
                        throw new IllegalArgumentException("Ambiguous methods: \n" + method + "\n and \n" + m + "\n, which one to execute?");
                    }
                    method = m;
                } else {
                    throw new IllegalArgumentException("MicroBenchmark parameters does not match the signature contract, see the " + MicroBenchmark.class + " documentation");
                }
            }
        }
        if (method == null) {
            throw new IllegalArgumentException("No matching methods found in benchmark");
        }
        return method;
    }

    public static MicroBenchmarkHandler getInstance(OutputFormat out, BenchmarkRecord microbenchmark, Class<?> clazz, Method method, MicroBenchmarkParameters executionParams, BaseOptions options) {
        return new LoopMicroBenchmarkHandler(out, microbenchmark, clazz, method, options, executionParams);
    }

    /**
     * checks if method signature is valid microbenchmark signature,
     * besited checks if method signature corresponds to benchmark type.
     * @param m
     * @return
     */
    private static boolean isValidBenchmarkSignature(Method m) {
        if (m.getReturnType() != Result.class) {
            return false;
        }
        final Class<?>[] parameterTypes = m.getParameterTypes();

        if (parameterTypes.length != 1) {
            return false;
        }

        if (parameterTypes[0] != InfraControl.class) {
            return false;
        }

        return true;
    }

}
