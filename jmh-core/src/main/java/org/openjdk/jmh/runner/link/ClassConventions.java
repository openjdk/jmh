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
package org.openjdk.jmh.runner.link;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class ClassConventions {

    private static final Map<Method, String> METHOD_NAMES = new HashMap<>();

    public static String getMethodName(Method m) {
        String result = METHOD_NAMES.get(m);
        if (result == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(m.getName());
            for (Class<?> paramType : m.getParameterTypes()) {
                builder.append(paramType.getName());
                builder.append(",");
            }
            result = builder.toString();
            METHOD_NAMES.put(m, result);
        }
        return result;
    }

}
