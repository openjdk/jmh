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
package org.openjdk.jmh.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A utility class for File creation and manipulation.
 *
 * @author anders.astrand@oracle.com
 *
 */
public class FileUtils {
    // Static access only
    private FileUtils() {

    }

    /**
     * Helper method for extracting a given resource to File
     *
     * @param name name of the resource
     * @return a File pointing to the extracted resource
     * @throws IOException if things go crazy
     */
    public static File extractFromResource(String name) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(FileUtils.class.getResourceAsStream(name));
        String suffix = name;

        if (suffix.contains("/")) {
            suffix = name.substring(name.lastIndexOf('/') + 1);
        }

        File temp = File.createTempFile("extracted", suffix);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp));
        byte[] b = new byte[1024];

        int available = bis.available();
        while (available > 0) {
            int length = Math.min(b.length, available);

            int read = bis.read(b, 0, length);
            bos.write(b, 0, read);

            available = bis.available();
        }

        bos.flush();
        bos.close();

        bis.close();

        return temp;
    }

}
