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

import org.openjdk.jmh.annotations.Level;

class HelperMethodInvocation implements Comparable<HelperMethodInvocation> {
    public final MethodInfo method;
    public final StateObject state;
    public final Level helperLevel;
    public final HelperType type;

    public HelperMethodInvocation(MethodInfo method, StateObject state, Level helperLevel, HelperType type) {
        this.method = method;
        this.state = state;
        this.helperLevel = helperLevel;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HelperMethodInvocation that = (HelperMethodInvocation) o;

        if (helperLevel != that.helperLevel) return false;
        if (!method.equals(that.method)) return false;
        if (!state.equals(that.state)) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + helperLevel.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public int compareTo(HelperMethodInvocation o) {
        return method.compareTo(o.method);
    }
}
