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
package org.openjdk.jmh.runner.options;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class OptionFormatter implements HelpFormatter {

    public String format(Map<String, ? extends OptionDescriptor> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: java -jar ... [regexp*] [options]");
        sb.append("\n");
        sb.append(" [opt] means optional argument.\n");
        sb.append(" <opt> means required argument.\n");
        sb.append(" \"+\" means comma-separated list of values.\n");
        sb.append(" \"time\" arguments accept time suffixes, like \"100ms\".\n");
        sb.append("\n");
        for (OptionDescriptor each : options.values()) {
            sb.append(lineFor(each));
        }

        return sb.toString();
    }

    private Collection<String> rewrap(String lines) {
        Collection<String> result = new ArrayList<String>();
        String[] words = lines.split("[ \n]");
        String line = "";
        int cols = 0;
        for (String w : words) {
            cols += w.length();
            line += w + " ";
            if (cols > 40) {
                result.add(line);
                line = "";
                cols = 0;
            }
        }
        result.add(line);
        return result;
    }

    private String lineFor(OptionDescriptor d) {
        StringBuilder line = new StringBuilder();

        StringBuilder o = new StringBuilder();
        o.append("  ");
        for (String str : d.options()) {
            if (!d.representsNonOptions()) {
                o.append("-");
            }
            o.append(str);
            if (d.acceptsArguments()) {
                o.append(" ");
                if (d.requiresArgument()) {
                    o.append("<");
                } else {
                    o.append("[");
                }
                o.append(d.argumentDescription());
                if (d.requiresArgument()) {
                    o.append(">");
                } else {
                    o.append("]");
                }
            }
        }

        line.append(String.format("%-30s", o.toString()));
        boolean first = true;
        for (String l : rewrap(d.description())) {
            if (first) {
                first = false;
            } else {
                line.append("\n");
                line.append(String.format("%-30s", ""));
            }
            line.append(l);
        }

        line.append(System.getProperty("line.separator"));
        return line.toString();
    }

}
