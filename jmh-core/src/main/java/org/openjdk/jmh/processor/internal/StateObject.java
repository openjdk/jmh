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

import javax.lang.model.element.VariableElement;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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
    public final Map<String, String> params;

    public StateObject(String userType, String jmhType, Scope scope, String fieldIdentifier, String localIdentifier) {
        this.userType = userType;
        this.type = jmhType;
        this.scope = scope;
        this.localIdentifier = localIdentifier;
        this.fieldIdentifier = fieldIdentifier;
        this.params = new TreeMap<String, String>();
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

    public String getParamAccessor(String name) {
        return params.get(name);
    }

    public void addParam(VariableElement ve) {
        String type = ve.asType().toString();
        String name = ve.getSimpleName().toString();
        if (type.equalsIgnoreCase("java.lang.String")) {
            params.put(name, "control.getParam(\"" + name + "\")");
            return;
        }
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("java.lang.Boolean")) {
            params.put(name, "Boolean.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("byte") || type.equalsIgnoreCase("java.lang.Byte")) {
            params.put(name, "Byte.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("char") || type.equalsIgnoreCase("java.lang.Character")) {
            params.put(name, "(control.getParam(\"" + name + "\")).charAt(0)");
            return;
        }
        if (type.equalsIgnoreCase("short") || type.equalsIgnoreCase("java.lang.Short")) {
            params.put(name, "Short.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("java.lang.Integer")) {
            params.put(name, "Integer.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("float") || type.equalsIgnoreCase("java.lang.Float")) {
            params.put(name, "Float.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("long") || type.equalsIgnoreCase("java.lang.Long")) {
            params.put(name, "Long.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("java.lang.Double")) {
            params.put(name, "Double.valueOf(control.getParam(\"" + name + "\"))");
            return;
        }
        throw new IllegalStateException("Unknown type: " + type);
    }

    public Collection<String> getParamsLabels() {
        return params.keySet();
    }
}
