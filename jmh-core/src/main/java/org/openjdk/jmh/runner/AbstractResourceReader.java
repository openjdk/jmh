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

import java.io.*;
import java.net.URL;
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
                return Collections.<Reader>singletonList(new FileReader(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("ERROR: Could not find resource", e);
            }
        }

        if (resource != null) {
            Enumeration<URL> urls;
            try {
                urls = getClass().getClassLoader().getResources(
                        resource.startsWith("/")
                                ? resource.substring(1)
                                : resource
                );
            } catch (IOException e) {
                throw new RuntimeException("ERROR: While obtaining resource: " + resource, e);
            }

            if (urls.hasMoreElements()) {
                List<Reader> readers = new ArrayList<>();
                URL url = null;
                try {
                    while (urls.hasMoreElements()) {
                        url = urls.nextElement();
                        InputStream stream = url.openStream();
                        readers.add(new InputStreamReader(stream));
                    }
                } catch (IOException e) {
                    for (Reader r : readers) {
                        try {
                            r.close();
                        } catch (IOException e1) {
                            // ignore
                        }
                    }
                    throw new RuntimeException("ERROR: While opening resource: " + url, e);
                }
                return readers;
            } else {
                throw new RuntimeException("ERROR: Unable to find the resource: " + resource);
            }
        }

        throw new IllegalStateException();
    }


}
