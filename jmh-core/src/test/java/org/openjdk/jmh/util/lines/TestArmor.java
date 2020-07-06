/*
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmh.util.lines;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class TestArmor {

    @Test
    public void simple() {
        String[] srcs = new String[] {
                "jmh",
                "test"
        };

        for (String src : srcs) {
            Assert.assertEquals(src, src, Armor.decode(Armor.encode(src)));
        }
    }

    @Test
    public void exhaustivePlaces() {
        for (char c = 0; c < Character.MAX_VALUE; c++) {
            testFour(c, (char)0, (char)0, (char)0);
            testFour((char)0, c, (char)0, (char)0);
            testFour((char)0, (char)0, c, (char)0);
            testFour((char)0, (char)0, (char)0, c);
        }
    }

    private void testFour(char c1, char c2, char c3, char c4) {
        StringBuilder sb = new StringBuilder();
        sb.append(c1);
        sb.append(c2);
        sb.append(c3);
        sb.append(c4);

        String src = sb.toString();
        String dst = Armor.decode(Armor.encode(src));

        Assert.assertEquals(src, src, dst);
    }

    @Test
    public void random() {
        Random r = new Random(1);
        for (int c = 0; c < 100000; c++) {
            for (int s = 0; s < 10; s++) {
                testWith(r, s, 127);
                testWith(r, s, 255);
                testWith(r, s, Character.MAX_VALUE - 10);
            }
        }
    }

    private void testWith(Random r, int size, int maxChar) {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < size; s++) {
            sb.append((char)r.nextInt(maxChar));
        }

        String src = sb.toString();
        String dst = Armor.decode(Armor.encode(src));

        Assert.assertEquals(src, src, dst);
    }

}
