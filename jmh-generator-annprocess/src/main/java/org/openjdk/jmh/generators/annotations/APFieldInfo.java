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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;

class APFieldInfo extends APMetadataInfo implements FieldInfo {

    private final VariableElement ve;

    public APFieldInfo(ProcessingEnvironment processEnv, VariableElement ve) {
        super(processEnv, ve);
        if (ve == null) {
            throw new IllegalArgumentException("element is null");
        }
        this.ve = ve;
    }

    @Override
    public String getName() {
        return ve.getSimpleName().toString();
    }

    @Override
    public ClassInfo getType() {
        return new APClassInfo(processEnv, ve.asType());
    }

    @Override
    public boolean isPublic() {
        return ve.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isStatic() {
        return ve.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public boolean isFinal() {
        return ve.getModifiers().contains(Modifier.FINAL);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return ve.getAnnotation(annClass);
    }

    @Override
    public ClassInfo getDeclaringClass() {
        return new APClassInfo(processEnv, (TypeElement)ve.getEnclosingElement());
    }

    public String toString() {
        return getType() + " " + getName();
    }
}
