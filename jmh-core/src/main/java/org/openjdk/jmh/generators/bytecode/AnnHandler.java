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
package org.openjdk.jmh.generators.bytecode;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnnHandler extends AnnotationVisitor implements InvocationHandler {
    private final Map<String, Object> values;
    private final Multimap<String, String> valuesArray;

    public AnnHandler(AnnotationVisitor annotationVisitor) {
        super(Opcodes.ASM4, annotationVisitor);
        this.values = new HashMap<String, Object>();
        this.valuesArray = new HashMultimap<String, String>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String key = method.getName();
        Class<?> returnType = method.getReturnType();

        // FIXME: Better type handling

        if (returnType.isArray()) {
            String[] strings = valuesArray.get(key).toArray(new String[0]);
            if (strings.length == 0) {
                strings = (String[]) method.getDefaultValue();
            }

            return strings;
        } else {
            Object value = values.get(key);
            if (value == null) {
                value = method.getDefaultValue();
            }

            if (returnType.isEnum() && (value instanceof String)) {
                Method m = returnType.getMethod("valueOf", String.class);
                return m.invoke(null, value);
            }
            return value;
        }
    }

    @Override
    public void visit(String name, Object value) {
        values.put(name, value);
        super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        values.put(name, value);
        super.visitEnum(name, desc, value);
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        return new AnnotationVisitor(Opcodes.ASM4, super.visitArray(name)) {
            @Override
            public void visitEnum(String n, String desc, String value) {
                valuesArray.put(name, value);
                super.visitEnum(n, desc, value);
            }

            @Override
            public void visit(String n, Object value) {
                valuesArray.put(name, (String)value);
                super.visit(n, value);
            }
        };
    }
}
