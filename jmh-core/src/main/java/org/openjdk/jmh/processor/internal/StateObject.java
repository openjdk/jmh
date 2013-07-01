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
package org.openjdk.jmh.processor.internal;

import org.openjdk.jmh.annotations.Scope;

import java.util.Comparator;

public class StateObject {

    public static final Comparator<StateObject> ID_COMPARATOR = new Comparator<StateObject>() {
        @Override
        public int compare(StateObject o1, StateObject o2) {
            return o1.fieldIdentifier.compareTo(o2.fieldIdentifier);
        }
    };

    public final String userType;
    public final String type;
    public final Scope scope;
    public final String localIdentifier;
    public final String fieldIdentifier;

    public StateObject(String userType, String jmhType, Scope scope, String fieldIdentifier, String localIdentifier) {
        this.userType = userType;
        this.type = jmhType;
        this.scope = scope;
        this.localIdentifier = localIdentifier;
        this.fieldIdentifier = fieldIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StateObject that = (StateObject) o;

        if (fieldIdentifier != null ? !fieldIdentifier.equals(that.fieldIdentifier) : that.fieldIdentifier != null)
            return false;
        if (scope != that.scope) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (fieldIdentifier != null ? fieldIdentifier.hashCode() : 0);
        return result;
    }

    public String toTypeDef() {
        return type + " " + localIdentifier;
    }

    public String toLocal() {
        return localIdentifier;
    }

}
