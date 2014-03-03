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
package org.openjdk.jmh.generators.source;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * Generator source.
 *
 * This is an entry point towards {@link org.openjdk.jmh.generators.core.BenchmarkGenerator}
 * interface.
 */
public interface GeneratorSource {

    /**
     * @return collection of all resolved classes
     */
    Collection<ClassInfo> getClasses();

    /**
     * Resolve class info for a name.
     * <p/>
     * Users may call this method for the classes not
     * listed in {@link #getClasses()} call, the implementation
     * has to have the fall-back strategy for these cases.
     *
     * @param className class name
     * @return class metainfo
     */
    ClassInfo resolveClass(String className);

    /**
     * Returns the Writer for the given resource.
     * Callers are responsible for closing Writers.
     *
     * @param resourcePath resource path
     * @return writer usable to write the resource
     * @throws IOException if something wacked happens
     */
    Writer newResource(String resourcePath) throws IOException;

    /**
     * Returns the Writer for the given class.
     * Callers are responsible for closing Writers.
     *
     * @param className class name
     * @return writer usable to write the resource
     * @throws IOException if something wacked happens
     */
    Writer newClass(String className) throws IOException;

    /**
     * Print the error.
     * Calling this method should not terminate anything.
     *
     * @param message error.
     */
    void printError(String message);

    /**
     * Print the error.
     * Calling this method should not terminate anything.
     *
     * @param message error.
     * @param element metadata element, to which this error is tailored
     */
    void printError(String message, MetadataInfo element);

    /**
     * Print the error.
     * Calling this method should not terminate anything.
     *
     * @param message error.
     * @param throwable exception causing the error
     */
    void printError(String message, Throwable throwable);
}
