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
package org.openjdk.jmh.results.format;

import org.openjdk.jmh.results.RunResult;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

public class ResultFormatFactory {

    private ResultFormatFactory() {}

    /**
     * Get the instance of ResultFormat of given type which writes the result to file
     * @param type result format type
     * @param file target file
     * @return result format
     */
    public static ResultFormat getInstance(final ResultFormatType type, final String file) {
        return new ResultFormat() {
            @Override
            public void writeOut(Collection<RunResult> results) {
                try {
                    PrintStream pw = new PrintStream(file, "UTF-8");
                    ResultFormat rf = getInstance(type, pw);
                    rf.writeOut(results);
                    pw.flush();
                    pw.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    /**
     * Get the instance of ResultFormat of given type which write the result to out.
     * It is a user responsibility to initialize and finish the out as appropriate.
     *
     * @param type result format type
     * @param out target out
     * @return result format.
     */
    public static ResultFormat getInstance(ResultFormatType type, PrintStream out) {
        switch (type) {
            case TEXT:
                return new TextResultFormat(out);
            case CSV:
                return new XSVResultFormat(out, ",");
            case SCSV:
                /*
                 *    Since some implementations, notably Excel, think it is a good
                 *    idea to hijack the CSV standard, and use semi-colon instead of
                 *    comma in some locales, this is the specialised
                 *     Semi-Colon Separated Values formatter.
                 */
                return new XSVResultFormat(out, ";");
            case JSON:
                return new JSONResultFormat(out);
            case LATEX:
                return new LaTeXResultFormat(out);
            default:
                throw new IllegalStateException("Unsupported result format: " + type);
        }
    }

}
