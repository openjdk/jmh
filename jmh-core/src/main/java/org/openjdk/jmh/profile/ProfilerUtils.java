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
package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

public class ProfilerUtils {

    public static OptionSet parseInitLine(String initLine, OptionParser parser) throws ProfilerException {
        parser.accepts("help", "Display help.");

        OptionSpec<String> nonOptions = parser.nonOptions();

        String[] split = initLine.split(";");
        for (int c = 0; c < split.length; c++) {
            if (!split[c].isEmpty()) {
                split[c] = "-" + split[c];
            }
        }

        OptionSet set;
        try {
            set = parser.parse(split);
        } catch (OptionException e) {
            try {
                StringWriter sw = new StringWriter();
                sw.append(e.getMessage());
                sw.append("\n");
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            } catch (IOException e1) {
                throw new ProfilerException(e1);
            }
        }

        if (set.has("help")) {
            try {
                StringWriter sw = new StringWriter();
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String s = set.valueOf(nonOptions);
        if (s != null && !s.isEmpty()) {
            throw new ProfilerException("Unhandled options: " + s + " in " + initLine);
        }
        return set;
    }

    public static long measurementDelayMs(BenchmarkResult br) {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md != null) {
            // try to ask harness itself:
            return md.getMeasurementTime() - md.getStartTime();
        } else {
            // metadata is not available, let's make a guess:
            IterationParams wp = br.getParams().getWarmup();
            return wp.getCount() * wp.getTime().convertTo(TimeUnit.MILLISECONDS)
                    + TimeUnit.SECONDS.toMillis(1); // loosely account for the JVM lag
        }
    }

    public static long measuredTimeMs(BenchmarkResult br) {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md != null) {
            // try to ask harness itself:
            return md.getStopTime() - md.getMeasurementTime();
        } else {
            // metadata is not available, let's make a guess:
            IterationParams mp = br.getParams().getMeasurement();
            return mp.getCount() * mp.getTime().convertTo(TimeUnit.MILLISECONDS);
        }
    }
}
