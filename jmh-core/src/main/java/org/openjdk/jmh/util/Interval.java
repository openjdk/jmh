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
package org.openjdk.jmh.util;

public class Interval implements Comparable<Interval> {
    public final long src;
    public final long dst;

    public Interval(long src, long dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interval interval = (Interval) o;

        if (dst != interval.dst) return false;
        if (src != interval.src) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (src ^ (src >>> 32));
        result = 31 * result + (int) (dst ^ (dst >>> 32));
        return result;
    }

    @Override
    public int compareTo(Interval o) {
        if (src < o.src) {
            return -1;
        } else if (src > o.src) {
            return 1;
        } else {
            return (dst < o.dst) ? -1 : ((dst == o.dst) ? 0 : 1);
        }
    }

    @Override
    public String toString() {
        return "[" + src + ", " + dst + "]";
    }
}
