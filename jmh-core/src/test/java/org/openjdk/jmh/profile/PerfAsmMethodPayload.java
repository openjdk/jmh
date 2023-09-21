/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
package org.openjdk.jmh.profile;

public class PerfAsmMethodPayload {
    public static void main(String... args) throws Exception {
        Thread.sleep(1000); // let stubs print
        long s = 0;
        for (int c = 0; c < 100_000; c++) {
            doInterface(new C1());
            doInterface(new C2());
            doInterface(new C3());
            doVirtual(new C1());
            doVirtual(new C2());
            doVirtual(new C3());
            s += System.currentTimeMillis();
        }
        System.out.println("Hello " + s);
    }

    public static void doInterface(I i) {
        while (!i.doWork());
    }

    public static void doVirtual(B b) {
        while (!b.doWork());
    }

    interface I {
        boolean doWork();
    }

    static class B {
        int count = 100;

        public boolean doWork() {
            return (count-- < 0);
        }
    }

    static class C1 extends B implements I {
        public boolean doWork() {
            return super.doWork();
        }
    }

    static class C2 extends B implements I {
        public boolean doWork() {
            return super.doWork();
        }
    }

    static class C3 extends B implements I {
        public boolean doWork() {
            return super.doWork();
        }
    }
}
