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

import java.util.Map;

public class BenchmarkInfo {
    public final String userName;
    public final String generatedName;
    public final String generatedPackageName;
    public final String generatedClassName;
    public final Map<String, MethodGroup> methodGroups;

    public BenchmarkInfo(String userName, String generatedPackageName, String generatedClassName, Map<String, MethodGroup> methods) {
        this.userName = userName;
        this.generatedPackageName = generatedPackageName;
        this.generatedClassName = generatedClassName;
        this.generatedName = generatedPackageName + "." + generatedClassName;
        this.methodGroups = methods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkInfo that = (BenchmarkInfo) o;

        if (!userName.equals(that.userName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }
}
