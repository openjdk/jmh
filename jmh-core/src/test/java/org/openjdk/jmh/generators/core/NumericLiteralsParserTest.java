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

import static org.junit.Assert.*;

public class NumericLiteralsParserTest {
    @Test
    public void testIntegerLiteral() {
        testValidIntegerLiteral(10, "10");
        testValidIntegerLiteral(1_0, "10");
        testValidIntegerLiteral(1__0__0, "1__0__0");
        testValidIntegerLiteral(0xFFF, "0xFFF");
        testValidIntegerLiteral(0xF__F_F, "0xF__F_F");
        testValidIntegerLiteral(0000, "0000");
        testValidIntegerLiteral(0_1234, "0_1234");
        testValidIntegerLiteral(0_12___3_4, "0_12___3_4");
        testValidIntegerLiteral(0b0_000, "0b0_000");
        testValidIntegerLiteral(0b0_1___00, "0b0_1___00");

        testInvalidIntegerLiteral("_00");
        testInvalidIntegerLiteral("00_");
        testInvalidIntegerLiteral("123_");
        testInvalidIntegerLiteral("0x_F");
        testInvalidIntegerLiteral("0_xF");
        testInvalidIntegerLiteral("0b1212");
        testInvalidIntegerLiteral("09");
        testInvalidIntegerLiteral("0_9");
    }

    private void testValidIntegerLiteral(int actualValue, String literal) {
        Assert.assertTrue(NumericLiteralsParser.isValidLiteral(literal, NumericLiteralsParser.Type.INT));
        assertEquals(actualValue, NumericLiteralsParser.parseInt(literal));
    }

    private void testInvalidIntegerLiteral(String literal) {
        Assert.assertFalse(NumericLiteralsParser.isValidLiteral(literal, NumericLiteralsParser.Type.INT));
        try {
            NumericLiteralsParser.parseInt(literal);
            fail();
        } catch (NumberFormatException e) {
            // Nothing
        }
    }
}
