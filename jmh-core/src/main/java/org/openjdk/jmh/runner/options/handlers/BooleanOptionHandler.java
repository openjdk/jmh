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
import org.openjdk.jmh.runner.parameters.Defaults;

/**
 * OptionHandler for boolean options.
 * <p/>
 * Will set to TRUE if: - Option is called with -<option> true - Option is called alone: -<option>
 * <p/>
 * Will set to FALSE if: - Option is called with anything but true: -<option> <anything else>
 *
 * @author sergey.kuksenko@oracle.com
 */
public class BooleanOptionHandler extends OptionHandler<Boolean> {

    /**
     * Constructor
     *
     * @param parser CmdLineParser parent
     * @param option Run-time copy of the Option
     * @param setter Setter to feed back the value
     */
    public BooleanOptionHandler(CmdLineParser parser, OptionDef option, Setter<Boolean> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        if (params.size() > 0) {
            String param = params.getParameter(0).toLowerCase();
            if(Defaults.TRUE_VALUES.contains(param)) {
                setter.addValue(true);
                return 1;
            }
            if(Defaults.FALSE_VALUES.contains(param)) {
                setter.addValue(false);
                return 1;
            }
        }
        setter.addValue(true);
        return 0;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "BOOL";
    }
}
