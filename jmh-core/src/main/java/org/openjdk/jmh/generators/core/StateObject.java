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
package org.openjdk.jmh.generators.core;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;

import java.util.*;

class StateObject {

    public static final Comparator<StateObject> ID_COMPARATOR = new Comparator<StateObject>() {
        @Override
        public int compare(StateObject o1, StateObject o2) {
            return o1.fieldIdentifier.compareTo(o2.fieldIdentifier);
        }
    };

    public final String packageName;
    public final String userType;
    public final String type;
    public final Scope scope;
    public final String localIdentifier;
    public final String fieldIdentifier;
    public final Multimap<String, FieldInfo> params;
    public final SortedSet<HelperMethodInvocation> helpers;
    public final Multimap<String, String> helperArgs;
    public final List<StateObject> depends;

    public StateObject(Identifiers identifiers, ClassInfo info, Scope scope) {
        this.packageName = info.getPackageName() + ".generated";
        this.userType = info.getQualifiedName();
        this.type = identifiers.getJMHtype(info);
        this.scope = scope;

        String id = identifiers.collapseTypeName(userType) + identifiers.identifier(scope);
        this.localIdentifier = "l_" + id;
        this.fieldIdentifier = "f_" + id;

        this.params = new TreeMultimap<>();
        this.helpers = new TreeSet<>();
        this.helperArgs = new HashMultimap<>();
        this.depends = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StateObject that = (StateObject) o;

        if (!fieldIdentifier.equals(that.fieldIdentifier))
            return false;
        if (scope != that.scope) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (fieldIdentifier.hashCode());
        return result;
    }

    public String toTypeDef() {
        return type + " " + localIdentifier;
    }

    public String toLocal() {
        return localIdentifier;
    }

    public Collection<String> getParamsLabels() {
        return params.keys();
    }

    public void addParam(FieldInfo fieldInfo) {
        params.put(fieldInfo.getName(), fieldInfo);
    }

    public Collection<FieldInfo> getParam(String name) {
        return params.get(name);
    }

    public String getParamAccessor(FieldInfo paramField) {
        String name = paramField.getName();
        String type = paramField.getType().getQualifiedName();

        if (type.equalsIgnoreCase("java.lang.String")) {
            return "control.getParam(\"" + name + "\")";
        }
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("java.lang.Boolean")) {
            return "Boolean.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("byte") || type.equalsIgnoreCase("java.lang.Byte")) {
            return "Byte.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("char") || type.equalsIgnoreCase("java.lang.Character")) {
            return "(control.getParam(\"" + name + "\")).charAt(0)";
        }
        if (type.equalsIgnoreCase("short") || type.equalsIgnoreCase("java.lang.Short")) {
            return "Short.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("java.lang.Integer")) {
            return "Integer.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("float") || type.equalsIgnoreCase("java.lang.Float")) {
            return "Float.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("long") || type.equalsIgnoreCase("java.lang.Long")) {
            return "Long.valueOf(control.getParam(\"" + name + "\"))";
        }
        if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("java.lang.Double")) {
            return "Double.valueOf(control.getParam(\"" + name + "\"))";
        }

        // assume enum
        return type + ".valueOf(control.getParam(\"" + name + "\"))";
    }

    public void addHelper(HelperMethodInvocation hmi) {
        helpers.add(hmi);
    }

    public Collection<HelperMethodInvocation> getHelpers() {
        return helpers;
    }
}
