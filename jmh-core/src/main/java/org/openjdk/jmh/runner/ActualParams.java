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
package org.openjdk.jmh.runner;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

public class ActualParams implements Comparable<ActualParams>, Serializable {

    private final SortedMap<String, String> params;

    public ActualParams() {
        params = new TreeMap<String, String>();
    }

    private ActualParams(SortedMap<String, String> params) {
        this();
        this.params.putAll(params);
    }

    @Override
    public int compareTo(ActualParams o) {
        return params.toString().compareTo(o.params.toString());
    }

    public void put(String k, String v) {
        params.put(k, v);
    }

    public boolean containsKey(String name) {
        return params.containsKey(name);
    }

    public String get(String name) {
        return params.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActualParams that = (ActualParams) o;

        if (params != null ? !params.equals(that.params) : that.params != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return params != null ? params.hashCode() : 0;
    }

    @Override
    public String toString() {
        if (params.isEmpty()) {
            return "<none>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean isFirst = true;
        for (String k : params.keySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(k).append(" = ").append(params.get(k));
        }
        sb.append(")");
        return sb.toString();
    }

    public ActualParams copy() {
        return new ActualParams(params);
    }
}
