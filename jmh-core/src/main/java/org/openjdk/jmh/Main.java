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
package org.openjdk.jmh;

import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.IOException;

/**
 * Main program entry point
 */
public class Main {

    public static void main(String[] argv) throws RunnerException, IOException {
        try {
            CommandLineOptions cmdOptions = new CommandLineOptions(argv);

            Runner runner = new Runner(cmdOptions);

            if (cmdOptions.shouldHelp()) {
                cmdOptions.showHelp();
                return;
            }

            if (cmdOptions.shouldList()) {
                runner.list();
                return;
            }

            if (cmdOptions.shouldListWithParams()) {
                runner.listWithParams(cmdOptions);
                return;
            }

            if (cmdOptions.shouldListProfilers()) {
                cmdOptions.listProfilers();
                return;
            }

            if (cmdOptions.shouldListResultFormats()) {
                cmdOptions.listResultFormats();
                return;
            }

            try {
                runner.run();
            } catch (NoBenchmarksException e) {
                System.err.println("No matching benchmarks. Miss-spelled regexp?");

                if (cmdOptions.verbosity().orElse(Defaults.VERBOSITY) != VerboseMode.EXTRA) {
                    System.err.println("Use " + VerboseMode.EXTRA + " verbose mode to debug the pattern matching.");
                } else {
                    runner.list();
                }
                System.exit(1);
            } catch (ProfilersFailedException e) {
                // This is not exactly an error, set non-zero exit code
                System.err.println(e.getMessage());
                System.exit(1);
            } catch (RunnerException e) {
                System.err.print("ERROR: ");
                e.printStackTrace(System.err);
                System.exit(1);
            }

        } catch (CommandLineOptionException e) {
            System.err.println("Error parsing command line:");
            System.err.println(" " + e.getMessage());
            System.exit(1);
        }
    }

}
