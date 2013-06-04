/**
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

import org.openjdk.jmh.annotations.Mode;

import java.io.Serializable;

public class BenchmarkRecord implements Comparable<BenchmarkRecord>, Serializable {

    private final String userName;
    private final String generatedName;
    private final Mode mode;

    public BenchmarkRecord(String userName, String generatedName, Mode mode) {
        this.userName = userName;
        this.generatedName = generatedName;
        this.mode = mode;
    }

    public BenchmarkRecord(String line) {
        String[] args = line.split(",");

        if (args.length != 3) {
            throw new IllegalStateException("Mismatched format for the line: " + line);
        }

        this.userName = args[0].trim();
        this.generatedName = args[1].trim();
        this.mode = Mode.deepValueOf(args[2].trim());
    }

    public String toLine() {
        return userName + ", " + generatedName + ", " + mode;
    }

    public BenchmarkRecord cloneWith(Mode mode) {
        return new BenchmarkRecord(userName, generatedName, mode);
    }

    @Override
    public int compareTo(BenchmarkRecord o) {
        int v = mode.compareTo(o.mode);
        if (v != 0) {
            return v;
        }

        return userName.compareTo(o.userName);
    }

    public String generatedTarget(Mode type) {
        return generatedName + "_" + type;
    }

    public String generatedTarget() {
        return generatedTarget(mode);
    }

    public String generatedClass() {
        String s = generatedTarget();
        return s.substring(0, s.lastIndexOf('.'));
    }

    public String generatedMethod() {
        String s = generatedTarget();
        return s.substring(s.lastIndexOf('.') + 1);
    }

    public String getUsername() {
        return userName;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "BenchmarkRecord{" +
                "userName='" + userName + '\'' +
                ", generatedName='" + generatedName + '\'' +
                ", mode=" + mode +
                '}';
    }
}
