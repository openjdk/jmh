/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.profile.ClassloaderProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ClassloadProfilerTest {

    // Simplest Dummy class
    static final byte[] CLASS = new byte[] {
            (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
            0x00, 0x00, 0x00, 0x34,
            0x00, 0x0a, 0x0a, 0x00, 0x02, 0x00, 0x03, 0x07,
            0x00, 0x04, 0x0c, 0x00, 0x05, 0x00, 0x06, 0x01,
            0x00, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c,
            0x61, 0x6e, 0x67, 0x2f, 0x4f, 0x62, 0x6a, 0x65,
            0x63, 0x74, 0x01, 0x00, 0x06, 0x3c, 0x69, 0x6e,
            0x69, 0x74, 0x3e, 0x01, 0x00, 0x03, 0x28, 0x29,
            0x56, 0x07, 0x00, 0x08, 0x01, 0x00, 0x05, 0x44,
            0x75, 0x6d, 0x6d, 0x79, 0x01, 0x00, 0x04, 0x43,
            0x6f, 0x64, 0x65, 0x00, 0x21, 0x00, 0x07, 0x00,
            0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00,
            0x01, 0x00, 0x05, 0x00, 0x06, 0x00, 0x01, 0x00,
            0x09, 0x00, 0x00, 0x00, 0x11, 0x00, 0x01, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x05, 0x2a, (byte) 0xb7, 0x00,
            0x01, (byte) 0xb1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    static class MyClassLoader extends ClassLoader {
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals("Dummy")) {
                return defineClass(name, CLASS, 0, CLASS.length, null);
            } else {
                return super.loadClass(name);
            }
        }
    }

    @Benchmark
    public Class<?> work() throws Exception {
        Fixtures.work();
        ClassLoader loader = new MyClassLoader();
        return Class.forName("Dummy", true, loader);
    }

    @Test
    public void test() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .addProfiler(ClassloaderProfiler.class)
                .build();

        RunResult rr = new Runner(opts).runSingle();

        Map<String, Result> sr = rr.getSecondaryResults();
        double classLoad = sr.get("·class.load.norm").getScore();

        // Allow 5% slack
        if (Math.abs(1 - classLoad) > 0.05) {
            Assert.fail("Class loading rate is incorrect. Reported by profiler: " + classLoad);
        }
    }

}
