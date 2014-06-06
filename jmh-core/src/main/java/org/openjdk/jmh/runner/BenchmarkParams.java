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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.util.Utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class BenchmarkParams extends BenchmarkParamsL4 {
    static {
        Utils.check(BenchmarkParams.class, "benchmark", "generatedTarget", "synchIterations");
        Utils.check(BenchmarkParams.class, "threads", "threadGroups", "forks", "warmupForks");
        Utils.check(BenchmarkParams.class, "warmup", "measurement");
        Utils.check(BenchmarkParams.class, "mode", "params");
        Utils.check(BenchmarkParams.class, "timeUnit", "opsPerInvocation");
        Utils.check(BenchmarkParams.class, "jvmArgsPrepend", "jvmArgs", "jvmArgsAppend");
    }

    public BenchmarkParams(BenchmarkParams other) {
        super(other);
    }

    public BenchmarkParams(String benchmark, String generatedTarget, boolean synchIterations,
                             int threads, int[] threadGroups, int forks, int warmupForks,
                             IterationParams warmup, IterationParams measurement,
                             Mode mode, ActualParams params,
                             TimeUnit timeUnit, int opsPerInvocation,
                             Collection<String> jvmArgsPrepend, Collection<String> jvmArgs, Collection<String> jvmArgsAppend) {
        super(benchmark, generatedTarget, synchIterations,
                threads, threadGroups, forks, warmupForks,
                warmup, measurement,
                mode, params,
                timeUnit, opsPerInvocation,
                jvmArgsPrepend, jvmArgs, jvmArgsAppend);
    }
}

abstract class BenchmarkParamsL4 extends BenchmarkParamsL3 {
    private int markerEnd;
    public BenchmarkParamsL4(String benchmark, String generatedTarget, boolean synchIterations,
                             int threads, int[] threadGroups, int forks, int warmupForks,
                             IterationParams warmup, IterationParams measurement,
                             Mode mode, ActualParams params,
                             TimeUnit timeUnit, int opsPerInvocation,
                             Collection<String> jvmArgsPrepend, Collection<String> jvmArgs, Collection<String> jvmArgsAppend) {
        super(benchmark, generatedTarget, synchIterations,
                threads, threadGroups, forks, warmupForks,
                warmup, measurement,
                mode, params,
                timeUnit, opsPerInvocation,
                jvmArgsPrepend, jvmArgs, jvmArgsAppend);
    }

    public BenchmarkParamsL4(BenchmarkParams other) {
        super(other);
    }
}

abstract class BenchmarkParamsL3 extends BenchmarkParamsL2 {
    private boolean q001, q002, q003, q004, q005, q006, q007, q008;
    private boolean q011, q012, q013, q014, q015, q016, q017, q018;
    private boolean q021, q022, q023, q024, q025, q026, q027, q028;
    private boolean q031, q032, q033, q034, q035, q036, q037, q038;
    private boolean q041, q042, q043, q044, q045, q046, q047, q048;
    private boolean q051, q052, q053, q054, q055, q056, q057, q058;
    private boolean q061, q062, q063, q064, q065, q066, q067, q068;
    private boolean q071, q072, q073, q074, q075, q076, q077, q078;
    private boolean q101, q102, q103, q104, q105, q106, q107, q108;
    private boolean q111, q112, q113, q114, q115, q116, q117, q118;
    private boolean q121, q122, q123, q124, q125, q126, q127, q128;
    private boolean q131, q132, q133, q134, q135, q136, q137, q138;
    private boolean q141, q142, q143, q144, q145, q146, q147, q148;
    private boolean q151, q152, q153, q154, q155, q156, q157, q158;
    private boolean q161, q162, q163, q164, q165, q166, q167, q168;
    private boolean q171, q172, q173, q174, q175, q176, q177, q178;

    public BenchmarkParamsL3(String benchmark, String generatedTarget, boolean synchIterations,
                             int threads, int[] threadGroups, int forks, int warmupForks,
                             IterationParams warmup, IterationParams measurement,
                             Mode mode, ActualParams params,
                             TimeUnit timeUnit, int opsPerInvocation,
                             Collection<String> jvmArgsPrepend, Collection<String> jvmArgs, Collection<String> jvmArgsAppend) {
        super(benchmark, generatedTarget, synchIterations,
                threads, threadGroups, forks, warmupForks,
                warmup, measurement,
                mode, params,
                timeUnit, opsPerInvocation,
                jvmArgsPrepend, jvmArgs, jvmArgsAppend);
    }

    public BenchmarkParamsL3(BenchmarkParams other) {
        super(other);
    }
}

abstract class BenchmarkParamsL1 extends BenchmarkParamsL0 {
    private boolean p001, p002, p003, p004, p005, p006, p007, p008;
    private boolean p011, p012, p013, p014, p015, p016, p017, p018;
    private boolean p021, p022, p023, p024, p025, p026, p027, p028;
    private boolean p031, p032, p033, p034, p035, p036, p037, p038;
    private boolean p041, p042, p043, p044, p045, p046, p047, p048;
    private boolean p051, p052, p053, p054, p055, p056, p057, p058;
    private boolean p061, p062, p063, p064, p065, p066, p067, p068;
    private boolean p071, p072, p073, p074, p075, p076, p077, p078;
    private boolean p101, p102, p103, p104, p105, p106, p107, p108;
    private boolean p111, p112, p113, p114, p115, p116, p117, p118;
    private boolean p121, p122, p123, p124, p125, p126, p127, p128;
    private boolean p131, p132, p133, p134, p135, p136, p137, p138;
    private boolean p141, p142, p143, p144, p145, p146, p147, p148;
    private boolean p151, p152, p153, p154, p155, p156, p157, p158;
    private boolean p161, p162, p163, p164, p165, p166, p167, p168;
    private boolean p171, p172, p173, p174, p175, p176, p177, p178;
}

abstract class BenchmarkParamsL0 {
    private int markerBegin;
}

abstract class BenchmarkParamsL2 extends BenchmarkParamsL1 implements Serializable, Comparable<BenchmarkParams> {
    protected final String benchmark;
    protected final String generatedTarget;
    protected final boolean synchIterations;
    protected final int threads;
    protected final int[] threadGroups;
    protected final int forks;
    protected final int warmupForks;
    protected final IterationParams warmup;
    protected final IterationParams measurement;
    protected final Mode mode;
    protected final ActualParams params;
    protected final TimeUnit timeUnit;
    protected final int opsPerInvocation;
    protected final Collection<String> jvmArgsPrepend;
    protected final Collection<String> jvmArgs;
    protected final Collection<String> jvmArgsAppend;

    public BenchmarkParamsL2(String benchmark, String generatedTarget, boolean synchIterations,
                             int threads, int[] threadGroups, int forks, int warmupForks,
                             IterationParams warmup, IterationParams measurement,
                             Mode mode, ActualParams params,
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

    public BenchmarkParamsL2(BenchmarkParams other) {
        this.benchmark = other.benchmark;
        this.generatedTarget = other.generatedTarget;
        this.synchIterations = other.synchIterations;
        this.threads = other.threads;
        this.threadGroups = other.threadGroups;
        this.forks = other.forks;
        this.warmupForks = other.warmupForks;
        this.warmup = other.warmup;
        this.measurement = other.measurement;
        this.mode = other.mode;
        this.params = other.params;
        this.timeUnit = other.timeUnit;
        this.opsPerInvocation = other.opsPerInvocation;
        this.jvmArgsPrepend = other.jvmArgsPrepend;
        this.jvmArgs = other.jvmArgs;
        this.jvmArgsAppend = other.jvmArgsAppend;
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
