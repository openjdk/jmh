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
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.lines.TestLineReader;
import org.openjdk.jmh.util.lines.TestLineWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BenchmarkListEntry implements Comparable<BenchmarkListEntry> {

    private final String userClassQName;
    private final String generatedClassQName;
    private final String method;
    private final Mode mode;
    private final int[] threadGroups;
    private final Optional<Collection<String>> threadGroupLabels;
    private final Optional<Integer> threads;
    private final Optional<Integer> warmupIterations;
    private final Optional<TimeValue> warmupTime;
    private final Optional<Integer> warmupBatchSize;
    private final Optional<Integer> measurementIterations;
    private final Optional<TimeValue> measurementTime;
    private final Optional<Integer> measurementBatchSize;
    private final Optional<Integer> forks;
    private final Optional<Integer> warmupForks;
    private final Optional<String> jvm;
    private final Optional<Collection<String>> jvmArgs;
    private final Optional<Collection<String>> jvmArgsPrepend;
    private final Optional<Collection<String>> jvmArgsAppend;
    private final Optional<Map<String, String[]>> params;
    private final Optional<TimeUnit> tu;
    private final Optional<Integer> opsPerInvocation;
    private final Optional<TimeValue> timeout;

    private WorkloadParams workloadParams;

    public BenchmarkListEntry(String userClassQName, String generatedClassQName, String method, Mode mode,
                              Optional<Integer> threads, int[] threadGroups, Optional<Collection<String>> threadGroupLabels,
                              Optional<Integer> warmupIterations, Optional<TimeValue> warmupTime, Optional<Integer> warmupBatchSize,
                              Optional<Integer> measurementIterations, Optional<TimeValue> measurementTime, Optional<Integer> measurementBatchSize,
                              Optional<Integer> forks, Optional<Integer> warmupForks,
                              Optional<String> jvm, Optional<Collection<String>> jvmArgs, Optional<Collection<String>> jvmArgsPrepend, Optional<Collection<String>> jvmArgsAppend,
                              Optional<Map<String, String[]>> params, Optional<TimeUnit> tu, Optional<Integer> opsPerInv,
                              Optional<TimeValue> timeout) {
        this.userClassQName = userClassQName;
        this.generatedClassQName = generatedClassQName;
        this.method = method;
        this.mode = mode;
        this.threadGroups = threadGroups;
        this.threads = threads;
        this.threadGroupLabels = threadGroupLabels;
        this.warmupIterations = warmupIterations;
        this.warmupTime = warmupTime;
        this.warmupBatchSize = warmupBatchSize;
        this.measurementIterations = measurementIterations;
        this.measurementTime = measurementTime;
        this.measurementBatchSize = measurementBatchSize;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.jvm = jvm;
        this.jvmArgs = jvmArgs;
        this.jvmArgsPrepend = jvmArgsPrepend;
        this.jvmArgsAppend = jvmArgsAppend;
        this.params = params;
        this.workloadParams = new WorkloadParams();
        this.tu = tu;
        this.opsPerInvocation = opsPerInv;
        this.timeout = timeout;
    }

    public BenchmarkListEntry(String line) {
        this.workloadParams = new WorkloadParams();

        TestLineReader reader = new TestLineReader(line);

        if (!reader.isCorrect()) {
            throw new IllegalStateException("Unable to parse the line: " + line);
        }

        this.userClassQName         = reader.nextString();
        this.generatedClassQName    = reader.nextString();
        this.method                 = reader.nextString();
        this.mode                   = Mode.deepValueOf(reader.nextString());
        this.threads                = reader.nextOptionalInt();
        this.threadGroups           = reader.nextIntArray();
        this.threadGroupLabels      = reader.nextOptionalStringCollection();
        this.warmupIterations       = reader.nextOptionalInt();
        this.warmupTime             = reader.nextOptionalTimeValue();
        this.warmupBatchSize        = reader.nextOptionalInt();
        this.measurementIterations  = reader.nextOptionalInt();
        this.measurementTime        = reader.nextOptionalTimeValue();
        this.measurementBatchSize   = reader.nextOptionalInt();
        this.forks                  = reader.nextOptionalInt();
        this.warmupForks            = reader.nextOptionalInt();
        this.jvm                    = reader.nextOptionalString();
        this.jvmArgs                = reader.nextOptionalStringCollection();
        this.jvmArgsPrepend         = reader.nextOptionalStringCollection();
        this.jvmArgsAppend          = reader.nextOptionalStringCollection();
        this.params                 = reader.nextOptionalParamCollection();
        this.tu                     = reader.nextOptionalTimeUnit();
        this.opsPerInvocation       = reader.nextOptionalInt();
        this.timeout                = reader.nextOptionalTimeValue();
    }

    public String toLine() {
        TestLineWriter writer = new TestLineWriter();

        writer.putString(userClassQName);
        writer.putString(generatedClassQName);
        writer.putString(method);
        writer.putString(mode.toString());
        writer.putOptionalInt(threads);
        writer.putIntArray(threadGroups);
        writer.putOptionalStringCollection(threadGroupLabels);
        writer.putOptionalInt(warmupIterations);
        writer.putOptionalTimeValue(warmupTime);
        writer.putOptionalInt(warmupBatchSize);
        writer.putOptionalInt(measurementIterations);
        writer.putOptionalTimeValue(measurementTime);
        writer.putOptionalInt(measurementBatchSize);
        writer.putOptionalInt(forks);
        writer.putOptionalInt(warmupForks);
        writer.putOptionalString(jvm);
        writer.putOptionalStringCollection(jvmArgs);
        writer.putOptionalStringCollection(jvmArgsPrepend);
        writer.putOptionalStringCollection(jvmArgsAppend);
        writer.putOptionalParamCollection(params);
        writer.putOptionalTimeUnit(tu);
        writer.putOptionalInt(opsPerInvocation);
        writer.putOptionalTimeValue(timeout);

        return writer.toString();
    }

    public BenchmarkListEntry cloneWith(Mode mode) {
        return new BenchmarkListEntry(userClassQName, generatedClassQName, method, mode,
                threads, threadGroups, threadGroupLabels,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks,
                jvm, jvmArgs, jvmArgsPrepend, jvmArgsAppend,
                params, tu, opsPerInvocation,
                timeout);
    }

    public BenchmarkListEntry cloneWith(WorkloadParams p) {
        BenchmarkListEntry br = new BenchmarkListEntry(userClassQName, generatedClassQName, method, mode,
                threads, threadGroups, threadGroupLabels,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks,
                jvm, jvmArgs, jvmArgsPrepend, jvmArgsAppend,
                params, tu, opsPerInvocation,
                timeout);
        br.workloadParams = p;
        return br;
    }

    public WorkloadParams getWorkloadParams() {
        return workloadParams;
    }

    @Override
    public int compareTo(BenchmarkListEntry o) {
        int v = mode.compareTo(o.mode);
        if (v != 0) {
            return v;
        }

        int v1 = getUsername().compareTo(o.getUsername());
        if (v1 != 0) {
            return v1;
        }

        if (workloadParams == null || o.workloadParams == null) {
            return 0;
        }

        return workloadParams.compareTo(o.workloadParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkListEntry record = (BenchmarkListEntry) o;

        if (mode != record.mode) return false;
        if (workloadParams != null ? !workloadParams.equals(record.workloadParams) : record.workloadParams != null) return false;
        if (userClassQName != null ? !userClassQName.equals(record.userClassQName) : record.userClassQName != null) return false;
        if (method != null ? !method.equals(record.method) : record.method != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userClassQName != null ? userClassQName.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (workloadParams != null ? workloadParams.hashCode() : 0);
        return result;
    }

    public String generatedTarget() {
        return generatedClassQName + "." + method + "_" + mode;
    }

    public String getUsername() {
        return userClassQName + "." + method;
    }

    public String getUserClassQName() {
        return userClassQName;
    }

    public Mode getMode() {
        return mode;
    }

    public int[] getThreadGroups() {
        return Arrays.copyOf(threadGroups, threadGroups.length);
    }

    public Optional<Collection<String>> getThreadGroupLabels() {
        return threadGroupLabels;
    }

    @Override
    public String toString() {
        return "{\'" + userClassQName + "." + method + "\', " + mode + ", " + workloadParams + "}";
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

    public Optional<String> getJvm() {
        return jvm;
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

    public Optional<Map<String, String[]>> getParams() {
        return params;
    }

    public Optional<TimeUnit> getTimeUnit() {
        return tu;
    }

    public Optional<Integer> getOperationsPerInvocation() {
        return opsPerInvocation;
    }

    public Optional<TimeValue> getTimeout() {
        return timeout;
    }

}
