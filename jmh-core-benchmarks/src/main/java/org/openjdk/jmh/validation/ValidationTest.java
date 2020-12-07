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
package org.openjdk.jmh.validation;

import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.validation.tests.BlackholeTestMode;

import java.io.PrintWriter;

public abstract class ValidationTest {

    public abstract void runWith(PrintWriter pw, Options parent) throws RunnerException;

    protected String blackholeModeString(BlackholeTestMode mode) {
        switch (mode) {
            case normal:
                return "DEFAULT";
            case compiler:
                return "COMPILER BLACKHOLE";
            case full_dontinline:
                return "FULL BLACKHOLE, NO INLINE";
            case full:
                return "FULL BLACKHOLE";
            default:
                throw new IllegalStateException("Unknown blackhole mode: " + mode);
        }
    }

    protected void blackholeModeMessage(PrintWriter pw, BlackholeTestMode mode) {
        switch (mode) {
            case normal:
                break;
            case compiler:
                org.openjdk.jmh.util.Utils.reflow(pw,
                        "This particular test mode enables the compiler-assisted blackholes. " +
                        "It should provide the most consistent performance across all types. " +
                        "This mode is only available in modern JDKs.",
                        80, 2);
                pw.println();
                break;
            case full_dontinline:
                org.openjdk.jmh.util.Utils.reflow(pw,
                        "This particular test mode omits the compiler-assisted blackholes. " +
                            "It should provide the basic level of safety for all JDKs.",
                        80, 2);
                pw.println();
                break;
            case full:
                org.openjdk.jmh.util.Utils.reflow(pw,
                        "This particular test mode forces the inline of Blackhole methods, and so demolishes two of the layers " +
                                "in defence in depth. If this layer is broken, Blackhole should also survive. If it isn't, then " +
                                "JMH will have to provide more contingencies.",
                        80, 2);
                pw.println();
                break;
            default:
                throw new IllegalStateException("Unknown blackhole mode: " + mode);
        }
    }

}
