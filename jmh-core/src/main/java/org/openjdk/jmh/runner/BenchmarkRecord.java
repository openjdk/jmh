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
import org.openjdk.jmh.util.internal.Optional;

import java.io.Serializable;

public class BenchmarkRecord implements Comparable<BenchmarkRecord>, Serializable {

    private final String userName;
    private final String generatedName;
    private final Mode mode;
    private final int[] threadGroups;
    private final Optional<Integer> threads;
    private final Optional<Integer> warmupIterations;
    private final Optional<TimeValue> warmupTime;
    private final Optional<Integer> measurementIterations;
    private final Optional<TimeValue> measurementTime;
    private final Optional<Integer> forks;
    private final Optional<Integer> warmupForks;
    private final Optional<String> jvmArgs;
    private final Optional<String> jvmArgsPrepend;
    private final Optional<String> jvmArgsAppend;

    public BenchmarkRecord(String userName, String generatedName, Mode mode, int[] threadGroups, Optional<Integer> threads,
                           Optional<Integer> warmupIterations, Optional<TimeValue> warmupTime,
                           Optional<Integer> measurementIterations, Optional<TimeValue> measurementTime,
                           Optional<Integer> forks, Optional<Integer> warmupForks, Optional<String> jvmArgs, Optional<String> jvmArgsPrepend, Optional<String> jvmArgsAppend) {
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
        this.threads = Optional.of(args[4], Optional.INTEGER_EXTRACTOR);
        this.warmupIterations = Optional.of(args[5], Optional.INTEGER_EXTRACTOR);
        this.warmupTime = Optional.of(args[6], Optional.TIME_VALUE_EXTRACTOR);
        this.measurementIterations = Optional.of(args[7], Optional.INTEGER_EXTRACTOR);
        this.measurementTime = Optional.of(args[8], Optional.TIME_VALUE_EXTRACTOR);
        this.forks = Optional.of(args[9], Optional.INTEGER_EXTRACTOR);
        this.warmupForks = Optional.of(args[10], Optional.INTEGER_EXTRACTOR);
        this.jvmArgs = Optional.of(args[11], Optional.STRING_EXTRACTOR);
        this.jvmArgsPrepend = Optional.of(args[12], Optional.STRING_EXTRACTOR);
        this.jvmArgsAppend = Optional.of(args[13], Optional.STRING_EXTRACTOR);
    }

    public BenchmarkRecord(String userName, String generatedName, Mode mode) {
        this(userName, generatedName, mode, new int[]{}, Optional.<Integer>none(),
                Optional.<Integer>none(), Optional.<TimeValue>none(), Optional.<Integer>none(), Optional.<TimeValue>none(),
                Optional.<Integer>none(), Optional.<Integer>none(), Optional.<String>none(), Optional.<String>none(), Optional.<String>none());
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

    public Optional<TimeValue> getWarmupTime() {
        return warmupTime;
    }

    public Optional<Integer> getWarmupIterations() {
        return warmupIterations;
    }

    public Optional<TimeValue> getMeasurementTime() {
        return measurementTime;
    }

    public Optional<Integer> getMeasurementIterations() {
        return measurementIterations;
    }

    public Optional<Integer> getForks() {
        return forks;
    }

    public Optional<Integer> getWarmupForks() {
        return warmupForks;
    }

    public Optional<String> getJvmArgs() {
        return jvmArgs;
    }

    public Optional<String> getJvmArgsAppend() {
        return jvmArgsAppend;
    }

    public Optional<String> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public Optional<Integer> getThreads() {
        return threads;
    }
}
