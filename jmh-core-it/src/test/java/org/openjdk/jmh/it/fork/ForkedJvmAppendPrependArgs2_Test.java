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
package org.openjdk.jmh.it.fork;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.it.Fixtures;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.All)
@Fork(jvmArgsAppend = "-DappendedUp", jvmArgsPrepend = "-DprependedUp")
public class ForkedJvmAppendPrependArgs2_Test {

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgs = "-Dreplaced", jvmArgsAppend = "-Dappended", jvmArgsPrepend = "-Dprepended")
    public void test1() {
        Fixtures.work();
        Assert.assertNotNull(System.getProperty("replaced"));
        Assert.assertNull(System.getProperty("appended"));
        Assert.assertNull(System.getProperty("prepended"));
        Assert.assertNull(System.getProperty("appendedUp"));
        Assert.assertNull(System.getProperty("prependedUp"));
    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgsAppend = "-Dappended", jvmArgsPrepend = "-Dprepended")
    public void test2() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("replaced"));
        Assert.assertNotNull(System.getProperty("appended"));
        Assert.assertNotNull(System.getProperty("prepended"));
        Assert.assertNull(System.getProperty("appendedUp"));
        Assert.assertNull(System.getProperty("prependedUp"));
    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgsPrepend = "-Dprepended")
    public void test3() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("replaced"));
        Assert.assertNull(System.getProperty("appended"));
        Assert.assertNotNull(System.getProperty("prepended"));
        Assert.assertNotNull(System.getProperty("appendedUp"));
        Assert.assertNull(System.getProperty("prependedUp"));

    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(jvmArgsAppend = "-Dappended")
    public void test4() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("replaced"));
        Assert.assertNotNull(System.getProperty("appended"));
        Assert.assertNull(System.getProperty("prepended"));
        Assert.assertNull(System.getProperty("appendedUp"));
        Assert.assertNotNull(System.getProperty("prependedUp"));

    }

    @GenerateMicroBenchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork()
    public void test5() {
        Fixtures.work();
        Assert.assertNull(System.getProperty("replaced"));
        Assert.assertNull(System.getProperty("appended"));
        Assert.assertNull(System.getProperty("prepended"));
        Assert.assertNotNull(System.getProperty("appendedUp"));
        Assert.assertNotNull(System.getProperty("prependedUp"));

    }

    @Test
    public void invoke() {
        Main.testMain(Fixtures.getTestMask(this.getClass()) + " -foe");
    }

}
