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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.internal.Option;

import java.io.Serializable;

public class BenchmarkRecord implements Comparable<BenchmarkRecord>, Serializable {

    private final String userName;
    private final String generatedName;
    private final Mode mode;
    private final int[] threadGroups;
    private final Option<Integer> threads;
    private final Option<Integer> warmupIterations;
    private final Option<TimeValue> warmupTime;
    private final Option<Integer> measurementIterations;
    private final Option<TimeValue> measurementTime;
    private final Option<Integer> forks;
    private final Option<Integer> warmupForks;
    private final Option<String> jvmArgs;
    private final Option<String> jvmArgsPrepend;
    private final Option<String> jvmArgsAppend;

    public BenchmarkRecord(String userName, String generatedName, Mode mode, int[] threadGroups, Option<Integer> threads,
                           Option<Integer> warmupIterations, Option<TimeValue> warmupTime,
                           Option<Integer> measurementIterations, Option<TimeValue> measurementTime,
                           Option<Integer> forks, Option<Integer> warmupForks, Option<String> jvmArgs, Option<String> jvmArgsPrepend, Option<String> jvmArgsAppend) {
        this.userName = userName;
        this.generatedName = generatedName;
        this.mode = mode;
        this.threadGroups = threadGroups;
        this.threads = threads;
        this.warmupIterations = warmupIterations;
        this.warmupTime = warmupTime;
        this.measurementIterations = measurementIterations;
        this.measurementTime = measurementTime;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.jvmArgs = jvmArgs;
        this.jvmArgsPrepend = jvmArgsPrepend;
        this.jvmArgsAppend = jvmArgsAppend;
    }

    public BenchmarkRecord(String line) {
        String[] args = line.split(",");

        if (args.length != 14) {
            throw new IllegalStateException("Mismatched format for the line: " + line);
        }

        this.userName = args[0].trim();
        this.generatedName = args[1].trim();
        this.mode = Mode.deepValueOf(args[2].trim());
        this.threadGroups = convert(args[3].split("="));
        this.threads = Option.of(args[4], Option.INTEGER_EXTRACTOR);
        this.warmupIterations = Option.of(args[5], Option.INTEGER_EXTRACTOR);
        this.warmupTime = Option.of(args[6], Option.TIME_VALUE_EXTRACTOR);
        this.measurementIterations = Option.of(args[7], Option.INTEGER_EXTRACTOR);
        this.measurementTime = Option.of(args[8], Option.TIME_VALUE_EXTRACTOR);
        this.forks = Option.of(args[9], Option.INTEGER_EXTRACTOR);
        this.warmupForks = Option.of(args[10], Option.INTEGER_EXTRACTOR);
        this.jvmArgs = Option.of(args[11], Option.STRING_EXTRACTOR);
        this.jvmArgsPrepend = Option.of(args[12], Option.STRING_EXTRACTOR);
        this.jvmArgsAppend = Option.of(args[13], Option.STRING_EXTRACTOR);
    }

    public BenchmarkRecord(String userName, String generatedName, Mode mode) {
        this(userName, generatedName, mode, new int[]{}, Option.<Integer>none(),
                Option.<Integer>none(), Option.<TimeValue>none(), Option.<Integer>none(), Option.<TimeValue>none(),
                Option.<Integer>none(), Option.<Integer>none(), Option.<String>none(), Option.<String>none(), Option.<String>none());
    }

    public String toLine() {
        return userName + "," + generatedName + "," + mode + "," + convert(threadGroups) + "," + threads + "," +
                warmupIterations + "," + warmupTime + "," + measurementIterations + "," + measurementTime + "," +
                forks + "," + warmupForks + "," + jvmArgs + "," + jvmArgsPrepend + "," + jvmArgsAppend;
    }

    public BenchmarkRecord cloneWith(Mode mode) {
        return new BenchmarkRecord(userName, generatedName, mode, threadGroups, threads,
                warmupIterations, warmupTime, measurementIterations, measurementTime,
                forks, warmupForks, jvmArgs, jvmArgsPrepend, jvmArgsAppend);
    }

    private int[] convert(String[] ss) {
        int[] arr = new int[ss.length];
        int cnt = 0;
        for (String s : ss) {
            arr[cnt] = Integer.valueOf(s.trim());
            cnt++;
        }
        return arr;
    }

    private String convert(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i : arr) {
            sb.append(i);
            sb.append("=");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(BenchmarkRecord o) {
        int v = mode.compareTo(o.mode);
        if (v != 0) {
            return v;
        }

        return userName.compareTo(o.userName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkRecord that = (BenchmarkRecord) o;

        if (mode != that.mode) return false;
        if (!userName.equals(that.userName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName.hashCode();
        result = 31 * result + mode.hashCode();
        return result;
    }

    public String generatedTarget(Mode type) {
        return generatedName + "_" + type;
    }

    public String generatedTarget() {
        return generatedTarget(mode);
    }

    public String generatedClass() {
        String s = generatedTarget();
        return s.substring(0, s.lastIndexOf('.'));
    }

    public String generatedMethod() {
        String s = generatedTarget();
        return s.substring(s.lastIndexOf('.') + 1);
    }

    public String getUsername() {
        return userName;
    }

    public Mode getMode() {
        return mode;
    }

    public int[] getThreadGroups() {
        return threadGroups;
    }

    @Override
    public String toString() {
        return "BenchmarkRecord{" +
                "userName='" + userName + '\'' +
                ", generatedName='" + generatedName + '\'' +
                ", mode=" + mode +
                '}';
    }

    public Option<TimeValue> getWarmupTime() {
        return warmupTime;
    }

    public Option<Integer> getWarmupIterations() {
        return warmupIterations;
    }

    public Option<TimeValue> getMeasurementTime() {
        return measurementTime;
    }

    public Option<Integer> getMeasurementIterations() {
        return measurementIterations;
    }

    public Option<Integer> getForks() {
        return forks;
    }

    public Option<Integer> getWarmupForks() {
        return warmupForks;
    }

    public Option<String> getJvmArgs() {
        return jvmArgs;
    }

    public Option<String> getJvmArgsAppend() {
        return jvmArgsAppend;
    }

    public Option<String> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public Option<Integer> getThreads() {
        return threads;
    }
}
