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
package org.openjdk.jmh.it.profilers;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class AsyncProfilerTest extends AbstractHotspotProfilerTest {

    private Map<String, Result> run(String mode) throws RunnerException {
        try {
            new AsyncProfiler("");
        } catch (ProfilerException e) {
            Assume.assumeNoException("Could not load async-profiler", e);
        }

        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(AsyncProfiler.class, "output=" + mode)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        return rr.getSecondaryResults();
    }

    @Test
    public void text() throws RunnerException {
        Map<String, Result> sr = run("text");

        Collection<? extends File> files = ((AsyncProfiler.FileResult) sr.get("async-summary")).getFiles();
        Assert.assertEquals(1, files.size());
        File single = files.iterator().next();
        Assert.assertTrue(single.length() > 10);

        TextResult text = (TextResult) sr.get("async-text");
        Assert.assertTrue(text.extendedInfo().length() > 10);
    }

    @Test
    public void collapsed() throws RunnerException {
        Map<String, Result> sr = run("collapsed");

        Collection<? extends File> files = ((AsyncProfiler.FileResult) sr.get("async-collapsed")).getFiles();
        Assert.assertEquals(1, files.size());
        File single = files.iterator().next();
        Assert.assertTrue(single.length() > 10);
    }

    @Test
    public void flamegraph() throws RunnerException {
        Map<String, Result> sr = run("flamegraph");

        Collection<? extends File> files = ((AsyncProfiler.FileResult) sr.get("async-flamegraph")).getFiles();
        Assert.assertEquals(2, files.size());
        for (File file : files) {
            Assert.assertTrue(file.length() > 10);
        }
    }

    @Test
    public void jfr() throws RunnerException {
        Map<String, Result> sr = run("jfr");
        Collection<? extends File> files = ((AsyncProfiler.FileResult) sr.get("async-jfr")).getFiles();
        Assert.assertEquals(1, files.size());
        File single = files.iterator().next();
        Assert.assertTrue(single.length() > 10);
    }
}
