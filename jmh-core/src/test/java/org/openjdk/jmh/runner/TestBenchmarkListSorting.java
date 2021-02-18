/*
 * Copyright (c) 2021, Red Hat Inc. All rights reserved.
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
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestBenchmarkListSorting {

    private static BenchmarkListEntry stub(String userClassQName, String generatedClassQName, String method, Mode mode) {
        BenchmarkListEntry br = new BenchmarkListEntry(
                userClassQName,
                generatedClassQName,
                method,
                mode,
                Optional.<Integer>none(),
                new int[]{1},
                Optional.<Collection<String>>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<Integer>none(),
                Optional.<String>none(),
                Optional.<Collection<String>>none(),
                Optional.<Collection<String>>none(),
                Optional.<Collection<String>>none(),
                Optional.<Map<String, String[]>>none(),
                Optional.<TimeUnit>none(),
                Optional.<Integer>none(),
                Optional.<TimeValue>none()
        );
        return br;
    }

    @Test
    public void test() throws Exception {
        BenchmarkListEntry br1 = stub("something.Test1",
                "something.generated.Test1",
                "something.generated.TestMethod",
                Mode.AverageTime);
        BenchmarkListEntry br2 = stub("something.Test2",
                "something.generated.Test1",
                "something.generated.TestMethod",
                Mode.AverageTime);
        BenchmarkListEntry br3 = stub("something.Test3",
                "something.generated.Test1",
                "something.generated.TestMethod",
                Mode.AverageTime);
        BenchmarkListEntry br4 = stub("something.Test4",
                "something.generated.Test1",
                "something.generated.TestMethod",
                Mode.AverageTime);

        // Present to writer in mixed order
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BenchmarkList.writeBenchmarkList(bos, Arrays.asList(br4, br2, br3, br1));

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        List<BenchmarkListEntry> read = BenchmarkList.readBenchmarkList(bis);

        // Assert we read these in proper order
        assertEquals("something.Test1", read.get(0).getUserClassQName());
        assertEquals("something.Test2", read.get(1).getUserClassQName());
        assertEquals("something.Test3", read.get(2).getUserClassQName());
        assertEquals("something.Test4", read.get(3).getUserClassQName());
    }

}
