/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Paddings {

    private static final Map<String, String> PADDING_CACHE =
            Collections.synchronizedMap(new HashMap<>());

    private static String generate(String prefix) {
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be empty");
        }
        StringJoiner sj = new StringJoiner("");
        for (int p = 0; p < 16; p++) {
            sj.add("    byte ");
            for (int q = 0; q < 16; q++) {
                if (q != 0) {
                    sj.add(", ");
                }
                sj.add(prefix);
                sj.add(String.format("%03d", p * 16 + q));
            }
            sj.add(";");
            sj.add(System.lineSeparator());
        }
        return sj.toString();
    }

    public static void padding(PrintWriter writer, String prefix) {
        writer.print(PADDING_CACHE.computeIfAbsent(prefix, Paddings::generate));
    }
}
