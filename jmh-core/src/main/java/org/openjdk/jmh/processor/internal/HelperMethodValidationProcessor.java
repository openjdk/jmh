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

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;

public class HelperMethodValidationProcessor implements SubProcessor {

    @Override
    public void process(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(Setup.class)) {
                // OK to have these annotation for @State objects
                if (element.getEnclosingElement().getAnnotation(State.class) != null) continue;

                // Abstract classes are not instantiated, assume OK
                if (element.getEnclosingElement().getModifiers().contains(Modifier.ABSTRACT)) continue;

                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + Setup.class.getSimpleName() + " annotation is placed within " +
                                    "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                    "This has no behavioral effect, and prohibited.",
                            element);
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(TearDown.class)) {
                // OK to have these annotation for @State objects
                if (element.getEnclosingElement().getAnnotation(State.class) != null) continue;

                // Abstract classes are not instantiated, assume OK
                if (element.getEnclosingElement().getModifiers().contains(Modifier.ABSTRACT)) continue;

                processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + TearDown.class.getSimpleName() + " annotation is placed within " +
                                    "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                    "This can be futile if no @State-bearing subclass is used.",
                            element);
            }

        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had throw exception: " + t);
            t.printStackTrace(System.err);
        }
    }

}
