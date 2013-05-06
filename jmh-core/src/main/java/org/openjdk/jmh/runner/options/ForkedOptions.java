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
package org.openjdk.jmh.runner.options;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Class that handles options and arguments for forked JVM
 *
 * @author sergey.kuksenko@oracle.com
 */
public class ForkedOptions extends BaseOptions {


    @Argument(metaVar = "benchmark", usage = "Microbenchmarks to run")
    protected String benchmark = null;

    @Option(name = "--hostName", metaVar = "String", usage = "The IP address of host JVM")
    protected String hostName = null;

    @Option(name = "--hostPort", metaVar = "INT", usage = "The IP port of host JVM")
    protected int hostPort = 0;

    /**
     * Kawaguchi's parser
     */
    private CmdLineParser parser;

    public static ForkedOptions newInstance() {
        ForkedOptions opts = new ForkedOptions();
        opts.parser = new CmdLineParser(opts);
        return opts;
    }

    private ForkedOptions() {
    }

    /**
     * parse arguments and set fields in the Options instance
     *
     * @throws org.kohsuke.args4j.CmdLineException
     *
     */
    public void parseArguments(String[] argv) throws CmdLineException {
        parser.parseArgument(argv);
    }

    public String getBenchmark() {
        return benchmark;
    }

    public int getHostPort() {
        return hostPort;
    }

    public String getHostName() {
        return hostName;
    }
}
