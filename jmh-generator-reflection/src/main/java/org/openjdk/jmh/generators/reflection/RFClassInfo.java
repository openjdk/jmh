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
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

class RFClassInfo implements ClassInfo {
    private final Class<?> klass;

    public RFClassInfo(Class<?> klass) {
        this.klass = klass;
    }

    @Override
    public String getPackageName() {
        if (klass.getDeclaringClass() != null) {
            return klass.getDeclaringClass().getPackage().getName();
        } else {
            return klass.getPackage().getName();
        }
    }

    @Override
    public String getName() {
        String name = klass.getSimpleName();
        if (name.contains("$")) {
            return name.substring(name.lastIndexOf("$"));
        } else {
            return name;
        }
    }

    @Override
    public String getQualifiedName() {
        String name = klass.getCanonicalName();
        if (name == null) {
            name = klass.getName();
        }
        if (name.contains("$")) {
            return name.replace("$", ".");
        } else {
            return name;
        }
    }

    @Override
    public Collection<FieldInfo> getFields() {
        Collection<FieldInfo> fis = new ArrayList<>();
        for (Field f : klass.getDeclaredFields()) {
            fis.add(new RFFieldInfo(this, f));
        }
        return fis;
    }

    @Override
    public Collection<MethodInfo> getConstructors() {
        Collection<MethodInfo> mis = new ArrayList<>();
        for (Constructor m : klass.getDeclaredConstructors()) {
            mis.add(new RFConstructorInfo(this, m));
        }
        return mis;
    }

    @Override
    public Collection<MethodInfo> getMethods() {
        Collection<MethodInfo> mis = new ArrayList<>();
        for (Method m : klass.getDeclaredMethods()) {
            mis.add(new RFMethodInfo(this, m));
        }
        return mis;
    }

    @Override
    public ClassInfo getSuperClass() {
        if (klass.getSuperclass() != null) {
            return new RFClassInfo(klass.getSuperclass());
        } else {
            return null;
        }
    }

    @Override
    public ClassInfo getDeclaringClass() {
        if (klass.getDeclaringClass() != null) {
            return new RFClassInfo(klass.getDeclaringClass());
        } else {
            return null;
        }
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

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(klass.getModifiers());
    }

    @Override
    public boolean isInner() {
        // LOL, Reflection: http://mail.openjdk.java.net/pipermail/core-libs-dev/2014-February/025246.html
        return klass.isAnonymousClass() ||
               klass.isLocalClass() ||
                       (klass.isMemberClass() && !Modifier.isStatic(klass.getModifiers()));
    }

    @Override
    public boolean isEnum() {
        return klass.isEnum();
    }

    @Override
    public Collection<String> getEnumConstants() {
        Collection<String> res = new ArrayList<>();
        for (Object cnst : klass.getEnumConstants()) {
            res.add(cnst.toString());
        }
        return res;
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }
}
