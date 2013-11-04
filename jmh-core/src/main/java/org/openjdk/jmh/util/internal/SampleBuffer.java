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
package org.openjdk.jmh.util.internal;

import java.io.Serializable;

/**
 * Sampling buffer accepts samples.
 *
 * @author Aleksey Shipilev, aleksey.shipilev@oracle.com
 */
public class SampleBuffer implements Serializable {

    private static final int PRECISION_BITS = 10;
    private static final int SIZE_LIMIT = 1000000;

    private final int[][] hdr;

    private int size;

    public SampleBuffer() {
        hdr = new int[64][];
        clear();
    }

    private void clear() {
        for (int p = 0; p < 64; p++) {
            hdr[p] = new int[1 << PRECISION_BITS];
        }
        size = 0;
    }

    public boolean add(long sample) {
        if (size++ > SIZE_LIMIT) {
            clear();
            return true;
        } else {
            int msb = 64 - Long.numberOfLeadingZeros(sample);
            int bucket = Math.max(0, msb - PRECISION_BITS);
            int subBucket = (int) (sample >> bucket);
            hdr[bucket][subBucket]++;
            return false;
        }
    }

    public Statistics getStatistics() {
        MultisetStatistics stat = new MultisetStatistics();
        for (int i = 0; i < hdr.length; i++) {
            for (int j = 0; j < hdr[i].length; j++) {
                stat.addValue((long) j << i, hdr[i][j]);
            }
        }
        return stat;
    }

    public void addAll(SampleBuffer other) {
        for (int i = 0; i < other.hdr.length; i++) {
            for (int j = 0; j < other.hdr[i].length; j++) {
                hdr[i][j] += other.hdr[i][j];
            }
        }
    }
}
