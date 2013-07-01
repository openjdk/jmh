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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.TreeSet;

public class CompilerHints extends AbstractResourceReader {

    public static final String LIST = "/META-INF/CompilerHints";
    public static CompilerHints defaultList = null;

    private final Set<String> hints;

    public static CompilerHints defaultList() {
        if (defaultList == null) {
            defaultList = fromResource(LIST);
        }
        return defaultList;
    }

    public static CompilerHints fromResource(String resource) {
        return new CompilerHints(null, resource, null);
    }

    public static CompilerHints fromFile(String file) {
        return new CompilerHints(file, null, null);
    }

    public static CompilerHints fromString(String line) {
        return new CompilerHints(null, null, line);
    }

    private CompilerHints(String file, String resource, String line) {
        super(file, resource, line);

        // naive protection
        String name = System.getProperty("java.vm.name");

        hints = read();

        if (!hints.isEmpty() && !(name.contains("HotSpot") || name.contains("OpenJDK"))) {
            System.err.println("WARNING: Not the HotSpot VM (\"" + name + "\"), compilerHints are disabled.");
            hints.clear();
        }
    }

    public Set<String> get() {
        return hints;
    }

    private Set<String> read() {
        Set<String> result = new TreeSet<String>();
        try {
            for (Reader r : getReaders()) {
                BufferedReader reader = new BufferedReader(r);

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.startsWith("#")) {
                        continue;
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    result.add(line);
                }

                reader.close();
            }

        } catch (IOException ex) {
            throw new RuntimeException("Error reading compiler hints", ex);
        }

        return result;
    }

}
