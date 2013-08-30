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
package org.openjdk.jmh.runner.options;

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author anders.astrand@oracle.com
 */
public class TestOptions {

    public static final String ORACLE_FORKED_MAIN_CLASS = "org.openjdk.jmh.ForkedMain";

    private static CommandLineOptions getOptions(String[] argv) throws Exception {
        CommandLineOptions opts = CommandLineOptions.newInstance();
        opts.parseArguments(argv);
        return opts;
    }

    @Test
    public void testThreadsMax() throws Exception {
        String[] argv = {"-t", "max"};
        CommandLineOptions options = getOptions(argv);
        assertEquals(0, options.getThreads());
    }

    @Test
    public void testThreadsWrong() throws Exception {
        String parameter = "maxx";
        String[] argv = {"-t", parameter};
        try {
            CommandLineOptions options = getOptions(argv);
        } catch (CmdLineException e) {
            assertTrue("Exception message did not contain parameter value", e.getMessage().contains(parameter));
            return;
        }

        fail("Did not get the expected CmdLineException when given a faulty -t parameter");
    }

    @Threads(100)
    @Measurement(iterations = 100, time = 3)
    @Fork(jvmArgs = "apa")
    public void dummyMethod() {
    }

    @Measurement(iterations = 100, time = 3)
    @Fork(jvmArgs = "apa")
    public void dummyMethod2() {
    }

    @Test
    public void testWarmupWithArg() throws Exception {
        String[] argv = {"-w", "100"};
        CommandLineOptions options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.SECONDS), options.getWarmupTime());

        argv = new String[]{"-w", "100ms"};
        options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.MILLISECONDS), options.getWarmupTime());

        argv = new String[]{"-w", "100us"};
        options = getOptions(argv);

        assertEquals(new TimeValue(100, TimeUnit.MICROSECONDS), options.getWarmupTime());

        try {
            argv = new String[]{"-w", "100ps"};
            options = getOptions(argv);
            fail("CmdLineException Expected");
        } catch (CmdLineException e) {
            // PASS
        }
    }

}
