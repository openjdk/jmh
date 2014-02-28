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
package org.openjdk.jmh.generators.reflective;

import org.openjdk.jmh.generators.source.ClassInfo;
import org.openjdk.jmh.generators.source.FieldInfo;
import org.openjdk.jmh.generators.source.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReflectiveClassInfo implements ClassInfo {
    private final Class<?> klass;

    public ReflectiveClassInfo(Class<?> klass) {
        this.klass = klass;
    }

    @Override
    public String getPackageName() {
        return klass.getPackage().getName();
    }

    @Override
    public String getNestedName() {
        // FIXME
        return getQualifiedName();
    }

    @Override
    public String getQualifiedName() {
        return klass.getCanonicalName();
    }

    @Override
    public Collection<FieldInfo> getFields() {
        // FIXME
        return Collections.emptyList();
    }

    @Override
    public Collection<MethodInfo> getConstructors() {
        // FIXME
        return Collections.emptyList();
    }

    @Override
    public Collection<MethodInfo> getMethods() {
        // FIXME
        return Collections.emptyList();
    }

    @Override
    public ClassInfo getSuperClass() {
        // FIXME
        return null;
    }

    @Override
    public ClassInfo getEnclosingClass() {
        // FIXME
        return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return klass.getAnnotation(annClass);
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(klass.getModifiers());
    }

    @Override
    public boolean isPublic() {
        return Modifier.isPublic(klass.getModifiers());
    }

    @Override
    public boolean isStrictFP() {
        return Modifier.isStrict(klass.getModifiers());
    }
}
