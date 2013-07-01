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

import org.openjdk.jmh.annotations.MicroBenchmark;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** @author staffan.friberg@oracle.com */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MicroBenchmarkProcessor extends AbstractProcessor {

    public static final Set<String> COLLECTED_MICROBENCHMARKS = new HashSet<String>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(MicroBenchmark.class.getName());
    }

    /**
     * Process all method annotated with MicroBenchmark and add them to the list of available microbenchmarks.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                // Still processing add all annotated methods to the set
                for (TypeElement annotation : annotations) {
                    for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                        processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING,
                                "The " + MicroBenchmark.class.getSimpleName()
                                        + " is detected. This is not the supported API anymore. \n"
                                        + element.getEnclosingElement() + '.' + element.toString());
                        TypeElement klass = (TypeElement) element.getEnclosingElement();
                        COLLECTED_MICROBENCHMARKS.add(klass.getQualifiedName().toString() + "." + element.getSimpleName().toString());
                    }
                }
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had throw exception: " + t);
            t.printStackTrace(System.err);
        }
        return true;
    }

}
