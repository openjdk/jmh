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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.util.JDKVersion;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public abstract class AbstractAsmProfilerTest {

    private static final int SIZE = 10_000;

    private byte[] src, dst;

    @Setup
    public void setup() {
        src = new byte[SIZE];
        dst = new byte[SIZE];
    }

    @Benchmark
    public void work() {
        // Call something that definitely results in calling a native stub.
        // This should work on environments where hsdis is not available.
        System.arraycopy(src, 0, dst, 0, SIZE);
    }

    public static boolean checkDisassembly(String out) {
        if (JDKVersion.parseMajor(System.getProperty("java.version")) >= 17) {
            // Should always print, since abstract assembler is available
            return out.contains("StubRoutines::");
        } else {
            if (out.contains("StubRoutines::")) {
                // hsdis is enabled, good
                return true;
            } else {
                // hsdis is not enabled, okay
                return out.contains("<no assembly is recorded, native region>");
            }
        }
    }

}
