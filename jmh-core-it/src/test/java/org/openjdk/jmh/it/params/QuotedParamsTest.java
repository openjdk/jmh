/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class QuotedParamsTest {

    @Param("value")
    public String param_plain;

    @Param("[value]")
    public String param_square;

    @Param("{value}")
    public String param_curly;

    @Param("'value'")
    public String param_squote;

    @Param("\"value\"")
    public String param_dquote;

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void test() {
        Fixtures.work();
        Assert.assertEquals("value", param_plain);
        Assert.assertEquals("[value]", param_square);
        Assert.assertEquals("{value}", param_curly);
        Assert.assertEquals("'value'", param_squote);
        Assert.assertEquals("\"value\"", param_dquote);
    }

    @Test
    public void vals() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass()))
                .shouldFailOnError(true)
                .build();

        new Runner(opts).run();
    }

}
