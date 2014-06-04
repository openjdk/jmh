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
package org.openjdk.jmh.runner.format;

import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.PrintStream;

public class OutputFormatFactory {

    /**
     * Factory method for OutputFormat instances
     *
     * @param out  output stream to use
     * @param mode how much verbosity to use
     * @return a new OutputFormat instance of given type
     */
    public static OutputFormat createFormatInstance(PrintStream out, VerboseMode mode) {
        switch (mode) {
            case SILENT:
                return new SilentFormat(out, mode);
            case NORMAL:
            case EXTRA:
                return new TextReportFormat(out, mode);
            default:
                throw new IllegalArgumentException("Mode " + mode + " not found!");
        }
    }

}
