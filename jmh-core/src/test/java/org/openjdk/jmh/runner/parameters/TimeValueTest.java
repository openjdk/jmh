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
package org.openjdk.jmh.runner.parameters;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

public class TimeValueTest {

    @Test
    public void testNano() {
        TimeValue v = TimeValue.fromString(TimeValue.nanoseconds(10).toString());
        Assert.assertEquals(TimeUnit.NANOSECONDS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testMicro() {
        TimeValue v = TimeValue.fromString(TimeValue.microseconds(10).toString());
        Assert.assertEquals(TimeUnit.MICROSECONDS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testMilli() {
        TimeValue v = TimeValue.fromString(TimeValue.milliseconds(10).toString());
        Assert.assertEquals(TimeUnit.MILLISECONDS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testSeconds() {
        TimeValue v = TimeValue.fromString(TimeValue.seconds(10).toString());
        Assert.assertEquals(TimeUnit.SECONDS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testMinutes() {
        TimeValue v = TimeValue.fromString(TimeValue.minutes(10).toString());
        Assert.assertEquals(TimeUnit.MINUTES, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testHours() {
        TimeValue v = TimeValue.fromString(TimeValue.hours(10).toString());
        Assert.assertEquals(TimeUnit.HOURS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

    @Test
    public void testDays() {
        TimeValue v = TimeValue.fromString(TimeValue.days(10).toString());
        Assert.assertEquals(TimeUnit.DAYS, v.getTimeUnit());
        Assert.assertEquals(10, v.getTime());
    }

}
