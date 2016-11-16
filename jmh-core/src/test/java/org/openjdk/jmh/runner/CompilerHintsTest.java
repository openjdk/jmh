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
package org.openjdk.jmh.runner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompilerHintsTest {

    private String vmName;

    @Before
    public void storeCurrentVM() {
        vmName = System.getProperty("java.vm.name");
    }

    @Test
    public void testListNotEmptyForCompliantJvms() {
        for (String name : CompilerHints.HINT_COMPATIBLE_JVMS) {
            System.setProperty("java.vm.name", name);
            List<String> args = new ArrayList<>();
            CompilerHints.addCompilerHints(args);
            assertFalse(args.isEmpty());
        }
    }

    @Test
    public void testListEmptyForOldZingJvms() {
        System.setProperty("java.vm.name", "Zing");
        System.setProperty("java.version", "1.7.0-zing_5.9.2.0");
        // load up some default hints
        List<String> args = new ArrayList<>();
        CompilerHints.addCompilerHints(args);
        assertTrue(args.isEmpty());
    }

    @Test
    public void testListNotEmptyForNewerZingJvms() {
        System.setProperty("java.vm.name", "Zing");
        System.setProperty("java.version", "1.7.0-zing_5.10.2.0");
        // load up some default hints
        List<String> args = new ArrayList<>();
        CompilerHints.addCompilerHints(args);
        assertFalse(args.isEmpty());
    }

    @Test
    public void testListEmptyForNonCompliantJvms() {
        System.setProperty("java.vm.name", "StupidVmCantTakeAHint");
        List<String> args = new ArrayList<>();
        CompilerHints.addCompilerHints(args);
        assertTrue(args.isEmpty());
    }

    @After
    public void restoreCurrentVM() {
        System.setProperty("java.vm.name", vmName);
    }
}
