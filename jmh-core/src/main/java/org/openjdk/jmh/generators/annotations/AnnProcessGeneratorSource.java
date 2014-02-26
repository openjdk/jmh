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
package org.openjdk.jmh.generators.annotations;

import org.openjdk.jmh.generators.source.ClassInfo;
import org.openjdk.jmh.generators.source.GeneratorSource;
import org.openjdk.jmh.generators.source.MetadataInfo;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class AnnProcessGeneratorSource implements GeneratorSource {

    private final RoundEnvironment roundEnv;
    private final ProcessingEnvironment processingEnv;

    public AnnProcessGeneratorSource(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        this.roundEnv = roundEnv;
        this.processingEnv = processingEnv;
    }

    @Override
    public Collection<ClassInfo> getClasses() {
        // Need to do a few rollovers to find all classes that have @GMB-annotated methods in their
        // subclasses. This is mostly due to some of the nested classes not discoverable at once,
        // when we need to discover the enclosing class first. With the potentially non-zero nesting
        // depth, we need to do a few rounds. Hopefully we will just do a single stride in most
        // cases.

        Collection<TypeElement> discoveredClasses = new TreeSet<TypeElement>(new Comparator<TypeElement>() {
            @Override
            public int compare(TypeElement o1, TypeElement o2) {
                return o1.getQualifiedName().toString().compareTo(o2.getQualifiedName().toString());
            }
        });

        // Walk around until convergence...

        int lastSize = -1;
        while (discoveredClasses.size() > lastSize) {
            lastSize = discoveredClasses.size();
            for (Element e : roundEnv.getRootElements()) {
                if (e.getKind() != ElementKind.CLASS) continue;
                TypeElement walk = (TypeElement) e;
                do {
                    discoveredClasses.add(walk);
                    for (TypeElement nested : ElementFilter.typesIn(walk.getEnclosedElements())) {
                        discoveredClasses.add(nested);
                    }
                } while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
            }
        }

        return convert(discoveredClasses);
    }

    protected Collection<ClassInfo> convert(Collection<TypeElement> els) {
        List<ClassInfo> list = new ArrayList<ClassInfo>();
        for (TypeElement el : els) {
            list.add(new APClassInfo(processingEnv, el));
        }
        return list;
    }

    @Override
    public ClassInfo resolveClass(String className) {
        return new APClassInfo(processingEnv, processingEnv.getElementUtils().getTypeElement(className));
    }

    @Override
    public Writer newResource(String path) throws IOException {
        return processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", path).openWriter();
    }

    @Override
    public Writer newFile(String objectName) throws IOException {
        return processingEnv.getFiler().createSourceFile(objectName).openWriter();
    }

    @Override
    public void printError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public void printError(String message, MetadataInfo element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, ((APMetadataInfo)element).getElement());
    }

    @Override
    public void printError(String message, Throwable throwable) {
        printError(message + " " + throwable);
    }

}
