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

import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Version {

    public static void printVersion(OutputFormat pw) {
        Properties p = new Properties();
        InputStream s = Version.class.getResourceAsStream("/jmh.properties");
        String time = null;
        if (s != null) {
            try {
                p.load(s);
                time = (String) p.get("jmh.buildTime");

                Date parse = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(time);
                long diff = (System.currentTimeMillis() - parse.getTime()) / TimeUnit.DAYS.toMillis(1);

                pw.print("# JMH " + p.get("jmh.version") + " (released ");
                if (diff > 0) {
                    pw.print(String.format("%d days ago", diff));
                    if (diff > 30) {
                        pw.print(", please consider updating!");
                    }
                } else {
                    pw.print("today");
                }
                pw.println(")");
            } catch (IOException e) {
                pw.println("# Can not figure out JMH version");
            } catch (ParseException e) {
                pw.println("# Can not figure out JMH version, unable to parse the build time: " + time);
            }
        } else {
            pw.println("# Can not figure out JMH version, no jmh.properties");
        }
    }

}
