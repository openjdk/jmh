/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.parameters;

import org.junit.Assert;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class Parameters {

    public static BenchmarkListEntry get(Class<?> klass) {
        BenchmarkList list = BenchmarkList.fromFile("target/test-classes" + BenchmarkList.BENCHMARK_LIST);
        Set<BenchmarkListEntry> set = list.find(OutputFormatFactory.createFormatInstance(System.out, VerboseMode.EXTRA),
                Collections.singletonList(klass.getName().replaceAll("\\$", ".")),
                Collections.<String>emptyList());
        Assert.assertEquals("The single benchmark exists", 1, set.size());
        return set.iterator().next();
    }
}
