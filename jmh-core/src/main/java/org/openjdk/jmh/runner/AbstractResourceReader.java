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

import org.openjdk.jmh.util.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class AbstractResourceReader {

    private final String file;
    private final String resource;
    private final String strings;

    protected AbstractResourceReader(String file, String resource, String strings) {
        this.file = file;
        this.resource = resource;
        this.strings = strings;
    }

    /**
     * Helper method for creating a Reader for the list file.
     *
     * @return a correct Reader instance
     */
    protected List<Reader> getReaders() {
        if (strings != null) {
            return Collections.<Reader>singletonList(new StringReader(strings));
        }

        if (file != null) {
            try {
                return Collections.<Reader>singletonList(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            } catch (FileNotFoundException e) {
                internalError("Could not find " + file, e);
            }
        }

        if (resource != null) {
            List<Reader> readers = new ArrayList<>();
            try {
                String name = resource.startsWith("/") ? resource.substring(1) : resource;
                Enumeration<URL> urls = getClass().getClassLoader().getResources(name);
                while (urls.hasMoreElements()) {
                   URL url = urls.nextElement();
                   InputStream stream = url.openStream();
                   readers.add(new InputStreamReader(stream, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                internalError("Unable to find " + resource, e);
                for (Reader r : readers) {
                    FileUtils.safelyClose(r);
                }
            }
            if (readers.isEmpty()) {
                internalError("Unable to find " + resource, null);
            }
            return readers;
        }

        throw new IllegalStateException();
    }

    protected void internalError(String msg, Exception e) {
        String guidance = "Internal error reading resource file: " + msg + "\n\n" +
                "This often indicates a build configuration problem. Common causes are:\n\n" +
                " 1. Annotation processing is not enabled or configured incorrectly.\n\n" +
                "    Note that JDK 23+ disables running annotation processors by default,\n" +
                "    which affects projects that used to build fine with older JDKs.\n" +
                "    Check if you need to add a relevant option to your compiler plugin.\n\n" +
                "    For example, maven-compiler-plugin can be configured like this:\n" +
                "        <configuration>\n" +
                "           <annotationProcessors>\n" +
                "              <annotationProcessor>org.openjdk.jmh.generators.BenchmarkProcessor</annotationProcessor>\n" +
                "           </annotationProcessors>\n" +
                "        </configuration>\n" +
                "\n" +
                " 2. Multi-module benchmark builds have not merged the resource files.\n\n" +
                "    Check if you need to add a relevant config to your build.\n\n" +
                "    For example, maven-shade-plugin needs to explicitly enable resource transformers:\n" +
                "       https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html\n\n";
        throw new RuntimeException(guidance, e);
    }

}
