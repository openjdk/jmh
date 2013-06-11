/**
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
package org.openjdk.jmh.runner.options.handlers;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.concurrent.TimeUnit;

/**
 * OptionHandler for TimeValue options.
 */
public class TimeUnitOptionHandler extends OptionHandler<TimeUnit> {

    /**
     * Constructor
     *
     * @param parser CmdLineParser parent
     * @param option Run-time copy of the Option
     * @param setter Setter to feed back the value
     */
    public TimeUnitOptionHandler(CmdLineParser parser, OptionDef option, Setter<TimeUnit> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        if (params.size() > 0) {
            String param = params.getParameter(0).toLowerCase();
            try {
                setter.addValue(fromString(param));
                return 1;
            } catch (Exception e) {
                throw new CmdLineException(owner, param + "; unknown time format.");
            }
        }
        return 0;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "TIME";
    }

    public static TimeUnit fromString(String timeString) {
        if (timeString == null) {
            return null;
        }
        if ("ns".equalsIgnoreCase(timeString)) {
            return TimeUnit.NANOSECONDS;
        }
        if ("ms".equalsIgnoreCase(timeString)) {
            return TimeUnit.MILLISECONDS;
        }
        if ("us".equalsIgnoreCase(timeString)) {
            return TimeUnit.MICROSECONDS;
        }
        if ("s".equalsIgnoreCase(timeString)) {
            return TimeUnit.SECONDS;
        }
        if ("m".equalsIgnoreCase(timeString)) {
            return TimeUnit.MINUTES;
        }
        throw new IllegalArgumentException("unknown timeunit format: " + timeString);
    }

   public static String toString(TimeUnit timeUnit) {
        switch(timeUnit) {
            case SECONDS:
                return "s";
            case MILLISECONDS:
                return "ms";
            case MICROSECONDS:
                return "us";
            case NANOSECONDS:
                return "ns";
            case MINUTES:
                return "m";
            default:
                return "s";
        }
    }

}
