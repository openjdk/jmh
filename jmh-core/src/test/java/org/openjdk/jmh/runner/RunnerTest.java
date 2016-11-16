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
package org.openjdk.jmh.runner;

import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunnerTest {
    private static final String DUMMY_HOST = "host";
    private static final int DUMMY_PORT = 42;
    static Set<String> defaultHints = CompilerHints.fromFile(CompilerHints.hintsFile()).get();

    @Test(expected = IllegalArgumentException.class)
    public void testNullCheck() {
        new Runner(null);
    }

    @Test
    public void testEmptyOptsHaveCompileCommandFile() {
        Runner blade = new Runner(new OptionsBuilder());
        BenchmarkParams bp = new BenchmarkParams("Foo", "bar", false,
                1, new int[]{1}, Collections.<String>emptyList(),
                1, 1,
                new IterationParams(IterationType.WARMUP,      1, TimeValue.seconds(1), 1),
                new IterationParams(IterationType.MEASUREMENT, 1, TimeValue.seconds(1), 1),
                Mode.Throughput, null, TimeUnit.SECONDS, 1,
                Utils.getCurrentJvm(), Collections.<String>emptyList(),
                TimeValue.days(1));
        List<String> command = blade.getForkedMainCommand(bp, Collections.<ExternalProfiler>emptyList(), DUMMY_HOST, DUMMY_PORT);

        // expecting 1 compile command file
        List<String> files = CompilerHints.getCompileCommandFiles(command);
        assertEquals(1, files.size());

        // file should exist
        final String hintFileName = files.get(0);
        File hintsFile = new File(hintFileName);
        assertTrue(hintsFile.exists());

        // hints should be the default as none were specified
        Set<String> hints = CompilerHints.fromFile(hintFileName).get();
        assertEquals(hints, defaultHints);
    }

    @Test
    public void testOptsWithCompileCommandFileResultInMergedCompileCommandFile() throws IOException {
        // add a hints file
        String tempHints = FileUtils.createTempFileWithLines("fileWithLines",
                Collections.singletonList("inline,we/like/to/move/it.*"));
        Set<String> extraHints = CompilerHints.fromFile(tempHints).get();
        Runner blade = new Runner(new OptionsBuilder().build());
        BenchmarkParams bp = new BenchmarkParams("Foo", "bar", false,
                1, new int[]{1}, Collections.<String>emptyList(),
                1, 1,
                new IterationParams(IterationType.WARMUP,      1, TimeValue.seconds(1), 1),
                new IterationParams(IterationType.MEASUREMENT, 1, TimeValue.seconds(1), 1),
                Mode.Throughput, null, TimeUnit.SECONDS, 1,
                Utils.getCurrentJvm(), Collections.singletonList(CompilerHints.XX_COMPILE_COMMAND_FILE + tempHints),
                TimeValue.days(1));
        List<String> command = blade.getForkedMainCommand(bp, Collections.<ExternalProfiler>emptyList(), DUMMY_HOST, DUMMY_PORT);

        // expecting 1 compile command file
        List<String> files = CompilerHints.getCompileCommandFiles(command);
        assertEquals(1, files.size());

        // file should exist
        final String hintFileName = files.get(0);
        File hintsFile = new File(hintFileName);
        assertTrue(hintsFile.exists());

        // hints should include defaults and specified
        Set<String> hints = CompilerHints.fromFile(hintFileName).get();
        assertTrue(hints.containsAll(defaultHints));
        assertTrue(hints.containsAll(extraHints));
    }

    @Test
    public void testOptsWith2CompileCommandFilesResultInMergedCompileCommandFile() throws IOException {
        // add hints files
        String tempHints1 = FileUtils.createTempFileWithLines("fileWithLines",
                Collections.singletonList("inline,we/like/to/move/it/move/it.*"));
        Set<String> extraHints1 = CompilerHints.fromFile(tempHints1).get();
        String tempHints2 = FileUtils.createTempFileWithLines("fileWithLines",
                Collections.singletonList("inline,we/like/to/move/it.*"));
        Set<String> extraHints2 = CompilerHints.fromFile(tempHints2).get();
        Runner blade = new Runner(new OptionsBuilder().build());
        BenchmarkParams bp = new BenchmarkParams("Foo", "bar", false,
                1, new int[]{1}, Collections.<String>emptyList(),
                1, 1,
                new IterationParams(IterationType.WARMUP,      1, TimeValue.seconds(1), 1),
                new IterationParams(IterationType.MEASUREMENT, 1, TimeValue.seconds(1), 1),
                Mode.Throughput, null, TimeUnit.SECONDS, 1,
                Utils.getCurrentJvm(),
                Arrays.asList(CompilerHints.XX_COMPILE_COMMAND_FILE + tempHints1, CompilerHints.XX_COMPILE_COMMAND_FILE + tempHints2),
                TimeValue.days(1));
        List<String> command = blade.getForkedMainCommand(bp, Collections.<ExternalProfiler>emptyList(), DUMMY_HOST, DUMMY_PORT);

        // expecting 1 compile command file
        List<String> files = CompilerHints.getCompileCommandFiles(command);
        assertEquals(1, files.size());

        // file should exist
        final String hintFileName = files.get(0);
        File hintsFile = new File(hintFileName);
        assertTrue(hintsFile.exists());

        // hints should include defaults and specified
        Set<String> hints = CompilerHints.fromFile(hintFileName).get();
        assertTrue(hints.containsAll(defaultHints));
        assertTrue(hints.containsAll(extraHints1));
        assertTrue(hints.containsAll(extraHints2));
    }
}
