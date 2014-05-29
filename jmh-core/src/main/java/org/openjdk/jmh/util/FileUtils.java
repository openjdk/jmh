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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * A utility class for File creation and manipulation.
 */
public class FileUtils {
    // Static access only
    private FileUtils() {

    }

    public static File tempFile(String suffix) throws IOException {
        File file = File.createTempFile("jmh", suffix);
        file.deleteOnExit();
        return file;
    }

    /**
     * Helper method for extracting a given resource to File
     *
     * @param name name of the resource
     * @return a File pointing to the extracted resource
     * @throws IOException if things go crazy
     */
    public static File extractFromResource(String name) throws IOException {
        InputStream fis = null;
        OutputStream fos = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            fis = FileUtils.class.getResourceAsStream(name);
            bis = new BufferedInputStream(fis);

            File temp = FileUtils.tempFile("extracted");

            fos = new FileOutputStream(temp);
            bos = new BufferedOutputStream(fos);
            byte[] b = new byte[1024];

            int available = bis.available();
            while (available > 0) {
                int length = Math.min(b.length, available);

                int read = bis.read(b, 0, length);
                bos.write(b, 0, read);

                available = bis.available();
            }

            return temp;
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                } catch (IOException e) {
                    // ignore
                }
                try {
                    bos.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Create a temporary file (see {@link File#createTempFile(String, String)}) and fill it with the given lines.
     *
     * @param prefix file prefix as for {@link File#createTempFile(String, String)}
     * @param suffix file suffix {@link File#createTempFile(String, String)}
     * @param lines to be written
     *
     * @return the temporary file absolute path
     * @throws IOException on file creation error
     */
    public static String createTempFileWithLines(String prefix, String suffix, Iterable<String> lines)
            throws IOException {
        File file = FileUtils.tempFile(suffix);
        PrintWriter pw = new PrintWriter(file);
        for (String l : lines) {
            pw.println(l);
        }
        pw.close();
        return file.getAbsolutePath();
    }

}
