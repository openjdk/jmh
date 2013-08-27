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
package org.openjdk.jmh.output;

import org.openjdk.jmh.output.format.CsvFormat;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.output.format.PrettyPrintFormat;
import org.openjdk.jmh.output.format.SilentFormat;
import org.openjdk.jmh.output.format.TextReportFormat;
import org.openjdk.jmh.link.BinaryLinkClient;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Proxy;

public class OutputFormatFactory {

    /**
     * Factory method for OutputFormat instances
     *
     * @param format Format enum to use
     * @return a new OutputFormat instance of given type
     */
    public static OutputFormat createFormatInstance(PrintStream out, OutputFormatType format, boolean verbose) {
        assert (format != null) : "format is not null";

        switch (format) {
            case Pretty:
                return new PrettyPrintFormat(out, verbose);
            case CSV:
                return new CsvFormat(out, verbose);
            case Silent:
                return new SilentFormat(out, verbose);
            case TextReport:
                return new TextReportFormat(out, verbose);
            default:
                throw new IllegalArgumentException("Format: " + format + " not found!");
        }
    }

    /**
     * Factory method - returns output format for forked JVM.
     * @return
     */
    public static OutputFormat createBinaryHook(BinaryLinkClient link) {
        return (OutputFormat) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{OutputFormat.class},
                link.getOutputFormatHandler()
        );
    }

    public static OutputFormat createFormatInstance(boolean verbose) {
        return createFormatInstance(System.out, OutputFormatType.Pretty, verbose);
    }

}
