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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class BenchmarkParams implements Serializable, Comparable<BenchmarkParams> {

    private final String benchmark;
    private final String generatedTarget;
    private final boolean synchIterations;
    private final int threads;
    private final int[] threadGroups;
    private final int forks;
    private final int warmupForks;
    private final IterationParams warmup;
    private final IterationParams measurement;
    private final Mode mode;
    private final ActualParams params;
    private final TimeUnit timeUnit;
    private final int opsPerInvocation;
    private final Collection<String> jvmArgsPrepend;
    private final Collection<String> jvmArgs;
    private final Collection<String> jvmArgsAppend;

    public BenchmarkParams(String benchmark, String generatedTarget, boolean synchIterations, int threads, int[] threadGroups, int forks, int warmupForks,
                           IterationParams warmup, IterationParams measurement, Mode mode, ActualParams params,
                           TimeUnit timeUnit, int opsPerInvocation,
                           Collection<String> jvmArgsPrepend, Collection<String> jvmArgs, Collection<String> jvmArgsAppend) {
        this.benchmark = benchmark;
        this.generatedTarget = generatedTarget;
        this.synchIterations = synchIterations;
        this.threads = threads;
        this.threadGroups = threadGroups;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.warmup = warmup;
        this.measurement = measurement;
        this.mode = mode;
        this.params = params;
        this.timeUnit = timeUnit;
        this.opsPerInvocation = opsPerInvocation;
        this.jvmArgsPrepend = jvmArgsPrepend;
        this.jvmArgs = jvmArgs;
        this.jvmArgsAppend = jvmArgsAppend;
    }

    public boolean shouldSynchIterations() {
        return synchIterations;
    }

    public IterationParams getWarmup() {
        return warmup;
    }

    public IterationParams getMeasurement() {
        return measurement;
    }

    public int getThreads() {
        return threads;
    }

    public int[] getThreadGroups() {
        return Arrays.copyOf(threadGroups, threadGroups.length);
    }

    public int getForks() {
        return forks;
    }

    public int getWarmupForks() {
        return warmupForks;
    }

    public Mode getMode() {
        return mode;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public int getOpsPerInvocation() {
        return opsPerInvocation;
    }

    public ActualParams getParams() {
        return params;
    }

    public String getParam(String key) {
        if (params != null) {
            return params.get(key);
        } else {
            return null;
        }
    }

    public String generatedClass() {
        String s = generatedTarget;
        return s.substring(0, s.lastIndexOf('.'));
    }

    public String generatedMethod() {
        String s = generatedTarget;
        return s.substring(s.lastIndexOf('.') + 1);
    }

    public Collection<String> getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public Collection<String> getJvmArgs() {
        return jvmArgs;
    }

    public Collection<String> getJvmArgsAppend() {
        return jvmArgsAppend;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BenchmarkParams that = (BenchmarkParams) o;

        if (!benchmark.equals(that.benchmark)) return false;
        if (mode != that.mode) return false;
        if (!params.equals(that.params)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = benchmark.hashCode();
        result = 31 * result + mode.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }

    public long estimatedTimeSingleFork() {
        if (mode == Mode.SingleShotTime) {
            // No way to tell how long it will execute,
            // guess anything, and let ETA compensation to catch up.
            return (warmup.getCount() + measurement.getCount()) * TimeUnit.MILLISECONDS.toNanos(1);
        }

        return (warmup.getCount() * warmup.getTime().convertTo(TimeUnit.NANOSECONDS) +
                measurement.getCount() * measurement.getTime().convertTo(TimeUnit.NANOSECONDS));
    }

    public long estimatedTime() {
        return (Math.max(1, forks) + warmupForks) * estimatedTimeSingleFork();
    }

    @Override
    public int compareTo(BenchmarkParams o) {
        int v = mode.compareTo(o.mode);
        if (v != 0) {
            return v;
        }

        int v1 = benchmark.compareTo(o.benchmark);
        if (v1 != 0) {
            return v1;
        }

        if (params == null || o.params == null) {
            return 0;
        }

        return params.compareTo(o.params);
    }

}
