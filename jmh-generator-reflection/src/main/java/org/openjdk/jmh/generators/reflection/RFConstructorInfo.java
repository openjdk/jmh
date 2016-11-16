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
package org.openjdk.jmh.generators.reflection;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

class RFConstructorInfo implements MethodInfo {

    private final RFClassInfo declaringClass;
    private final Constructor m;

    public RFConstructorInfo(RFClassInfo declaringClass, Constructor m) {
        this.declaringClass = declaringClass;
        this.m = m;
    }

    @Override
    public ClassInfo getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return m.getName();
    }

    @Override
    public String getQualifiedName() {
        return declaringClass.getQualifiedName() + "." + m.getName();
    }

    @Override
    public String getReturnType() {
        throw new IllegalStateException("Asking the return type for constructor");
    }

    @Override
    public Collection<ParameterInfo> getParameters() {
        Collection<ParameterInfo> pis = new ArrayList<>();
        for (Class<?> cl : m.getParameterTypes()) {
            pis.add(new RFParameterInfo(cl));
        }
        return pis;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        throw new IllegalStateException("Asking annotations for constructor");
    }

    @Override
    public boolean isPublic() {
        return Modifier.isPublic(m.getModifiers());
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(m.getModifiers());
    }

    @Override
    public boolean isSynchronized() {
        return Modifier.isSynchronized(m.getModifiers());
    }

    @Override
    public boolean isStrictFP() {
        return Modifier.isStrict(m.getModifiers());
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(m.getModifiers());
    }

    @Override
    public int compareTo(MethodInfo o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }
}
