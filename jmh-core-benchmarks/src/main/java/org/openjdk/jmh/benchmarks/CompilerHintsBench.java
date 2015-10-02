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
package org.openjdk.jmh.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CompilerHintsBench {

    @Benchmark
    public void baseI_baseline() {
        do_Plain(Math.PI);
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:MaxInlineSize=0", "-XX:FreqInlineSize=0"})
    public void baseNI_baseline() {
        do_Plain(Math.PI);
    }

    @Benchmark
    public void baseI_inline() {
        do_Inline(Math.PI);
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:MaxInlineSize=0", "-XX:FreqInlineSize=0"})
    public void baseNI_inline() {
        do_Inline(Math.PI);
    }

    @Benchmark
    public void baseI_dontInline() {
        do_DontInline(Math.PI);
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:MaxInlineSize=0", "-XX:FreqInlineSize=0"})
    public void baseNI_dontInline() {
        do_DontInline(Math.PI);
    }

    @Benchmark
    public void baseI_exclude() {
        do_Exclude(Math.PI);
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:MaxInlineSize=0", "-XX:FreqInlineSize=0"})
    public void baseNI_exclude() {
        do_Exclude(Math.PI);
    }

    private double do_Plain(double x) {
        return Math.log(x);
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    private double do_Inline(double x) {
        return Math.log(x);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private double do_DontInline(double x) {
        return Math.log(x);
    }

    @CompilerControl(CompilerControl.Mode.EXCLUDE)
    private double do_Exclude(double x) {
        return Math.log(x);
    }


}
