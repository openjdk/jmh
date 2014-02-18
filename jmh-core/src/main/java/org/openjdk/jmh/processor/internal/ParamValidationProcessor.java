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

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.State;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

public class ParamValidationProcessor implements SubProcessor {

    @Override
    public void process(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(Param.class)) {
                if (element.getKind() != ElementKind.FIELD) {
                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + Param.class.getSimpleName() + " annotation is acceptable on fields only.",
                            element
                    );
                }

                if (!element.getModifiers().contains(Modifier.PUBLIC)) {
                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + Param.class.getSimpleName() + " annotation is not acceptable on non-public field.",
                            element
                    );
                }

                if (element.getModifiers().contains(Modifier.STATIC)) {
                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + Param.class.getSimpleName() + " annotation is not acceptable on static fields.",
                            element
                    );
                }

                if (AnnUtils.getAnnotationRecursive(element, State.class) == null) {
                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "@" + Param.class.getSimpleName() + " annotation should be placed in @" + State.class.getSimpleName() +
                                    "-annotated class.",
                            element
                    );
                }

                VariableElement ve = (VariableElement) element;
                String[] values = element.getAnnotation(Param.class).value();

                if (values.length >= 1 && !values[0].equalsIgnoreCase(Param.BLANK_ARGS)) {
                    String type = ve.asType().toString();
                    for (String val : values) {
                        if (!isConforming(val, type)) {
                            processingEnv.getMessager().printMessage(Kind.ERROR,
                                    "Some @" + Param.class.getSimpleName() + " values can not be converted to target type: " +
                                    "\"" + val + "\" can not be converted to " + type,
                                    element
                            );
                        }
                    }
                }

            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had throw exception: " + t);
            t.printStackTrace(System.err);
        }
    }

    private boolean isConforming(String val, String type) {
        if (type.equalsIgnoreCase("java.lang.String")) {
            return true;
        }
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("java.lang.Boolean")) {
            return (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"));
        }
        if (type.equalsIgnoreCase("byte") || type.equalsIgnoreCase("java.lang.Byte")) {
            try {
                Byte.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        if (type.equalsIgnoreCase("char") || type.equalsIgnoreCase("java.lang.Character")) {
            return (val.length() != 0);
        }
        if (type.equalsIgnoreCase("short") || type.equalsIgnoreCase("java.lang.Short")) {
            try {
                Short.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("java.lang.Integer")) {
            try {
                Integer.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        if (type.equalsIgnoreCase("float") || type.equalsIgnoreCase("java.lang.Float")) {
            try {
                Float.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        if (type.equalsIgnoreCase("long") || type.equalsIgnoreCase("java.lang.Long")) {
            try {
                Long.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("java.lang.Double")) {
            try {
                Double.valueOf(val);
                return true;
            } catch (NumberFormatException nfe){
            }
        }
        return false;
    }

    @Override
    public void finish(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        // do nothing
    }

}
