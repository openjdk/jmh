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

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class BenchmarkGeneratorUtils {

    private static final Collection<Class<? extends Annotation>> JMH_ANNOTATIONS;

    private static final Multimap<Class<? extends Annotation>, ElementType> JMH_ANNOTATION_TARGETS;

    static {
        JMH_ANNOTATIONS = Arrays.asList(
                AuxCounters.class, BenchmarkMode.class, CompilerControl.class, Fork.class,
                GenerateMicroBenchmark.class, Group.class, GroupThreads.class, Measurement.class,
                OperationsPerInvocation.class, OutputTimeUnit.class, Param.class, Setup.class,
                State.class, TearDown.class, Threads.class, Warmup.class
        );

        JMH_ANNOTATION_TARGETS = new HashMultimap<Class<? extends Annotation>, ElementType>();

        for (Class<? extends Annotation> ann : JMH_ANNOTATIONS) {
            Target target = ann.getAnnotation(Target.class);
            if (target != null) {
                ElementType[] types = target.value();
                for (ElementType type : types) {
                    JMH_ANNOTATION_TARGETS.put(ann, type);
                }
            }
        }
    }

    public static boolean checkJavaIdentifier(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

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

    public static String getNestedNames(ClassInfo ci) {
        String name = "";
        do {
            name = ci.getName() + (name.isEmpty() ? "" : "$" + name);
        } while ((ci = ci.getDeclaringClass()) != null);
        return name;
    }

    public static void checkAnnotations(FieldInfo fi) {
        for (Class<? extends Annotation> ann : JMH_ANNOTATIONS) {
            if (fi.getAnnotation(ann) != null && !JMH_ANNOTATION_TARGETS.get(ann).contains(ElementType.FIELD)) {
                throw new GenerationException(
                        "Annotation @" + ann.getSimpleName() + " is not applicable to fields.", fi);
            }
        }
    }

    public static void checkAnnotations(ClassInfo ci) {
        for (Class<? extends Annotation> ann : JMH_ANNOTATIONS) {
            if (ci.getAnnotation(ann) != null && !JMH_ANNOTATION_TARGETS.get(ann).contains(ElementType.TYPE)) {
                throw new GenerationException(
                        "Annotation @" + ann.getSimpleName() + " is not applicable to types.", ci);
            }
        }
    }

    public static void checkAnnotations(MethodInfo mi) {
        for (Class<? extends Annotation> ann : JMH_ANNOTATIONS) {
            if (mi.getAnnotation(ann) != null && !JMH_ANNOTATION_TARGETS.get(ann).contains(ElementType.METHOD)) {
                throw new GenerationException(
                        "Annotation @" + ann.getSimpleName() + " is not applicable to methods.", mi);
            }
        }
    }

    public static boolean isEnum(ClassInfo type) {
        if (type.getSuperClass() == null) {
            return false;
        } else {
            return type.getSuperClass().getQualifiedName().equals("java.lang.Enum");
        }
    }
}
