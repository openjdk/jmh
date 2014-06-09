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

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Class metadata.
 */
public interface ClassInfo extends MetadataInfo {

    /**
     * @return fully qualified package name
     */
    String getPackageName();

    /**
     *
     * @return fully qualified class name
     */
    String getQualifiedName();

    /**
     * @return short class name
     */
    String getName();

    /**
     * @return reference to super-class metadata
     */
    ClassInfo getSuperClass();

    /**
     * @return reference to syntactically-enclosing class
     */
    ClassInfo getDeclaringClass();

    /**
     * @return collection of all fields in class
     */
    Collection<FieldInfo> getFields();

    /**
     * @return collection of all methods in class
     */
    Collection<MethodInfo> getMethods();

    /**
     * @return collection of all constructors in class
     */
    Collection<MethodInfo> getConstructors();

    /**
     * @param annClass annotation class
     * @param <T> annotation type
     * @return class-level annotation, if any; null otherwise
     */
    <T extends Annotation> T getAnnotation(Class<T> annClass);

    /**
     * @return true, if class is abstract
     */
    boolean isAbstract();

    /**
     * @return true, if class is abstract
     */
    boolean isPublic();

    /**
     * @return true, if class is strictfp
     */
    boolean isStrictFP();

    /**
     * @return true, if class is final
     */
    boolean isFinal();

    /**
     * @return true, if class is inner
     */
    boolean isInner();

    /**
     * @return true, if class is enum
     */
    boolean isEnum();

    /**
     * @return if class is enum, the collection of its constant values;
     * empty collection otherwise
     */
    Collection<String> getEnumConstants();
}

