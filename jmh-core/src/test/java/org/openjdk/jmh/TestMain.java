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
package org.openjdk.jmh;

import org.openjdk.jmh.Main;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Tests for Main
 *
 * @author anders.astrand@oracle.com
 *
 */
public class TestMain {

    @Test
    public void testFaultyArgs() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream tmp = System.err;

        System.setErr(new PrintStream(baos));
        Main.main(new String[] {"-kakor"});
        System.setErr(tmp);

        assertTrue(baos.toString().contains("\"-kakor\" is not a valid option"));
        baos.close();
    }


    @Test
    public void testNoArgs() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream tmp = System.err;

        System.setErr(new PrintStream(baos));
        Main.main(new String[] {});
        System.setErr(tmp);

        assertTrue(baos.toString().contains("Usage: [options]"));
        baos.close();
    }

    @Test
    public void testHelp() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream tmp = System.err;

        System.setErr(new PrintStream(baos));
        Main.main(new String[] {"-h"});
        System.setErr(tmp);

        assertTrue(baos.toString().contains("Usage: [options]"));
        baos.close();
    }

}
