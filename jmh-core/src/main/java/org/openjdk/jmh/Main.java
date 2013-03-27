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
package org.openjdk.jmh;

import org.kohsuke.args4j.CmdLineException;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.runner.MicroBenchmarkList;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.HarnessOptions;

/**
 * Main program entry point
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com
 */
public class Main {

    /**
     * Application main entry point
     *
     * @param argv Command line arguments
     */
    public static void main(String[] argv) {
        HarnessOptions cmdOptions = HarnessOptions.newInstance();

        if (argv.length == 0) {
            cmdOptions.printUsage("Print help");
        } else {
            try {
                cmdOptions.parseArguments(argv);

                // list output formats?
                if (cmdOptions.shouldListFormats()) {
                    StringBuilder sb = new StringBuilder();

                    for (OutputFormatType f : OutputFormatType.values()) {
                        sb.append(f.toString().toLowerCase());
                        sb.append(", ");
                    }
                    sb.setLength(sb.length() - 2);

                    System.out.println("Available formats: " + sb.toString());
                    return;
                }

                // list available profilers?
                if (cmdOptions.shouldListProfilers()) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : ProfilerFactory.getAvailableProfilers()) {
                        if (ProfilerFactory.isSupported(s)) {
                            sb.append(String.format("%10s: %s\n", s, ProfilerFactory.getDescription(s)));
                        }
                    }
                    if (!sb.toString().isEmpty()) {
                        System.out.println("Supported profilers:\n" + sb.toString());
                    }

                    sb = new StringBuilder();
                    for (String s : ProfilerFactory.getAvailableProfilers()) {
                        if (!ProfilerFactory.isSupported(s)) {
                            sb.append(String.format("%10s: %s\n", s, ProfilerFactory.getDescription(s)));
                        }
                    }

                    if (!sb.toString().isEmpty()) {
                        System.out.println("Unsupported profilers:\n" + sb.toString());
                    }
                    return;
                }

                if (cmdOptions.shouldHelp()) {
                    cmdOptions.printUsage("Displaying help");
                    return;
                }

                Runner runner = new Runner(cmdOptions);
                runner.run(MicroBenchmarkList.defaultList());
            } catch (CmdLineException ex) {
                cmdOptions.printUsage(ex.getMessage());
            } catch (RunnerException e) {
                cmdOptions.printUsage(e.getMessage());
            }
        }
    }

    /**
     * Additional entry points for integration tests
     * @param args non-exploded command line arguments
     */
    public static void testMain(String args) {
        main(args.split(" "));
    }

}
