/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.profile;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class XCTraceTestBase {
    protected final SAXParserFactory factory = SAXParserFactory.newInstance();

    protected static InputStream openResource(String name) {
        InputStream stream = XCTraceTestBase.class.getResourceAsStream("/org.openjdk.jmh.profile.xctrace/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        return stream;
    }

    protected static List<Object[]> readExpectedData(String name) {
        InputStream stream = XCTraceTestBase.class.getResourceAsStream("/org.openjdk.jmh.profile.xctrace/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        List<Object[]> rows = new ArrayList<>();
        long[] empty = new long[0];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (true) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    break;
                }
                // line format:
                // timestamp;weight;0xAddress;symbol;name;pmc-events
                String[] partsRaw = line.split(";");
                if (partsRaw.length > 6) {
                    throw new IllegalStateException("Can't parse line: " + line);
                }
                String[] parts = Arrays.copyOf(partsRaw, 6);
                for (int idx = partsRaw.length; idx < parts.length; idx++) {
                    parts[idx] = "";
                }
                Object[] row = new Object[6];
                row[0] = Long.parseLong(parts[0]);
                row[1] = Long.parseLong(parts[1]);
                row[2] = Long.parseUnsignedLong(parts[2].substring(2), 16);
                row[3] = parts[3].isEmpty() ? null : parts[3];
                row[4] = parts[4].isEmpty() ? null : parts[4];
                if (parts[5].isEmpty()) {
                    row[5] = empty;
                } else {
                    row[5] = Arrays.stream(parts[5].split(" ")).mapToLong(Long::parseLong).toArray();
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return rows;
    }
}
