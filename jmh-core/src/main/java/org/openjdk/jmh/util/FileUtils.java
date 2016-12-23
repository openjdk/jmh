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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for File creation and manipulation.
 */
public class FileUtils {
    // Static access only
    private FileUtils() {

    }

    static final TempFileManager TEMP_FILE_MANAGER = new TempFileManager();

    /**
     * Creates the temp file, and retains it as long as the reference to it
     * is reachable.
     *
     * @param suffix suffix
     * @return temp file
     * @throws IOException
     */
    public static TempFile weakTempFile(String suffix) throws IOException {
        return TEMP_FILE_MANAGER.create(suffix);
    }

    public static void purgeTemps() {
        TEMP_FILE_MANAGER.purge();
    }

    /**
     * Creates the temp file with given suffix. The file would be removed
     * on JVM exit, or when caller deletes the file itself.
     *
     * @param suffix suffix
     * @return temporary file
     * @throws IOException
     */
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
        File temp = FileUtils.tempFile("extracted");
        try (InputStream fis = FileUtils.class.getResourceAsStream(name);
             OutputStream fos = new FileOutputStream(temp)) {

            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }

            fos.close();

            return temp;
        }
    }

    /**
     * Create a temporary file (see {@link File#createTempFile(String, String)}) and fill it with the given lines.
     *
     * @param suffix file suffix {@link File#createTempFile(String, String)}
     * @param lines to be written
     *
     * @return the temporary file absolute path
     * @throws IOException on file creation error
     */
    public static String createTempFileWithLines(String suffix, Iterable<String> lines)
            throws IOException {
        File file = FileUtils.tempFile(suffix);
        PrintWriter pw = new PrintWriter(file);
        for (String l : lines) {
            pw.println(l);
        }
        pw.close();
        return file.getAbsolutePath();
    }

    public static Collection<String> tail(File file, int num) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader is = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(is)) {
            LinkedList<String> lines = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > num) {
                    lines.remove(0);
                }
            }
            return lines;
        }
    }

    public static Collection<String> readAllLines(Reader src) throws IOException {
        BufferedReader reader = new BufferedReader(src);
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    public static Collection<String> readAllLines(File file) throws IOException {
        try (FileReader fr = new FileReader(file)) {
            return readAllLines(fr);
        }
    }

    public static Collection<String> readAllLines(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            return readAllLines(reader);
        } finally {
            FileUtils.safelyClose(stream);
        }
    }

    public static void writeLines(File file, Collection<String> lines) throws IOException {
        try (PrintWriter pw = new PrintWriter(file)) {
            for (String line : lines) {
                pw.println(line);
            }
            pw.close();
        }
    }

    public static void appendLines(File file, Collection<String> lines) throws IOException {
        Collection<String> newLines = new ArrayList<>();
        try {
            newLines.addAll(readAllLines(file));
        } catch (IOException e) {
            // no file
        }
        newLines.addAll(lines);
        writeLines(file, newLines);
    }

    public static Collection<File> getClasses(File root) {
        Collection<File> result = new ArrayList<>();

        List<File> newDirs = new ArrayList<>();
        newDirs.add(root);
        while (!newDirs.isEmpty()) {
            List<File> add = new ArrayList<>();
            for (File dir : newDirs) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            add.add(f);
                        } else {
                            if (f.getName().endsWith(".class")) {
                                result.add(f);
                            }
                        }
                    }
                }
            }
            newDirs.clear();
            newDirs = add;
        }

        return result;
    }

    public static void copy(String src, String dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }

            fos.close();
        }
    }

    public static <T extends Flushable & Closeable> void safelyClose(T obj) {
        if (obj != null) {
            try {
                obj.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                obj.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static <T extends Closeable> void safelyClose(T obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public static void touch(String f) throws IOException {
        File file = new File(f);
        try {
            if (file.createNewFile() || file.canWrite()) {
                return;
            }
        } catch (IOException e) {
            // fall-through
        }
        throw new IOException("The file is not writable: " + f);
    }
}
