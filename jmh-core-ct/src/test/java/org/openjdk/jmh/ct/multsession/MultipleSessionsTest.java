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
package org.openjdk.jmh.ct.multsession;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.ct.InMemoryGeneratorDestination;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.reflection.RFGeneratorSource;
import org.openjdk.jmh.runner.BenchmarkList;

public class MultipleSessionsTest {

    @Test
    public void testAppend() {
        InMemoryGeneratorDestination dst = new InMemoryGeneratorDestination();

        {
            RFGeneratorSource src = new RFGeneratorSource();
            BenchmarkGenerator gen = new BenchmarkGenerator();

            src.processClasses(Benchmark1.class);

            gen.generate(src, dst);
            gen.complete(src, dst);

            Assert.assertFalse("First stage error", dst.hasErrors());
            Assert.assertFalse("First stage warnings", dst.hasWarnings());
            Assert.assertFalse("First stage infos", dst.hasNotes());

            String[] list = dst.getResources().get(BenchmarkList.BENCHMARK_LIST.substring(1)).split("\n");
            Assert.assertEquals("First stage should have only 1 benchmark", 1, list.length);
        }

        {
            RFGeneratorSource src = new RFGeneratorSource();
            BenchmarkGenerator gen = new BenchmarkGenerator();

            src.processClasses(Benchmark2.class);

            gen.generate(src, dst);
            gen.complete(src, dst);

            Assert.assertFalse("Second stage error", dst.hasErrors());
            Assert.assertFalse("Second stage warnings", dst.hasWarnings());
            Assert.assertFalse("Second stage notes", dst.hasNotes());

            String[] list = dst.getResources().get(BenchmarkList.BENCHMARK_LIST.substring(1)).split("\n");
            Assert.assertEquals("Second stage should have 2 benchmarks", 2, list.length);
        }
    }

    @Test
    public void testOverwrite() {
        InMemoryGeneratorDestination dst = new InMemoryGeneratorDestination();

        {
            RFGeneratorSource src = new RFGeneratorSource();
            BenchmarkGenerator gen = new BenchmarkGenerator();

            src.processClasses(Benchmark1.class);

            gen.generate(src, dst);
            gen.complete(src, dst);

            Assert.assertFalse("First stage error", dst.hasErrors());
            Assert.assertFalse("First stage warnings", dst.hasWarnings());
            Assert.assertFalse("First stage notes", dst.hasNotes());

            String[] list = dst.getResources().get(BenchmarkList.BENCHMARK_LIST.substring(1)).split("\n");
            Assert.assertEquals("First stage should have only 1 benchmark", 1, list.length);
        }

        {
            RFGeneratorSource src = new RFGeneratorSource();
            BenchmarkGenerator gen = new BenchmarkGenerator();

            src.processClasses(Benchmark1.class);

            gen.generate(src, dst);
            gen.complete(src, dst);

            Assert.assertFalse("Second stage error", dst.hasErrors());
            Assert.assertFalse("Second stage warnings", dst.hasWarnings());
            Assert.assertTrue("Second stage notes", dst.hasNotes());
            boolean hasOurInfo = false;
            for (String warning : dst.getNotes()) {
                hasOurInfo |= (warning.contains("Benchmark1") && warning.contains("overwriting"));
            }
            Assert.assertTrue("Should have our note: " + dst.getNotes(), hasOurInfo);

            String[] list = dst.getResources().get(BenchmarkList.BENCHMARK_LIST.substring(1)).split("\n");
            Assert.assertEquals("Second stage should have 1 benchmark", 1, list.length);
        }
    }

}
