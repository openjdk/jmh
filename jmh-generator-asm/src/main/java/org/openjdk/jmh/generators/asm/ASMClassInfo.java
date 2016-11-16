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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ASMClassInfo extends ClassVisitor implements ClassInfo {

    private String idName;
    private String packageName;
    private String qualifiedName;
    private String name;
    private int access;

    private final List<MethodInfo> methods;
    private final List<MethodInfo> constructors;
    private final List<FieldInfo> fields;
    private final Map<String, AnnotationInvocationHandler> annotations = new HashMap<>();
    private final ClassInfoRepo classInfos;
    private String superName;
    private String declaringClass;
    private boolean isInner;
    private String origQualifiedName;

    public ASMClassInfo(ClassInfoRepo classInfos) {
        super(Opcodes.ASM4);
        this.classInfos = classInfos;
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

    public String getIdName() {
        return idName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.superName = superName;
        this.idName = name;
        this.access = access;
        qualifiedName = name.replace("/", ".");
        packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        origQualifiedName = qualifiedName;
        qualifiedName = qualifiedName.replace('$', '.');
        this.name = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
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
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        ClassInfo type = classInfos.get(Type.getReturnType(desc).getClassName());
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        ASMFieldInfo fi = new ASMFieldInfo(fv, this, access, name, type);
        fields.add(fi);
        return fi;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
        ASMMethodInfo mi = new ASMMethodInfo(super.visitMethod(access, methodName, methodDesc, signature, exceptions),
                classInfos, this, access, methodName, methodDesc, signature);
        if (methodName.equals("<init>")) {
            constructors.add(mi);
        } else {
            methods.add(mi);
        }
        return mi;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public Collection<FieldInfo> getFields() {
        return fields;
    }

    @Override
    public Collection<MethodInfo> getConstructors() {
        return constructors;
    }

    @Override
    public Collection<MethodInfo> getMethods() {
        return methods;
    }

    @Override
    public ClassInfo getSuperClass() {
        return classInfos.get(superName);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (name.equals(idName)) {
            declaringClass = outerName;
        }
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        isInner = true;
    }

    @Override
    public ClassInfo getDeclaringClass() {
        if (declaringClass != null) {
            return classInfos.get(declaringClass);
        } else {
            return null;
        }
    }

    @Override
    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) > 0;
    }

    @Override
    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) > 0;
    }

    @Override
    public boolean isStrictFP() {
        return (access & Opcodes.ACC_STRICT) > 0;
    }

    @Override
    public boolean isFinal() {
        return (access & Opcodes.ACC_FINAL) > 0;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    @Override
    public boolean isEnum() {
        return (access & Opcodes.ACC_ENUM) > 0;
    }

    @Override
    public Collection<String> getEnumConstants() {
        if (isEnum()) {
            try {
                Collection<String> res = new ArrayList<>();
                for (Object cnst : Class.forName(origQualifiedName, false, Thread.currentThread().getContextClassLoader()).getEnumConstants()) {
                    res.add(cnst.toString());
                }
                return res;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Can not find and instantiate enum: " + origQualifiedName);
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
