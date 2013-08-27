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
package org.openjdk.jmh.runner.parameters;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * A generic time scalar.
 */
public class TimeValue implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final TimeValue NONE = new TimeValue(0, TimeUnit.SECONDS);


    public static TimeValue days(long v) {
        return new TimeValue(v, TimeUnit.DAYS);
    }

    public static TimeValue hours(long v) {
        return new TimeValue(v, TimeUnit.HOURS);
    }

    public static TimeValue microseconds(long v) {
        return new TimeValue(v, TimeUnit.MICROSECONDS);
    }

    public static TimeValue milliseconds(long v) {
        return new TimeValue(v, TimeUnit.MILLISECONDS);
    }

    public static TimeValue minutes(long v) {
        return new TimeValue(v, TimeUnit.MINUTES);
    }

    public static TimeValue nanoseconds(long v) {
        return new TimeValue(v, TimeUnit.NANOSECONDS);
    }

    public static TimeValue seconds(long v) {
        return new TimeValue(v, TimeUnit.SECONDS);
    }

    private final long time;

    private final TimeUnit timeUnit;

    public TimeValue(long time, TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 41 * hash + (this.timeUnit != null ? this.timeUnit.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimeValue other = (TimeValue) obj;
        if (this.time != other.time) {
            return false;
        }
        if (this.timeUnit != other.timeUnit) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (time == 0) {
            return "single-shot";
        } else {
            return String.valueOf(time) + timeUnitString();
        }
    }

    public String toCommandLine() {
        return String.valueOf(time) + timeUnitString();
    }

    private String timeUnitString() {
        switch(timeUnit) {
            case SECONDS:
                return "s";
            case MILLISECONDS:
                return "ms";
            case MICROSECONDS:
                return "us";
            case NANOSECONDS:
                return "ns";
            case MINUTES:
                return "m";
            default:
                return "s";
        }
    }

    public static TimeValue fromString(String timeString) {
        if (timeString == null) {
            return new TimeValue(-1, null);
        }
        timeString = timeString.toLowerCase();
        if (timeString.contains("ns")) {
            return new TimeValue(Integer.parseInt(timeString.substring(0, timeString.indexOf("ns"))), TimeUnit.NANOSECONDS);
        }
        if (timeString.contains("ms")) {
            return new TimeValue(Integer.parseInt(timeString.substring(0, timeString.indexOf("ms"))), TimeUnit.MILLISECONDS);
        }
        if (timeString.contains("us")) {
            return new TimeValue(Integer.parseInt(timeString.substring(0, timeString.indexOf("us"))), TimeUnit.MICROSECONDS);
        }
        if (timeString.contains("s")) {
            return new TimeValue(Integer.parseInt(timeString.substring(0, timeString.indexOf("s"))), TimeUnit.SECONDS);
        }
        if (timeString.contains("m")) {
            return new TimeValue(Integer.parseInt(timeString.substring(0, timeString.indexOf("m"))), TimeUnit.MINUTES);
        }
        return new TimeValue(Integer.parseInt(timeString), TimeUnit.SECONDS);
    }

    public long convertTo(TimeUnit unit) {
        return unit.convert(time, timeUnit);
    }

    public void sleep() throws InterruptedException {
        timeUnit.sleep(time);
    }
}
