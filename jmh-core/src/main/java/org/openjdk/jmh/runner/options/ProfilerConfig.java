/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.runner.options;

import java.io.Serializable;

public class ProfilerConfig implements Serializable {

    private final String klass;
    private final String opts;

    public ProfilerConfig(String klass, String opts) {
        this.klass = klass;
        this.opts = opts;
    }

    public ProfilerConfig(String klass) {
        this.klass = klass;
        this.opts = "";
    }

    public String getKlass() {
        return klass;
    }

    public String getOpts() {
        return opts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfilerConfig that = (ProfilerConfig) o;

        if (!klass.equals(that.klass)) return false;
        if (!opts.equals(that.opts)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = klass.hashCode();
        result = 31 * result + opts.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return klass + ":" + opts;
    }
}
