/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Version {

    private static final int UPDATE_INTERVAL = 180;

    public static String getVersion() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        printVersion(pw);
        pw.close();
        return sw.toString();
    }

    private static void printVersion(PrintWriter pw) {
        Properties p = new Properties();
        InputStream s = Version.class.getResourceAsStream("/jmh.properties");

        if (s == null) {
            pw.print("Cannot figure out JMH version, no jmh.properties");
            return;
        }

        try {
            p.load(s);
        } catch (IOException e) {
            pw.print("Cannot figure out JMH version");
            return;
        } finally {
            FileUtils.safelyClose(s);
        }

        String version = (String) p.get("jmh.version");
        if (version == null) {
            pw.print("Cannot read jmh.version");
            return;
        }

        pw.print("JMH " + version + " ");

        String time = (String) p.get("jmh.buildDate");
        if (time == null) {
            pw.print("(cannot read jmh.buildDate)");
            return;
        }

        pw.print("(released ");
        try {
            Date parse = new SimpleDateFormat("yyyy/MM/dd", Locale.ROOT).parse(time);
            long diff = (System.currentTimeMillis() - parse.getTime()) / TimeUnit.DAYS.toMillis(1);
            if (diff > 0) {
                pw.print(String.format("%d days ago", diff));
                if (diff > UPDATE_INTERVAL) {
                    pw.print(", please consider updating!");
                }
            } else {
                pw.print("today");
            }
        } catch (ParseException e) {
            pw.print(time);
        }
        pw.print(")");
    }

}
