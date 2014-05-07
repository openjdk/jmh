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
 * Method info.
 */
public interface MethodInfo extends Comparable<MethodInfo>, MetadataInfo {

    /**
     * @return short method name.
     */
    String getName();

    /**
     * @return fully qualified method name, includes class qualified name
     */
    String getQualifiedName();

    /**
     * @return fully qualified return type
     */
    String getReturnType();

    /**
     * @return collection of method parameters.
     */
    Collection<ParameterInfo> getParameters();

    /**
     * @return reference to syntactically-enclosing class
     */
    ClassInfo getDeclaringClass();

    /**
     * @param annClass annotation class
     * @param <T> annotation type
     * @return method-level annotation, if any; null otherwise
     */
    <T extends Annotation> T getAnnotation(Class<T> annClass);

    /**
     * @return true, if method is public
     */
    boolean isPublic();

    /**
     * @return true, if method is abstract
     */
    boolean isAbstract();

    /**
     * @return true, if method is synchronized
     */
    boolean isSynchronized();

    /**
     * @return true, if method is strictfp
     */
    boolean isStrictFP();

    /**
     * @return true, if method is static
     */
    boolean isStatic();
}
