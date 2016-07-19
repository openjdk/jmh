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
package org.openjdk.jmh;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.infra.Blackhole;

public class BlackholeTest {

    @Test
    public void test() {
        int tlr = 1;
        int tlrMask = 2;
        for (int t = 0; t < 1000000000; t++) {
            tlr = (tlr * 48271);
            if ((tlr & tlrMask) == 0) {
                // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
                tlrMask = (tlrMask << 1) + 2;
                System.out.println(t);
            }
        }
    }

    @Test
    public void testUserConstructor() {
        try {
            new Blackhole("Boyaa");
            Assert.fail("Should have failed");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        } catch (Throwable e) {
            Assert.fail("Failed unexpectedly");
        }
    }

}
