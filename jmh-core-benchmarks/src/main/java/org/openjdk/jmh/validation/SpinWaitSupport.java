/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmh.validation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class SpinWaitSupport {

    // All critical code should fold with C2 compilation.
    // This provides us with JDK 8 compatibility.

    static final MethodHandle MH;

    static {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findStatic(Thread.class, "onSpinWait", MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            mh = null;
        }
        MH = mh;
    }

    public static boolean available() {
        return MH != null;
    }

    public static void onSpinWait() {
        if (MH != null) {
            try {
                MH.invokeExact();
            } catch (Throwable e) {
                throw new IllegalStateException("Should not happen", e);
            }
        }
    }

}
