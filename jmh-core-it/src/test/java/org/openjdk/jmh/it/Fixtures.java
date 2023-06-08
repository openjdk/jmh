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
package org.openjdk.jmh.it;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

public class Fixtures {

    private static final int REPS;
    private static final String PROFILE;

    static {
        REPS = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            public Integer run() {
                return Integer.getInteger("jmh.it.reps", 1);
            }
        });
        PROFILE = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("jmh.core.it.profile");
            }
        });
    }

    public static int repetitionCount() {
        return REPS;
    }

    public static String getTestMask(Class<?> klass) {
        return klass.getCanonicalName();
    }

    public static void work() {
        // courtesy for parallel-running tests
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean expectStableThreads() {
        return isDefaultProfile();
    }

    public static boolean isDefaultProfile() {
        return PROFILE.equals("default");
    }

    public static boolean isVirtualExecutor() {
        return PROFILE.contains("executor-virtual");
    }
}
