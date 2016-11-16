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
package org.openjdk.jmh.runner;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class WorkloadParams implements Comparable<WorkloadParams>, Serializable {
    private static final long serialVersionUID = 780563934988950196L;

    private final SortedMap<String, Value> params;

    public WorkloadParams() {
        params = new TreeMap<>();
    }

    private WorkloadParams(SortedMap<String, Value> params) {
        this();
        this.params.putAll(params);
    }

    @Override
    public int compareTo(WorkloadParams o) {
        if (!params.keySet().equals(o.params.keySet())) {
            throw new IllegalStateException("Comparing actual params with different key sets.");
        }

        for (Map.Entry<String, Value> e : params.entrySet()) {
            int cr = e.getValue().compareTo(o.params.get(e.getKey()));
            if (cr != 0) {
                return cr;
            }
        }
        return 0;
    }

    public void put(String k, String v, int vOrder) {
        params.put(k, new Value(v, vOrder));
    }

    public boolean containsKey(String name) {
        return params.containsKey(name);
    }

    public String get(String name) {
        Value value = params.get(name);
        if (value == null) {
            return null;
        } else {
            return value.value;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkloadParams that = (WorkloadParams) o;

        if (params != null ? !params.equals(that.params) : that.params != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return params != null ? params.hashCode() : 0;
    }

    @Override
    public String toString() {
        return params.toString();
    }

    public WorkloadParams copy() {
        return new WorkloadParams(params);
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public Collection<String> keys() {
        return params.keySet();
    }

    private static class Value implements Comparable<Value>, Serializable {
        private static final long serialVersionUID = 8846779314306880977L;

        private final String value;
        private final int order;

        public Value(String value, int order) {
            this.value = value;
            this.order = order;
        }

        @Override
        public int compareTo(Value o) {
            return Integer.valueOf(order).compareTo(o.order);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value1 = (Value) o;

            if (value != null ? !value.equals(value1.value) : value1.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

}
