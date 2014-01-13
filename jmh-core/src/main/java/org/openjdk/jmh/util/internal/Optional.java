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
     * @param extractor extractor lambda parsing the (String -> T)
     * @param <T> type
     * @return value wrapped in the Option
     */
    public static <T> Optional<T> of(String source, Extractor<T> extractor) {
        if (source.equals("[]")) {
            return Optional.none();
        } else {
            return Optional.of(extractor.valueOf(source.substring(1, source.length() - 1)));
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

    public interface Extractor<T> {
        T valueOf(String s);
    }

    public static final Extractor<Integer> INTEGER_EXTRACTOR = new Optional.Extractor<Integer>() {
        @Override
        public Integer valueOf(String s) {
            return Integer.valueOf(s);
        }
    };

    public static final Extractor<TimeValue> TIME_VALUE_EXTRACTOR = new Optional.Extractor<TimeValue>() {
        @Override
        public TimeValue valueOf(String s) {
            return TimeValue.fromString(s);
        }
    };

    public static final Extractor<String> STRING_EXTRACTOR = new Optional.Extractor<String>() {
        @Override
        public String valueOf(String s) {
            return s;
        }
    };

}
