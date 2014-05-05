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
package org.openjdk.jmh.util.internal;

import org.openjdk.jmh.runner.parameters.TimeValue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Option class
 * @param <T> stored value.
 */
public class Optional<T> implements Serializable {

    private final T val;

    private Optional(T val) {
        if (val == null) {
            throw new IllegalArgumentException("Val can not be null");
        }
        this.val = val;
    }

    private Optional() {
        this.val = null;
    }

    public T orElse(T elseVal) {
        return (val == null) ? elseVal : val;
    }

    public Optional<T> orAnother(Optional<T> alternative) {
        return (val == null) ? alternative : this;
    }

    /**
     * Produce empty Option
     * @param <T> type
     * @return empty option
     */
    public static <T> Optional<T> none() {
        return new Optional<T>();
    }

    /**
     * Wrap the existing value in Option.
     * @param val value to wrap
     * @param <T> type
     * @return option with value
     */
    public static <T> Optional<T> of(T val) {
        return new Optional<T>(val);
    }

    /**
     * Parse the existing string value into the Option
     * @param source source string
     * @param unmarshaller unmarshaller lambda parsing the (String -&gt; T)
     * @param <T> type
     * @return value wrapped in the Option
     */
    public static <T> Optional<T> of(String source, Unmarshaller<T> unmarshaller) {
        if (source.equals("[]")) {
            return Optional.none();
        } else {
            return Optional.of(unmarshaller.valueOf(source.substring(1, source.length() - 1)));
        }
    }

    public static <T> Optional<T> eitherOf(T val) {
        if (val == null) {
            return Optional.none();
        } else {
            return Optional.of(val);
        }
    }

    public boolean hasValue() {
        return val != null;
    }

    public String toString() {
        if (val == null) {
            return "[]";
        } else {
            return "[" + val + "]";
        }
    }

    public String toString(Marshaller<T> m) {
        if (val == null) {
            return "[]";
        } else {
            return "[" + m.valueOf(val) + "]";
        }
    }

    public T get() {
        if (val == null) {
            throw new IllegalStateException("Optional is null");
        }
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Optional optional = (Optional) o;

        if (val != null ? !val.equals(optional.val) : optional.val != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return val != null ? val.hashCode() : 0;
    }

    public interface Unmarshaller<T> {
        T valueOf(String s);
    }

    public interface Marshaller<T> {
        String valueOf(T val);
    }

    public static final Unmarshaller<Integer> INTEGER_UNMARSHALLER = new Unmarshaller<Integer>() {
        @Override
        public Integer valueOf(String s) {
            return Integer.valueOf(s);
        }
    };

    public static final Unmarshaller<TimeValue> TIME_VALUE_UNMARSHALLER = new Unmarshaller<TimeValue>() {
        @Override
        public TimeValue valueOf(String s) {
            return TimeValue.fromString(s);
        }
    };

    public static final Unmarshaller<Collection<String>> STRING_COLLECTION_UNMARSHALLER = new Unmarshaller<Collection<String>>() {
        @Override
        public Collection<String> valueOf(String s) {
            return Arrays.asList(s.split("===SEP==="));
        }
    };

    public static final Marshaller<Collection<String>> STRING_COLLECTION_MARSHALLER = new Optional.Marshaller<Collection<String>>() {
        @Override
        public String valueOf(Collection<String> src) {
            StringBuilder sb = new StringBuilder();
            for (String s : src) {
                sb.append(s).append("===SEP===");
            }
            return sb.toString();
        }
    };

    public static final Unmarshaller<Map<String, String[]>> PARAM_COLLECTION_UNMARSHALLER = new Unmarshaller<Map<String, String[]>>() {
        @Override
        public Map<String, String[]> valueOf(String s) {
            Map<String, String[]> map = new TreeMap<String, String[]>();
            String[] pairs = s.split("===PAIR-SEP===");
            for (String pair : pairs) {
                String[] kv = pair.split("===SEP-K===");
                if (kv[1].equalsIgnoreCase("===EMPTY===")) {
                    map.put(kv[0], new String[0]);
                } else {
                    map.put(kv[0], kv[1].split("===SEP-V==="));
                }
            }
            return map;
        }
    };

    public static final Marshaller<Map<String, String[]>> PARAM_COLLECTION_MARSHALLER = new Optional.Marshaller<Map<String, String[]>>() {
        @Override
        public String valueOf(Map<String, String[]> src) {
            StringBuilder sb = new StringBuilder();
            for (String s : src.keySet()) {
                sb.append(s);
                sb.append("===SEP-K===");
                if (src.get(s).length == 0) {
                    sb.append("===EMPTY===");
                } else {
                    for (String v : src.get(s)) {
                        sb.append(v);
                        sb.append("===SEP-V===");
                    }
                }
                sb.append("===PAIR-SEP===");
            }
            return sb.toString();
        }
    };

}
