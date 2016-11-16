/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.util.lines;

import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.util.lines.Constants.*;

public class TestLineReader {

    private final String line;
    private final boolean correct;
    private int cursor;

    public TestLineReader(String line) {
        this.line = line;
        this.correct = (line.length() > MAGIC.length() && line.startsWith(MAGIC));
        this.cursor = MAGIC.length();
    }

    private int readLen() {
        StringBuilder sb = new StringBuilder();
        char c = line.charAt(cursor);
        while (Character.isDigit(c)) {
            sb.append(c);
            cursor++;
            c = line.charAt(cursor);
        }
        cursor++;
        return Integer.valueOf(sb.toString());
    }

    private String readString() {
        int len = readLen();
        String s = line.substring(cursor, cursor + len);
        cursor += len + 1;
        return s;
    }

    private char readChar() {
        char c = line.charAt(cursor);
        cursor += 2;
        return c;
    }

    public String nextString() {
        char tag = readChar();
        if (tag == TAG_STRING) {
            return readString();
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    private RuntimeException error(String msg) {
        return new IllegalStateException("Error: " + msg + "\n at \"" + line + "\", pos " + cursor);
    }

    public boolean isCorrect() {
        return correct;
    }

    public Optional<Integer> nextOptionalInt() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_INT) {
            return Optional.of(Integer.valueOf(readString()));
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public Optional<String> nextOptionalString() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_STRING) {
            return Optional.of(readString());
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public Optional<TimeValue> nextOptionalTimeValue() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_TIMEVALUE) {
            return Optional.of(TimeValue.fromString(readString()));
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public Optional<TimeUnit> nextOptionalTimeUnit() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_TIMEUNIT) {
            return Optional.of(TimeUnit.valueOf(readString()));
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public Optional<Collection<String>> nextOptionalStringCollection() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_STRING_COLLECTION) {
            int len = readLen();
            Collection<String> list = new ArrayList<>();
            for (int c = 0; c < len; c++) {
                list.add(readString());
            }
            return Optional.of(list);
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public int[] nextIntArray() {
        char tag = readChar();
        if (tag == TAG_INT_ARRAY) {
            int len = readLen();
            int[] rs = new int[len];
            for (int c = 0; c < len; c++) {
                rs[c] = Integer.valueOf(readString());
            }
            return rs;
        } else {
            throw error("unexpected tag = " + tag);
        }
    }

    public Optional<Map<String, String[]>> nextOptionalParamCollection() {
        char tag = readChar();
        if (tag == Constants.TAG_EMPTY_OPTIONAL) {
            return Optional.none();
        } else if (tag == TAG_PARAM_MAP) {
            Map<String, String[]> result = new HashMap<>();

            int kvs = readLen();
            for (int kv = 0; kv < kvs; kv++) {
                String key = readString();

                int vlen = readLen();
                String[] values = new String[vlen];
                for (int v = 0; v < vlen; v++) {
                    values[v] = readString();
                }

                result.put(key, values);
            }
            return Optional.of(result);
        } else {
            throw error("unexpected tag = " + tag);
        }
    }
}
