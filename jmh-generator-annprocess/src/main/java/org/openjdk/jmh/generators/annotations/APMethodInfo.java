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
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

class APMethodInfo extends APMetadataInfo implements MethodInfo {

    private final ClassInfo ci;
    private final ExecutableElement el;

    public APMethodInfo(ProcessingEnvironment processEnv, ClassInfo ci, ExecutableElement el) {
        super(processEnv, el);
        if (ci == null) {
            throw new IllegalArgumentException("ci is null");
        }
        if (el == null) {
            throw new IllegalArgumentException("el is null");
        }
        this.ci = ci;
        this.el = el;
    }

    @Override
    public ClassInfo getDeclaringClass() {
        return ci;
    }

    @Override
    public String getName() {
        return el.getSimpleName().toString();
    }

    @Override
    public String getReturnType() {
        return el.getReturnType().toString();
    }

    @Override
    public Collection<ParameterInfo> getParameters() {
        Collection<ParameterInfo> pis = new ArrayList<>();
        for (VariableElement v : el.getParameters()) {
            pis.add(new APParameterInfo(processEnv, v));
        }
        return pis;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return el.getAnnotation(annClass);
    }

    @Override
    public boolean isPublic() {
        return el.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isAbstract() {
        return el.getModifiers().contains(Modifier.ABSTRACT);
    }

    @Override
    public boolean isSynchronized() {
        return el.getModifiers().contains(Modifier.SYNCHRONIZED);
    }

    @Override
    public boolean isStrictFP() {
        return el.getModifiers().contains(Modifier.STRICTFP);
    }

    @Override
    public boolean isStatic() {
        return el.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public String getQualifiedName() {
        return ci.getQualifiedName() + "." + el.toString();
    }

    @Override
    public int compareTo(MethodInfo o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }

    public String toString() {
        return getDeclaringClass() + " " + getName() ;
    }

}
