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
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.Utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BenchmarkRecord implements Comparable<BenchmarkRecord>, Serializable {

    private static final String BR_SEPARATOR = "===,===";

    private final String userName;
    private final String generatedName;
    private final Mode mode;
    private final int[] threadGroups;
    private final Optional<Integer> threads;
    private final Optional<Integer> warmupIterations;
    private final Optional<TimeValue> warmupTime;
    private final Optional<Integer> warmupBatchSize;
    private final Optional<Integer> measurementIterations;
    private final Optional<TimeValue> measurementTime;
    private final Optional<Integer> measurementBatchSize;
    private final Optional<Integer> forks;
    private final Optional<Integer> warmupForks;
    private final Optional<Collection<String>> jvmArgs;
    private final Optional<Collection<String>> jvmArgsPrepend;
    private final Optional<Collection<String>> jvmArgsAppend;
    private final Optional<Map<String, String[]>> params;

    private ActualParams actualParams;

    public BenchmarkRecord(String userName, String generatedName, Mode mode, int[] threadGroups, Optional<Integer> threads,
                           Optional<Integer> warmupIterations, Optional<TimeValue> warmupTime, Optional<Integer> warmupBatchSize,
                           Optional<Integer> measurementIterations, Optional<TimeValue> measurementTime, Optional<Integer> measurementBatchSize,
                           Optional<Integer> forks, Optional<Integer> warmupForks, Optional<Collection<String>> jvmArgs, Optional<Collection<String>> jvmArgsPrepend, Optional<Collection<String>> jvmArgsAppend,
                           Optional<Map<String, String[]>> params) {
        this.userName = userName;
        this.generatedName = generatedName;
        this.mode = mode;
        this.threadGroups = threadGroups;
        this.threads = threads;
        this.warmupIterations = warmupIterations;
        this.warmupTime = warmupTime;
        this.warmupBatchSize = warmupBatchSize;
        this.measurementIterations = measurementIterations;
        this.measurementTime = measurementTime;
        this.measurementBatchSize = measurementBatchSize;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.jvmArgs = jvmArgs;
        this.jvmArgsPrepend = jvmArgsPrepend;
        this.jvmArgsAppend = jvmArgsAppend;
        this.params = params;
        this.actualParams = new ActualParams();
    }

    public BenchmarkRecord(String line) {
        String[] args = line.split(BR_SEPARATOR);

        if (args.length != 17) {
            throw new IllegalStateException("Mismatched format for the line: " + line);
        }

        this.actualParams = new ActualParams();
        this.userName = args[0].trim();
        this.generatedName = args[1].trim();
        this.mode = Mode.deepValueOf(args[2].trim());
        this.threadGroups = Utils.unmarshalIntArray(args[3]);
        this.threads = Optional.of(args[4], Optional.INTEGER_UNMARSHALLER);
        this.warmupIterations = Optional.of(args[5], Optional.INTEGER_UNMARSHALLER);
        this.warmupTime = Optional.of(args[6], Optional.TIME_VALUE_UNMARSHALLER);
        this.warmupBatchSize = Optional.of(args[7], Optional.INTEGER_UNMARSHALLER);
        this.measurementIterations = Optional.of(args[8], Optional.INTEGER_UNMARSHALLER);
        this.measurementTime = Optional.of(args[9], Optional.TIME_VALUE_UNMARSHALLER);
        this.measurementBatchSize = Optional.of(args[10], Optional.INTEGER_UNMARSHALLER);
        this.forks = Optional.of(args[11], Optional.INTEGER_UNMARSHALLER);
        this.warmupForks = Optional.of(args[12], Optional.INTEGER_UNMARSHALLER);
        this.jvmArgs = Optional.of(args[13], Optional.STRING_COLLECTION_UNMARSHALLER);
        this.jvmArgsPrepend = Optional.of(args[14], Optional.STRING_COLLECTION_UNMARSHALLER);
        this.jvmArgsAppend = Optional.of(args[15], Optional.STRING_COLLECTION_UNMARSHALLER);
        this.params = Optional.of(args[16], Optional.PARAM_COLLECTION_UNMARSHALLER);
    }

    public BenchmarkRecord(String userName, String generatedName, Mode mode) {
        this(userName, generatedName, mode, new int[]{}, Optional.<Integer>none(),
                Optional.<Integer>none(), Optional.<TimeValue>none(), Optional.<Integer>none(), Optional.<Integer>none(), Optional.<TimeValue>none(), Optional.<Integer>none(),
                Optional.<Integer>none(), Optional.<Integer>none(), Optional.<Collection<String>>none(), Optional.<Collection<String>>none(), Optional.<Collection<String>>none(),
                Optional.<Map<String, String[]>>none());
    }

    public String toLine() {
        return userName + BR_SEPARATOR + generatedName + BR_SEPARATOR + mode + BR_SEPARATOR + Utils.marshalIntArray(threadGroups) + BR_SEPARATOR +
                threads + BR_SEPARATOR + warmupIterations + BR_SEPARATOR + warmupTime + BR_SEPARATOR + warmupBatchSize + BR_SEPARATOR +
                measurementIterations + BR_SEPARATOR + measurementTime + BR_SEPARATOR + measurementBatchSize + BR_SEPARATOR +
                forks + BR_SEPARATOR + warmupForks + BR_SEPARATOR +
                jvmArgs.toString(Optional.STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                jvmArgsPrepend.toString(Optional.STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                jvmArgsAppend.toString(Optional.STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                params.toString(Optional.PARAM_COLLECTION_MARSHALLER);
    }

    public BenchmarkRecord cloneWith(Mode mode) {
        return new BenchmarkRecord(userName, generatedName, mode, threadGroups, threads,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks, jvmArgs, jvmArgsPrepend, jvmArgsAppend, params);
    }

    public BenchmarkRecord cloneWith(ActualParams p) {
        BenchmarkRecord br = new BenchmarkRecord(userName, generatedName, mode, threadGroups, threads,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks, jvmArgs, jvmArgsPrepend, jvmArgsAppend, params);
        br.actualParams = p;
        return br;
    }

    public ActualParams getActualParams() {
        return actualParams;
    }

    public String getActualParam(String key) {
        if (actualParams != null) {
            return actualParams.get(key);
        } else {
            return null;
        }
    }

    @Override
    public int compareTo(BenchmarkRecord o) {
        int v = mode.compareTo(o.mode);
        if (v != 0) {
            return v;
        }

        int v1 = userName.compareTo(o.userName);
        if (v1 != 0) {
            return v1;
        }

        if (actualParams == null || o.actualParams == null) {
            return 0;
        }

        return actualParams.compareTo(o.actualParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkRecord record = (BenchmarkRecord) o;

        if (mode != record.mode) return false;
        if (actualParams != null ? !actualParams.equals(record.actualParams) : record.actualParams != null) return false;
        if (userName != null ? !userName.equals(record.userName) : record.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (actualParams != null ? actualParams.hashCode() : 0);
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
        return Arrays.copyOf(threadGroups, threadGroups.length);
    }

    @Override
    public String toString() {
        return "BenchmarkRecord{" +
                "userName='" + userName + '\'' +
                ", generatedName='" + generatedName + '\'' +
                ", mode=" + mode +
                ", actualParams=" + actualParams +
                '}';
    }

    public Optional<TimeValue> getWarmupTime() {
        return warmupTime;
    }

    public Optional<Integer> getWarmupIterations() {
        return warmupIterations;
    }

    public Optional<Integer> getWarmupBatchSize() {
        return warmupBatchSize;
    }

    public Optional<TimeValue> getMeasurementTime() {
        return measurementTime;
    }

    public Optional<Integer> getMeasurementIterations() {
        return measurementIterations;
    }

    public Optional<Integer> getMeasurementBatchSize() {
        return measurementBatchSize;
    }

    public Optional<Integer> getForks() {
        return forks;
    }

    public Optional<Integer> getWarmupForks() {
        return warmupForks;
    }

    public Optional<Collection<String>> getJvmArgs() {
        return jvmArgs;
    }

    public Optional<Collection<String>> getJvmArgsAppend() {
        return jvmArgsAppend;
    }

    public Optional<Collection<String>> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public Optional<Integer> getThreads() {
        return threads;
    }

    public long estimatedTimeSingleFork(Options opts) {
        int mi = opts.getMeasurementIterations()
                .orElse(getMeasurementIterations()
                        .orElse(Defaults.MEASUREMENT_ITERATIONS));

        TimeValue mt = opts.getMeasurementTime()
                .orElse(getMeasurementTime()
                        .orElse(Defaults.MEASUREMENT_TIME));

        int wi = opts.getWarmupIterations()
                .orElse(getWarmupIterations()
                        .orElse(Defaults.WARMUP_ITERATIONS));

        TimeValue wt = opts.getWarmupTime()
                .orElse(getWarmupTime()
                        .orElse(Defaults.WARMUP_TIME));

        if (opts.getBenchModes().contains(Mode.SingleShotTime)) {
            // No way to tell how long it will execute,
            // guess anything, and let ETA compensation to catch up.
            return (wi + mi) * TimeUnit.MILLISECONDS.toNanos(1);
        }

        return (wi * wt.convertTo(TimeUnit.NANOSECONDS) + mi * mt.convertTo(TimeUnit.NANOSECONDS));
    }

    public long estimatedTime(Options opts) {
        int forks = opts.getForkCount()
                .orElse(getForks()
                        .orElse(Defaults.MEASUREMENT_FORKS));

        int warmupForks = opts.getWarmupForkCount()
                .orElse(getWarmupForks()
                        .orElse(Defaults.WARMUP_FORKS));

        return (Math.max(1, forks) + warmupForks) * estimatedTimeSingleFork(opts);
    }

    public Optional<Map<String, String[]>> getParams() {
        return params;
    }

}
