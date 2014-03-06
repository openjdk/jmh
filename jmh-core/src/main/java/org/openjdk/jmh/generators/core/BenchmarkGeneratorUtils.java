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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BenchmarkGeneratorUtils {

    public static <T extends Annotation> Collection<MethodInfo> getMethodsAnnotatedWith(GeneratorSource source, Class<T> annClass) {
        List<MethodInfo> mis = new ArrayList<MethodInfo>();
        for (ClassInfo ci : source.getClasses()) {
            for (MethodInfo mi : ci.getMethods()) {
                if (mi.getAnnotation(annClass) != null) {
                    mis.add(mi);
                }
            }
        }
        return mis;
    }

    public static <T extends Annotation> Collection<ClassInfo> getClassesAnnotatedWith(GeneratorSource source, Class<T> annClass) {
        List<ClassInfo> cis = new ArrayList<ClassInfo>();
        for (ClassInfo ci : source.getClasses()) {
            if (ci.getAnnotation(annClass) != null) {
                cis.add(ci);
            }
        }
        return cis;
    }

    public static <T extends Annotation> Collection<FieldInfo> getFieldsAnnotatedWith(GeneratorSource source, Class<T> annClass) {
        List<FieldInfo> mis = new ArrayList<FieldInfo>();
        for (ClassInfo ci : source.getClasses()) {
            for (FieldInfo mi : ci.getFields()) {
                if (mi.getAnnotation(annClass) != null) {
                    mis.add(mi);
                }
            }
        }
        return mis;
    }

    public static Collection<FieldInfo> getAllFields(ClassInfo ci) {
        List<FieldInfo> ls = new ArrayList<FieldInfo>();
        do {
            ls.addAll(ci.getFields());
        } while ((ci = ci.getSuperClass()) != null);
        return ls;
    }

    public static Collection<MethodInfo> getMethods(ClassInfo ci) {
        List<MethodInfo> ls = new ArrayList<MethodInfo>();
        do {
            ls.addAll(ci.getMethods());
        } while ((ci = ci.getSuperClass()) != null);
        return ls;
    }

    public static <T extends Annotation> T getAnnSuper(ClassInfo ci, Class<T> annClass) {
        T ann = ci.getAnnotation(annClass);
        if (ann != null) {
            return ann;
        } else {
            ClassInfo eci = ci.getSuperClass();
            if (eci != null) {
                return getAnnSuper(eci, annClass);
            }
        }
        return null;
    }

    public static <T extends Annotation> T getAnnSyntax(ClassInfo ci, Class<T> annClass) {
        T ann = ci.getAnnotation(annClass);
        if (ann != null) {
            return ann;
        } else {
            ClassInfo eci = ci.getDeclaringClass();
            if (eci != null) {
                return getAnnSyntax(eci, annClass);
            }
        }
        return null;
    }

    public static <T extends Annotation> T getAnnSyntax(MethodInfo mi, Class<T> annClass) {
        T ann = mi.getAnnotation(annClass);
        if (ann != null) {
            return ann;
        } else {
            return getAnnSyntax(mi.getDeclaringClass(), annClass);
        }
    }

    public static String getGeneratedName(ClassInfo ci) {
        String name = "";
        do {
            name = ci.getName() + (name.isEmpty() ? "" : "_" + name);
        } while ((ci = ci.getDeclaringClass()) != null);
        return name;
    }

}
