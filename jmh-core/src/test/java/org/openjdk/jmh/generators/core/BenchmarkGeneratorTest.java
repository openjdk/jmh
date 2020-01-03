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
package org.openjdk.jmh.generators.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class BenchmarkGeneratorTest {

    static final int TAB_SIZE = 4;

    @Test
    public void testIndentsForward() {
        for (int c = 0; c < 10; c++) {
            BenchmarkGenerator.INDENTS = null;
            Assert.assertEquals(c*TAB_SIZE, BenchmarkGenerator.ident(c).length());
        }
    }

    @Test
    public void testIndentsBackwards() {
        for (int c = 10; c >= 0; c--) {
            BenchmarkGenerator.INDENTS = null;
            Assert.assertEquals(c*TAB_SIZE, BenchmarkGenerator.ident(c).length());
        }
    }

    @Test
    public void testIndentsRandom() {
        for (int c = 0; c < 10; c++) {
            BenchmarkGenerator.INDENTS = null;
            int i = ThreadLocalRandom.current().nextInt(10);
            Assert.assertEquals(i*TAB_SIZE, BenchmarkGenerator.ident(i).length());
        }
    }

}
