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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for loading classes in various ways.
 */
public class ClassUtils {

    private static final boolean USE_SEPARATE_CLASSLOADER = Boolean.getBoolean("jmh.separateClassLoader");

    // Static access only
    private ClassUtils() {

    }

    /**
     * Enumerates all methods in hierarchy. Note that is different from both Class.getDeclaredMethods() and
     * Class.getMethods().
     *
     * @param clazz class to enumerate.
     * @return list of methods.
     */
    public static List<Method> enumerateMethods(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            result.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        return result;
    }

    public static Class<?> loadClass(String className) {
        try {
            if (!USE_SEPARATE_CLASSLOADER) {
                return Class.forName(className);
            }

            // load the class in a different classloader
            String classPathValue = System.getProperty("java.class.path");
            String[] classPath = classPathValue.split(File.pathSeparator);
            URL[] classPathUrl = new URL[classPath.length];
            for (int i = 0; i < classPathUrl.length; i++) {
                try {
                    classPathUrl[i] = new File(classPath[i]).toURI().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException("Error parsing the value of property java.class.path: " + classPathValue, ex);
                }
            }

            URLClassLoader loader = new URLClassLoader(classPathUrl);
            return loader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Benchmark does not match a class", ex);
        }
    }

    /**
     * Make the collection of class names denser.
     *
     * @param src source class names
     * @return map of [src class name, denser class name]
     */
    public static Map<String, String> denseClassNames(Collection<String> src) {
        if (src.isEmpty()) return Collections.emptyMap();

        int maxLen = Integer.MIN_VALUE;
        for (String s : src) {
            maxLen = Math.max(maxLen, s.length());
        }

        boolean first = true;

        boolean prefixCut = false;

        String[] prefix = new String[0];
        for (String s : src) {
            String[] names = s.split("\\.");

            if (first) {
                prefix = new String[names.length];

                int c;
                for (c = 0; c < names.length; c++) {
                    if (names[c].toLowerCase().equals(names[c])) {
                        prefix[c] = names[c];
                    } else {
                        break;
                    }
                }
                prefix = Arrays.copyOf(prefix, c);
                first = false;
                continue;
            }

            int c = 0;
            while (c < Math.min(prefix.length, names.length)) {
                String n = names[c];
                String p = prefix[c];
                if (!n.equals(p) || !n.toLowerCase().equals(n)) {
                    break;
                }
                c++;
            }

            if (prefix.length != c) {
                prefixCut = true;
            }
            prefix = Arrays.copyOf(prefix, c);
        }

        for (int c = 0; c < prefix.length; c++) {
            prefix[c] = prefixCut ? String.valueOf(prefix[c].charAt(0)) : "";
        }

        Map<String, String> result = new HashMap<>();
        for (String s : src) {
            int prefixLen = prefix.length;

            String[] names = s.split("\\.");
            System.arraycopy(prefix, 0, names, 0, prefixLen);

            String dense = "";
            for (String n : names) {
                if (!n.isEmpty()) {
                    dense += n + ".";
                }
            }

            if (dense.endsWith(".")) {
                dense = dense.substring(0, dense.length() - 1);
            }

            result.put(s, dense);
        }

        return result;
    }


}
