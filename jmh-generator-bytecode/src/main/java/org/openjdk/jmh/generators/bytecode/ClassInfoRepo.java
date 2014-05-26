/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.generators.bytecode;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.reflective.RFClassInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassInfoRepo {

    private final Map<String, ClassInfo> map = new HashMap<String, ClassInfo>();

    public ClassInfo get(String desc) {
        desc = desc.replace('/', '.');
        ClassInfo info = map.get(desc);
        if (info != null) {
            return info;
        }

        if (desc.equals(boolean.class.getCanonicalName()))  return new RFClassInfo(boolean.class);
        if (desc.equals(byte.class.getCanonicalName()))     return new RFClassInfo(byte.class);
        if (desc.equals(char.class.getCanonicalName()))     return new RFClassInfo(char.class);
        if (desc.equals(short.class.getCanonicalName()))    return new RFClassInfo(short.class);
        if (desc.equals(int.class.getCanonicalName()))      return new RFClassInfo(int.class);
        if (desc.equals(float.class.getCanonicalName()))    return new RFClassInfo(float.class);
        if (desc.equals(long.class.getCanonicalName()))     return new RFClassInfo(long.class);
        if (desc.equals(double.class.getCanonicalName()))   return new RFClassInfo(double.class);

        if (desc.equals(boolean[].class.getCanonicalName()))  return new RFClassInfo(boolean[].class);
        if (desc.equals(byte[].class.getCanonicalName()))     return new RFClassInfo(byte[].class);
        if (desc.equals(char[].class.getCanonicalName()))     return new RFClassInfo(char[].class);
        if (desc.equals(short[].class.getCanonicalName()))    return new RFClassInfo(short[].class);
        if (desc.equals(int[].class.getCanonicalName()))      return new RFClassInfo(int[].class);
        if (desc.equals(float[].class.getCanonicalName()))    return new RFClassInfo(float[].class);
        if (desc.equals(long[].class.getCanonicalName()))     return new RFClassInfo(long[].class);
        if (desc.equals(double[].class.getCanonicalName()))   return new RFClassInfo(double[].class);

        if (desc.endsWith("[]")) {
            desc = "[L" + desc.substring(0, desc.length() - 2) + ";";
        }

        try {
            return new RFClassInfo(Class.forName(desc, false, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve class: " + desc);
        }
    }

    public void put(String desc, ClassInfo info) {
        desc = desc.replace('/', '.');
        map.put(desc, info);
    }

    public Collection<ClassInfo> getInfos() {
        return map.values();
    }
}
