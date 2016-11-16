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
package org.openjdk.jmh.util;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestClassUtils {

    @Test
    public void testDenseClasses1() {
        List<String> src = Arrays.asList("org.openjdk.benches.ForkJoinTest.test1", "org.openjdk.benches.ForkJoinTest.test2", "org.openjdk.benches.AnotherTest.test0");
        Map<String,String> map = ClassUtils.denseClassNames(src);

        Assert.assertEquals("ForkJoinTest.test1", map.get("org.openjdk.benches.ForkJoinTest.test1"));
        Assert.assertEquals("ForkJoinTest.test2", map.get("org.openjdk.benches.ForkJoinTest.test2"));
    }

    @Test
    public void testDenseClasses2() {
        List<String> src = Collections.singletonList("org.openjdk.benches.ForkJoinTest.test1");
        Map<String,String> map = ClassUtils.denseClassNames(src);

        Assert.assertEquals("ForkJoinTest.test1", map.get("org.openjdk.benches.ForkJoinTest.test1"));
    }

    @Test
    public void testDenseClasses3() {
        List<String> src = Collections.singletonList("org.openjdk.benches.ForkJoinTest.test1:label1");
        Map<String,String> map = ClassUtils.denseClassNames(src);

        Assert.assertEquals("ForkJoinTest.test1:label1", map.get("org.openjdk.benches.ForkJoinTest.test1:label1"));
    }

    @Test
    public void testDifferentPackages() {
        List<String> src = Arrays.asList("com.some.even.longer.word.SomeEvenMoreWeirdBenchmark", "my.sample.pkg.MySampleBenchmark", "test.jmh.TestJmhBenchmark");
        Map<String,String> map = ClassUtils.denseClassNames(src);

        Assert.assertEquals("com.some.even.longer.word.SomeEvenMoreWeirdBenchmark", map.get("com.some.even.longer.word.SomeEvenMoreWeirdBenchmark"));
        Assert.assertEquals("my.sample.pkg.MySampleBenchmark", map.get("my.sample.pkg.MySampleBenchmark"));
        Assert.assertEquals("test.jmh.TestJmhBenchmark", map.get("test.jmh.TestJmhBenchmark"));
    }

    @Test
    public void testSemiDifferentPackages() {
        List<String> src = Arrays.asList("com.some.Benchmark", "com.some2.Benchmark2");
        Map<String,String> map = ClassUtils.denseClassNames(src);

        Assert.assertEquals("c.some.Benchmark", map.get("com.some.Benchmark"));
        Assert.assertEquals("c.some2.Benchmark2", map.get("com.some2.Benchmark2"));
    }


}
