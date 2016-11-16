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
package org.openjdk.jmh.generators.reflection;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class RFGeneratorSource implements GeneratorSource {

    private final Collection<Class> classes;

    public RFGeneratorSource() {
        this.classes = new ArrayList<>();
    }

    @Override
    public Collection<ClassInfo> getClasses() {
        Collection<ClassInfo> cis = new ArrayList<>();
        for (Class c : classes) {
            cis.add(new RFClassInfo(c));
        }
        return cis;
    }

    public static ClassInfo resolveClass(Class<?> klass) {
        return new RFClassInfo(klass);
    }

    @Override
    public ClassInfo resolveClass(String className) {
        String desc = className.replace('/', '.');
        try {
            return resolveClass(Class.forName(desc, false, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve class: " + desc);
        }
    }

    public void processClasses(Class... cs) {
        processClasses(Arrays.asList(cs));
    }

    public void processClasses(Collection<Class> cs) {
        classes.addAll(cs);
    }
}
