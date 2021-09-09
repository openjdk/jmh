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
package org.openjdk.jmh.it.params;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1)
public class JavaIntegerLiteralTest {

    @Param({"0b01_10__0", "0B01_10__0"})
    public int binary;

    @Param({"0B01_10__0"})
    public int binary2;

    @Param({"01_23"})
    public byte oct;

    @Param({"1__0____9"})
    public short dec;

    @Param({"0__1234___5"})
    public int oct2;

    @Param({"0x12_34"})
    public long hex;

    @Param({"0XF__F_FF"})
    public int hex2;

    @Benchmark
    public void test() {
        Assert.assertEquals(0b01_10__0, binary);
        Assert.assertEquals(0B01_10__0, binary2);
        Assert.assertEquals(01_23, oct);
        Assert.assertEquals(0__1234___5, oct2);
        Assert.assertEquals(1__0____9, dec);
        Assert.assertEquals(0x12_34, hex);
        Assert.assertEquals(0XF__F_FF, hex2);
    }

    @Test
    public void testValidArguments() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .build();

        new Runner(opts).run();
    }

    @Test
    public void testInvalidArguments() {
        testFailsOnInvalidLiteral("dec", "0_");
        testFailsOnInvalidLiteral("dec", "_");
        testFailsOnInvalidLiteral("dec", "_0");
        testFailsOnInvalidLiteral("hex", "0x_F");
        testFailsOnInvalidLiteral("hex", "0_xF");
    }

    private void testFailsOnInvalidLiteral(String name, String value) {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .param(name, value)
                .shouldFailOnError(true)
                .build();

        try {
            new Runner(opts).run();
            Assert.fail();
        } catch (RunnerException e) {
            // Expected
        }
    }
}
