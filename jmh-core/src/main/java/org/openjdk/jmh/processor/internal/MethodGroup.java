/**
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
package org.openjdk.jmh.processor.internal;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class MethodGroup implements Comparable<MethodGroup> {
    private final String name;
    private final Set<MethodInvocation> methods;
    private boolean strictFP;

    MethodGroup(String name) {
        this.name = name;
        this.methods = new LinkedHashSet<MethodInvocation>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodGroup methodGroup = (MethodGroup) o;

        if (!name.equals(methodGroup.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(MethodGroup o) {
        return name.compareTo(o.name);
    }

    public void addMethod(Element method, int threads) {
        methods.add(new MethodInvocation(method, threads));
    }

    public Collection<Element> methods() {
        Collection<Element> result = new ArrayList<Element>();
        for (MethodInvocation m : methods) {
            result.add(m.element);
        }
        return result;
    }

    public int getTotalThreadCount() {
        int threadCount = 0;
        for (MethodInvocation m : methods) {
            threadCount += m.threads;
        }
        return threadCount;
    }

    public int getMethodThreads(Element method) {
        for (MethodInvocation m : methods) {
            if (m.element.equals(method)) {
                return m.threads;
            }
        }
        throw new IllegalStateException("");
    }

    public String getName() {
        return name;
    }

    public void addStrictFP(boolean sfp) {
        strictFP |= sfp;
    }

    public boolean isStrictFP() {
        return strictFP;
    }
}
