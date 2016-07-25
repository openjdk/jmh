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
package org.openjdk.jmh.runner.options;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import org.openjdk.jmh.util.Utils;

import java.util.List;
import java.util.Map;

public class OptionFormatter implements HelpFormatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public String format(Map<String, ? extends OptionDescriptor> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: java -jar ... [regexp*] [options]");
        sb.append("\n");
        sb.append(" [opt] means optional argument.\n");
        sb.append(" <opt> means required argument.\n");
        sb.append(" \"+\" means comma-separated list of values.\n");
        sb.append(" \"time\" arguments accept time suffixes, like \"100ms\".\n");
        sb.append("\n");
        sb.append("Command line options usually take precedence over annotations.");
        sb.append("\n");
        sb.append("\n");
        for (OptionDescriptor each : options.values()) {
            sb.append(lineFor(each));
        }

        return sb.toString();
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

        final int optWidth = 30;

        line.append(String.format("%-" + optWidth + "s", o.toString()));
        boolean first = true;
        String desc = d.description();
        List<?> defaults = d.defaultValues();
        if (defaults != null && !defaults.isEmpty()) {
            desc += " (default: " + defaults.toString() + ")";
        }
        for (String l : Utils.rewrap(desc)) {
            if (first) {
                first = false;
            } else {
                line.append(LINE_SEPARATOR);
                line.append(String.format("%-" + optWidth + "s", ""));
            }
            line.append(l);
        }

        line.append(LINE_SEPARATOR);
        line.append(LINE_SEPARATOR);
        return line.toString();
    }

}
