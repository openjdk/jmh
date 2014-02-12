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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

public class AnnUtils {

    /**
     * Recursively get the annotation, until found, or reached the top of hierarchy.
     * @param root where to start
     * @param annotation what to look for
     * @param <A> type of what we look for
     * @return annotation
     */
    public static <A extends Annotation> A getAnnotationRecursive(Element root, Class<A> annotation) {
        Element walk = root;
        A result = null;
        while (walk != null && (result = walk.getAnnotation(annotation)) == null) {
            walk = walk.getEnclosingElement();
        }
        return result;
    }

    /**
     * Get the package name part of a class
     *
     * @param clazz the subject
     * @return the package name or "" if no package
     */
    public static String getPackageName(TypeElement clazz) {
        Element walk = clazz;
        while (walk.getKind() != ElementKind.PACKAGE) {
            walk = walk.getEnclosingElement();
        }
        return ((PackageElement)walk).getQualifiedName().toString();
    }

    /**
     * Get the class name along with any nested class names
     * @param clazz the subject
     * @return the synthetic class name in form of "parent1_parent2_classname"
     */
    public static String getNestedName(TypeElement clazz) {
        String name = "";
        Element walk = clazz;
        while (walk.getKind() != ElementKind.PACKAGE) {
            name = walk.getSimpleName().toString() + (name.isEmpty() ? "" : "_" + name);
            walk = walk.getEnclosingElement();
        }
        return name.substring(0, name.length());
    }
}
