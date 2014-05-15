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
package org.openjdk.jmh.generators.annotations;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class APClassInfo extends APMetadataInfo implements ClassInfo {

    private final TypeElement el;
    private final boolean isPrimitive;
    private final TypeMirror mirror;

    public APClassInfo(ProcessingEnvironment processEnv, TypeElement element) {
        super(processEnv, element);
        if (element == null) {
            throw new IllegalArgumentException("element is null");
        }
        this.el = element;
        this.isPrimitive = false;
        this.mirror = null;
    }

    public APClassInfo(ProcessingEnvironment processEnv, TypeMirror mirror) {
        super(processEnv, null);
        this.mirror = mirror;
        this.isPrimitive = mirror.getKind().isPrimitive();
        if (!isPrimitive) {
            this.el = (TypeElement) processEnv.getTypeUtils().asElement(mirror);
        } else {
            this.el = null;
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        if (isPrimitive) return null;
        return el.getAnnotation(annClass);
    }

    @Override
    public Collection<MethodInfo> getConstructors() {
        if (isPrimitive) return Collections.emptyList();
        Collection<MethodInfo> mis = new ArrayList<MethodInfo>();
        for (ExecutableElement e : ElementFilter.constructorsIn(el.getEnclosedElements())) {
            mis.add(new APMethodInfo(processEnv, this, e));
        }
        return mis;
    }

    @Override
    public String getName() {
        if (isPrimitive) return mirror.toString();
        return el.getSimpleName().toString();
    }

    @Override
    public String getQualifiedName() {
        if (isPrimitive) return mirror.toString();
        return el.getQualifiedName().toString();
    }

    @Override
    public Collection<FieldInfo> getFields() {
        if (isPrimitive) return Collections.emptyList();
        List<FieldInfo> ls = new ArrayList<FieldInfo>();
        for (VariableElement e : ElementFilter.fieldsIn(el.getEnclosedElements())) {
            ls.add(new APFieldInfo(processEnv, e));
        }
        return ls;
    }

    @Override
    public Collection<MethodInfo> getMethods() {
        if (isPrimitive) return Collections.emptyList();
        Collection<MethodInfo> mis = new ArrayList<MethodInfo>();
        for (ExecutableElement e : ElementFilter.methodsIn(el.getEnclosedElements())) {
            mis.add(new APMethodInfo(processEnv, this, e));
        }
        return mis;
    }

    @Override
    public String getPackageName() {
        if (isPrimitive) return "";
        Element walk = el;
        while (walk.getKind() != ElementKind.PACKAGE) {
            walk = walk.getEnclosingElement();
        }
        return ((PackageElement)walk).getQualifiedName().toString();
    }

    @Override
    public ClassInfo getSuperClass() {
        if (isPrimitive) return null;
        TypeMirror superclass = el.getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            return null;
        } else {
            TypeElement element = (TypeElement) processEnv.getTypeUtils().asElement(superclass);
            return new APClassInfo(processEnv, element);
        }
    }

    @Override
    public ClassInfo getDeclaringClass() {
        if (isPrimitive) return null;
        Element enclosingElement = el.getEnclosingElement();
        if (enclosingElement.getKind() == ElementKind.CLASS) {
            return new APClassInfo(processEnv, (TypeElement) enclosingElement);
        } else {
            return null;
        }
    }

    @Override
    public boolean isAbstract() {
        if (isPrimitive) return false;
        return el.getModifiers().contains(Modifier.ABSTRACT);
    }

    @Override
    public boolean isPublic() {
        if (isPrimitive) return true;
        return el.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isStrictFP() {
        if (isPrimitive) return false;
        return el.getModifiers().contains(Modifier.STRICTFP);
    }

    @Override
    public boolean isFinal() {
        if (isPrimitive) return false;
        return el.getModifiers().contains(Modifier.FINAL);
    }

    @Override
    public boolean isInner() {
        if (isPrimitive) return false;
        return (getDeclaringClass() != null) && !el.getModifiers().contains(Modifier.STATIC);
    }

    public String toString() {
        return getQualifiedName();
    }

}
