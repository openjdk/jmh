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
package org.openjdk.jmh.generators.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

class ASMFieldInfo extends FieldVisitor implements FieldInfo {

    private final ClassInfo type;
    private final ASMClassInfo declaringClass;
    private final int access;
    private final String name;
    private final Map<String, AnnotationInvocationHandler> annotations;

    public ASMFieldInfo(FieldVisitor fieldVisitor, ASMClassInfo declaringClass, int access, String name, ClassInfo type) {
        super(Opcodes.ASM4, fieldVisitor);
        this.declaringClass = declaringClass;
        this.access = access;
        this.name = name;
        this.type = type;
        this.annotations = new HashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ClassInfo getType() {
        return type;
    }

    @Override
    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) > 0;
    }

    @Override
    public boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) > 0;
    }

    @Override
    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) > 0;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        AnnotationInvocationHandler handler = annotations.get(annClass.getCanonicalName());
        if (handler == null) {
            return null;
        } else {
            return (T) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[]{annClass},
                    handler);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
        String className = Type.getType(desc).getClassName();
        AnnotationInvocationHandler annHandler = new AnnotationInvocationHandler(className, super.visitAnnotation(desc, visible));
        annotations.put(className, annHandler);
        return annHandler;
    }

    @Override
    public ClassInfo getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String toString() {
        return declaringClass.getQualifiedName() + "." + name;
    }
}
