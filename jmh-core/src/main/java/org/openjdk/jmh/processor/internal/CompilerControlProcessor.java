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

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.runner.CompilerHints;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CompilerControlProcessor implements SubProcessor {

    private final List<String> lines = new ArrayList<String>();
    private final Set<Element> defaultForceInline = new TreeSet<Element>(new Comparator<Element>() {
        @Override
        public int compare(Element o1, Element o2) {
            return getName(o1).compareTo(getName(o2));
        }
    });

    public void defaultForceInline(Element element) {
        defaultForceInline.add(element);
    }

    public void process(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(CompilerControl.class)) {
                CompilerControl ann = element.getAnnotation(CompilerControl.class);
                if (ann == null) {
                    throw new IllegalStateException("No annotation");
                }

                CompilerControl.Mode command = ann.value();
                lines.add(command.command() + "," + getName(element));
            }

            for (Element element : defaultForceInline) {
                if (element.getAnnotation(CompilerControl.class) != null) continue;
                lines.add(CompilerControl.Mode.INLINE.command() + "," + getName(element));
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had thrown exception: " + t);
            t.printStackTrace(System.err);
        }
    }

    @Override
    public void finish(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    CompilerHints.LIST.substring(1));
            PrintWriter writer = new PrintWriter(file.openWriter());
            for (String line : lines) {
                writer.println(line);
            }
            writer.close();
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Error writing compiler hint list " + ex);
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had thrown exception: " + t);
            t.printStackTrace(System.err);
        }
    }

    private static String getName(Element element) {
        switch (element.getKind()) {
            case CLASS:
                return ((TypeElement)element).getQualifiedName().toString().replaceAll("\\.", "/") + ".*";
            case METHOD:
                return ((TypeElement)element.getEnclosingElement()).getQualifiedName().toString().replaceAll("\\.", "/") + "." + element.getSimpleName().toString();
            default:
                throw new GenerationException("@" + CompilerControl.class.getSimpleName() + " annotation is placed within " +
                                "unexpected target", element);
        }
    }

}
