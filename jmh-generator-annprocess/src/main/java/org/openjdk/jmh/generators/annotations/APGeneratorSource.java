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
package org.openjdk.jmh.generators.annotations;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class APGeneratorSource implements GeneratorSource {

    private final RoundEnvironment roundEnv;
    private final ProcessingEnvironment processingEnv;
    private Collection<ClassInfo> classInfos;

    public APGeneratorSource(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        this.roundEnv = roundEnv;
        this.processingEnv = processingEnv;
    }

    @Override
    public Collection<ClassInfo> getClasses() {
        if (classInfos != null) {
            return classInfos;
        }

        Collection<TypeElement> discoveredClasses = new TreeSet<>(new Comparator<TypeElement>() {
            @Override
            public int compare(TypeElement o1, TypeElement o2) {
                return o1.getQualifiedName().toString().compareTo(o2.getQualifiedName().toString());
            }
        });

        // Need to do a few rollovers to find all classes that have @Benchmark-annotated methods in their
        // subclasses. This is mostly due to some of the nested classes not discoverable at once,
        // when we need to discover the enclosing class first. With the potentially non-zero nesting
        // depth, we need to do a few rounds. Hopefully we will just do a single stride in most
        // cases.

        for (Element e : roundEnv.getRootElements()) {
            if (e.getKind() != ElementKind.CLASS) continue;
            discoveredClasses.add((TypeElement) e);
        }

        int lastSize = 0;
        while (discoveredClasses.size() > lastSize) {
            lastSize = discoveredClasses.size();
            List<TypeElement> newClasses = new ArrayList<>();
            for (Element e : discoveredClasses) {
                try {
                    TypeElement walk = (TypeElement) e;
                    do {
                        for (TypeElement nested : ElementFilter.typesIn(walk.getEnclosedElements())) {
                            newClasses.add(nested);
                        }
                    }
                    while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
                } catch (Exception t) {
                    // Working around the javac bug:
                    //   https://bugs.openjdk.java.net/browse/JDK-8071778
                    //
                    // JMH ignores these exceptions since they probably consider the classes that do not
                    // have any JMH-related annotations. We can do nothing better than to notify the user,
                    // and bail from traversing a current class.
                    if (t.getClass().getName().endsWith("CompletionFailure")) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "While traversing " + e + ", caught " + t);
                    } else {
                        throw new RuntimeException(t);
                    }
                }
            }
            discoveredClasses.addAll(newClasses);
        }

        classInfos = convert(discoveredClasses);
        return classInfos;
    }

    protected Collection<ClassInfo> convert(Collection<TypeElement> els) {
        List<ClassInfo> list = new ArrayList<>();
        for (TypeElement el : els) {
            list.add(new APClassInfo(processingEnv, el));
        }
        return list;
    }

    @Override
    public ClassInfo resolveClass(String className) {
        return new APClassInfo(processingEnv, processingEnv.getElementUtils().getTypeElement(className));
    }

}
