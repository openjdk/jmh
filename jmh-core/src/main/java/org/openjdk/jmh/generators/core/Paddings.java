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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Paddings {

    private static final Map<String, List<String>> PADDING_CACHE = new HashMap<String, List<String>>();

    public static void padding(List<String> lines, String suffix) {
        List<String> ps = PADDING_CACHE.get(suffix);
        if (ps != null) {
            lines.addAll(ps);
            return;
        }

        List<String> tl = new ArrayList<String>();
        for (int p = 0; p < 16; p++) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append("boolean ").append(suffix).append(String.format("_%03d", p * 16 + 0));
            for (int q = 1; q < 16; q++) {
                sb.append(", ").append(suffix).append(String.format("_%03d", p * 16 + q));
            }
            sb.append(";");
            tl.add(sb.toString());
        }
        PADDING_CACHE.put(suffix, tl);
        lines.addAll(tl);
    }

    public static void padding(PrintWriter writer, String suffix) {
        List<String> ss = new ArrayList<String>();
        padding(ss, suffix);
        for (String s : ss) {
            writer.println(s);
        }
    }
}
