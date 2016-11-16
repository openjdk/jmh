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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.util.lines.Constants.*;

public class TestLineWriter {

    private final StringBuilder line;

    public TestLineWriter() {
        line = new StringBuilder();
        line.append(MAGIC);
    }

    private void appendWithLen(String s) {
        appendLen(s.length());
        line.append(s);
        line.append(" ");
    }

    private void appendLen(int len) {
        line.append(len);
        line.append(" ");
    }

    private void appendTag(char tag) {
        line.append(tag);
        line.append(" ");
    }

    public void putString(String s) {
        appendTag(TAG_STRING);
        appendWithLen(s);
    }

    public void putOptionalInt(Optional<Integer> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_INT);
            appendWithLen(String.valueOf(opt.get()));
        }
    }

    public void putOptionalString(Optional<String> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_STRING);
            appendWithLen(opt.get());
        }
    }

    public void putIntArray(int[] arr) {
        appendTag(TAG_INT_ARRAY);
        appendLen(arr.length);
        for (int v : arr) {
            appendWithLen(String.valueOf(v));
        }
    }

    public void putOptionalStringCollection(Optional<Collection<String>> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_STRING_COLLECTION);
            Collection<String> coll = opt.get();
            appendLen(coll.size());
            for (String s : coll) {
                appendWithLen(s);
            }
        }
    }

    public void putOptionalTimeValue(Optional<TimeValue> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_TIMEVALUE);
            appendWithLen(opt.get().toString());
        }
    }

    public void putOptionalTimeUnit(Optional<TimeUnit> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_TIMEUNIT);
            appendWithLen(opt.get().toString());
        }
    }

    public void putOptionalParamCollection(Optional<Map<String, String[]>> opt) {
        if (!opt.hasValue()) {
            appendTag(TAG_EMPTY_OPTIONAL);
        } else {
            appendTag(TAG_PARAM_MAP);

            Map<String, String[]> map = opt.get();
            appendLen(map.size());

            for (String key : map.keySet()) {
                appendWithLen(key);

                String[] vals = map.get(key);
                appendLen(vals.length);

                for (String value : vals) {
                    appendWithLen(value);
                }
            }
        }
    }

    @Override
    public String toString() {
        return line.toString();
    }

}
