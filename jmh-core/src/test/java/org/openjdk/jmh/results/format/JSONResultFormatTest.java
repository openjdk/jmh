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
package org.openjdk.jmh.results.format;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Extra tests for special cases of the JSON formatter.
 *
 * @author Jens Wilke
 * @see JSONResultFormat
 */
public class JSONResultFormatTest {

    @Test
    public void toJsonString_tidy() {
        String s = JSONResultFormat.toJsonString("abc,\"{}()\\(\\)[]{}");
        s = JSONResultFormat.tidy(s);
        assertEquals("\"abc,\\\"{}()\\\\(\\\\)[]{}\"\n", s);
    }

    @Test
    public void toJsonString_tidy_curly() {
        String s = JSONResultFormat.toJsonString("{}");
        s = JSONResultFormat.tidy(s);
        assertEquals("\"{}\"\n", s);
    }

    @Test
    public void toJsonString_tidy_curved() {
        String s = JSONResultFormat.toJsonString("()");
        s = JSONResultFormat.tidy(s);
        assertEquals("\"()\"\n", s);
    }

    @Test
    public void toJsonString_tidy_escapedDoubleQuote() {
        String s = JSONResultFormat.toJsonString("\"");
        s = JSONResultFormat.tidy(s);
        assertEquals("\"\\\"\"\n", s);
    }

    @Test
    public void toJsonString_tidy_escapedEscape() {
        String s = JSONResultFormat.toJsonString("\\");
        s = JSONResultFormat.tidy(s);
        assertEquals("\"\\\\\"\n", s);
    }

    /**
     * Check that every ASCII character in a string makes it transparently through
     * the JSON tidying and formatting process.
     */
    @Test
    public void toJsonString_tidy_asciiTransparent () {
        for (char i = 32; i < 127; i++) {
            if (i == '"') {
                continue;
            }
            if (i == '\\') {
                continue;
            }
            String s = JSONResultFormat.toJsonString(Character.toString(i));
            s = JSONResultFormat.tidy(s);
            assertEquals("\"" + i + "\"\n", s);
        }
    }

}