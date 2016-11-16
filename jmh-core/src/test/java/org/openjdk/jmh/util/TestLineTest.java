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
package org.openjdk.jmh.util;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.lines.TestLineReader;
import org.openjdk.jmh.util.lines.TestLineWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestLineTest {

    @Test
    public void test() {
        TestLineWriter writer = new TestLineWriter();

        writer.putString("jmh");
        writer.putString("test");
        writer.putOptionalString(Optional.eitherOf("full-optional"));
        writer.putOptionalString(Optional.<String>none());

        writer.putOptionalInt(Optional.eitherOf(42));
        writer.putOptionalInt(Optional.<Integer>none());

        writer.putIntArray(new int[] {5, 3, 2});

        writer.putOptionalTimeValue(Optional.eitherOf(TimeValue.milliseconds(14)));
        writer.putOptionalTimeValue(Optional.<TimeValue>none());

        writer.putOptionalTimeUnit(Optional.eitherOf(TimeUnit.HOURS));
        writer.putOptionalTimeUnit(Optional.<TimeUnit>none());

        writer.putOptionalStringCollection(Optional.<Collection<String>>eitherOf(Arrays.asList("foo", "bar", "baz")));
        writer.putOptionalStringCollection(Optional.<Collection<String>>none());

        HashMap<String, String[]> expectedMap = new HashMap<>();
        expectedMap.put("key1", new String[] {"val1", "val2"});
        expectedMap.put("key2", new String[] {"val3", "val4"});
        writer.putOptionalParamCollection(Optional.<Map<String,String[]>>eitherOf(expectedMap));
        writer.putOptionalParamCollection(Optional.<Map<String,String[]>>none());

        String s = writer.toString();

        TestLineReader reader = new TestLineReader(s);

        Assert.assertEquals("jmh", reader.nextString());
        Assert.assertEquals("test", reader.nextString());

        Assert.assertEquals("full-optional", reader.nextOptionalString().get());
        Assert.assertEquals(false, reader.nextOptionalString().hasValue());

        Assert.assertEquals(42, (int)reader.nextOptionalInt().get());
        Assert.assertEquals(false, reader.nextOptionalInt().hasValue());

        Assert.assertTrue(Arrays.equals(new int[] {5, 3, 2}, reader.nextIntArray()));

        Assert.assertEquals(TimeValue.milliseconds(14), reader.nextOptionalTimeValue().get());
        Assert.assertEquals(false, reader.nextOptionalTimeValue().hasValue());

        Assert.assertEquals(TimeUnit.HOURS, reader.nextOptionalTimeUnit().get());
        Assert.assertEquals(false, reader.nextOptionalTimeUnit().hasValue());

        Assert.assertEquals(Arrays.asList("foo", "bar", "baz"), reader.nextOptionalStringCollection().get());
        Assert.assertEquals(false, reader.nextOptionalStringCollection().hasValue());

        Map<String, String[]> actualMap = reader.nextOptionalParamCollection().get();
        Assert.assertEquals(expectedMap.size(), actualMap.size());
        Assert.assertEquals(expectedMap.keySet(), actualMap.keySet());

        for (String key : expectedMap.keySet()) {
            String[] expectedVals = expectedMap.get(key);
            String[] actualVals = actualMap.get(key);
            Assert.assertTrue(Arrays.equals(expectedVals, actualVals));
        }
    }

}
