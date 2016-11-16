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
package org.openjdk.jmh.validation;

import joptsimple.*;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.util.Version;
import org.openjdk.jmh.validation.tests.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws RunnerException, CommandLineOptionException, IOException {
        PrintWriter pw = new PrintWriter(System.out, true);

        pw.println("JMH Core Benchmarks, Validation Tests");
        pw.println("----------------------------------------------------------------------------------------------------------");
        pw.println();

        pw.println("# " + Version.getVersion());
        pw.println("# " + Utils.getCurrentJvmVersion());
        pw.println("# " + Utils.getCurrentOSVersion());
        pw.println();

        Utils.reflow(pw,
                "These tests assess the current benchmarking environment health, including hardware, OS, JVM, and JMH " +
                        "itself. While the failure on these tests does not immediately means the problem with environment, " +
                        "it is instructive to understand and follow up on oddities in these tests.",
                80, 2);
        pw.println();

        Utils.reflow(pw,
                "If you are sharing this report, please share it in full, including the JVM version, OS flavor and version, " +
                        "plus some data on used hardware.",
                80, 2);

        pw.println();

        pw.println("  Use -h to get help on available options.");
        pw.println();

        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new OptionFormatter());

        OptionSpec<Test> optTests = parser.accepts("t", "Test names.")
                .withRequiredArg().ofType(Test.class).withValuesSeparatedBy(',').describedAs("string")
                .defaultsTo(Test.values());

        OptionSpec<Mode> optMode = parser.accepts("m", "Running mode, one of " + Arrays.toString(Mode.values()) + ".")
                .withRequiredArg().ofType(Mode.class).describedAs("mode")
                .defaultsTo(Mode.normal);

        parser.accepts("h", "Print help.");

        List<Test> tests;
        Mode mode;
        try {
            OptionSet set = parser.parse(args);

            if (set.has("h")) {
                parser.printHelpOn(System.out);
                return;
            }

            tests = set.valuesOf(optTests);
            mode = set.valueOf(optMode);
        } catch (OptionException e) {
            String message = e.getMessage();
            Throwable cause = e.getCause();
            if (cause instanceof ValueConversionException) {
                message += ". " + cause.getMessage();
            }
            throw new CommandLineOptionException(message, e);
        }

        Options opts = new OptionsBuilder()
                .detectJvmArgs()
                .jvmArgsAppend("-Xmx512m", "-Xms512m", "-server")
                .build();

        switch (mode) {
            case flash:
                opts = new OptionsBuilder()
                        .parent(opts)
                        .warmupIterations(3)
                        .warmupTime(TimeValue.milliseconds(10))
                        .measurementIterations(3)
                        .measurementTime(TimeValue.milliseconds(10))
                        .forks(1)
                        .build();
                break;
            case quick:
                opts = new OptionsBuilder()
                        .parent(opts)
                        .warmupIterations(3)
                        .warmupTime(TimeValue.milliseconds(100))
                        .measurementIterations(3)
                        .measurementTime(TimeValue.milliseconds(100))
                        .forks(3)
                        .build();
                break;
            case normal:
                opts = new OptionsBuilder()
                        .parent(opts)
                        .warmupIterations(5)
                        .warmupTime(TimeValue.milliseconds(500))
                        .measurementIterations(5)
                        .measurementTime(TimeValue.milliseconds(500))
                        .forks(5)
                        .build();
                break;
            case longer:
                opts = new OptionsBuilder()
                        .parent(opts)
                        .warmupIterations(10)
                        .warmupTime(TimeValue.seconds(1))
                        .measurementIterations(10)
                        .measurementTime(TimeValue.seconds(1))
                        .forks(10)
                        .build();
                break;
            default:
                throw new IllegalStateException();
        }

        for (Test t : tests) {
            switch (t) {
                case timing:
                    new TimingMeasurementsTest().runWith(pw, opts);
                    break;
                case stability:
                    new ScoreStabilityTest().runWith(pw, opts);
                    break;
                case compiler_hints:
                    new CompilerHintsTest().runWith(pw, opts);
                    break;
                case thermal:
                    switch (mode) {
                        case flash:
                            new ThermalRundownTest(3).runWith(pw, opts);
                            break;
                        case quick:
                            new ThermalRundownTest(5).runWith(pw, opts);
                            break;
                        case normal:
                            new ThermalRundownTest(18).runWith(pw, opts);
                            break;
                        case longer:
                            new ThermalRundownTest(60).runWith(pw, opts);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    break;
                case helpers:
                    new HelperMethodsTest().runWith(pw, opts);
                    break;
                case thread_scale:
                    new ThreadScalingTest().runWith(pw, opts);
                    break;
                case blackhole_cpu:
                    new BlackholeConsumeCPUTest().runWith(pw, opts);
                    break;
                case blackhole_single:
                    new BlackholeSingleTest().runWith(pw, opts);
                    break;
                case blackhole_pipelined:
                    setBlackholeInline(false);
                    new BlackholePipelinedTest(false, false).runWith(pw, opts);
                    new BlackholePipelinedTest(true, false).runWith(pw, opts);
                    setBlackholeInline(true);
                    new BlackholePipelinedTest(false, true).runWith(pw, opts);
                    new BlackholePipelinedTest(true, true).runWith(pw, opts);
                    setBlackholeInline(false);
                    break;
                case blackhole_consec:
                    setBlackholeInline(false);
                    new BlackholeConsecutiveTest(false).runWith(pw, opts);
                    setBlackholeInline(true);
                    new BlackholeConsecutiveTest(true).runWith(pw, opts);
                    setBlackholeInline(false);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public enum Test {
        timing,
        compiler_hints,
        thermal,
        stability,
        thread_scale,
        helpers,
        blackhole_cpu,
        blackhole_single,
        blackhole_pipelined,
        blackhole_consec,
    }

    public enum Mode {
        flash,
        quick,
        normal,
        longer,
    }

    private static void setBlackholeInline(boolean inline) {
        System.getProperties().setProperty("jmh.blackhole.forceInline", String.valueOf(inline));

        try {
            Field f = CompilerHints.class.getDeclaredField("hintsFile");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
