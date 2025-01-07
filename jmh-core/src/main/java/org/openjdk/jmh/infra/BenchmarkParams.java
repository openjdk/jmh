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
package org.openjdk.jmh.infra;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Version;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark parameters.
 *
 * <p>{@link BenchmarkParams} handles the parameters used in the current run.</p>
 * <p>This class is dual-purpose:</p>
 * <ol>
 *     <li>It acts as the interface between host JVM and forked JVM, so that the latter
 *     would not have to figure out the benchmark configuration again</li>
 *     <li>It can be injected into benchmark methods to access the runtime configuration
 *     info about the benchmark</li>
 * </ol>
 */
public final class BenchmarkParams extends BenchmarkParamsL2 {
    private static final long serialVersionUID = -53511295235994554L;

    byte b3_00, b3_01, b3_02, b3_03, b3_04, b3_05, b3_06, b3_07, b3_08, b3_09, b3_0a, b3_0b, b3_0c, b3_0d, b3_0e, b3_0f;
    long b3_10, b3_11, b3_12, b3_13, b3_14, b3_15, b3_16, b3_17, b3_18, b3_19, b3_1a, b3_1b, b3_1c, b3_1d, b3_1e, b3_1f;
    long b3_20, b3_21, b3_22, b3_23, b3_24, b3_25, b3_26, b3_27, b3_28, b3_29, b3_2a, b3_2b, b3_2c, b3_2d, b3_2e, b3_2f;

    public BenchmarkParams(String benchmark, String generatedTarget, boolean synchIterations,
                           int threads, int[] threadGroups, Collection<String> threadGroupLabels,
                           int forks, int warmupForks,
                           IterationParams warmup, IterationParams measurement,
                           Mode mode, WorkloadParams params,
                           TimeUnit timeUnit, int opsPerInvocation,
                           String jvm, Collection<String> jvmArgs,
                           String jdkVersion, String vmName, String vmVersion, String jmhVersion,
                           TimeValue timeout) {
        super(benchmark, generatedTarget, synchIterations,
                threads, threadGroups, threadGroupLabels,
                forks, warmupForks,
                warmup, measurement,
                mode, params,
                timeUnit, opsPerInvocation,
                jvm, jvmArgs,
                jdkVersion, vmName, vmVersion, jmhVersion,
                timeout);
    }
}

abstract class BenchmarkParamsL2 extends BenchmarkParamsL1 implements Serializable, Comparable<BenchmarkParams> {
    private static final long serialVersionUID = -1068219503090299117L;

    protected final String benchmark;
    protected final String generatedTarget;
    protected final boolean synchIterations;
    protected final int threads;
    protected final int[] threadGroups;
    protected final Collection<String> threadGroupLabels;
    protected final int forks;
    protected final int warmupForks;
    protected final IterationParams warmup;
    protected final IterationParams measurement;
    protected final Mode mode;
    protected final WorkloadParams params;
    protected final TimeUnit timeUnit;
    protected final int opsPerInvocation;
    protected final String jvm;
    protected final Collection<String> jvmArgs;
    protected final String jdkVersion;
    protected final String jmhVersion;
    protected final String vmName;
    protected final String vmVersion;
    protected final TimeValue timeout;

    public BenchmarkParamsL2(String benchmark, String generatedTarget, boolean synchIterations,
                             int threads, int[] threadGroups, Collection<String> threadGroupLabels,
                             int forks, int warmupForks,
                             IterationParams warmup, IterationParams measurement,
                             Mode mode, WorkloadParams params,
                             TimeUnit timeUnit, int opsPerInvocation,
                             String jvm, Collection<String> jvmArgs,
                             String jdkVersion, String vmName, String vmVersion, String jmhVersion,
                             TimeValue timeout) {
        this.benchmark = benchmark;
        this.generatedTarget = generatedTarget;
        this.synchIterations = synchIterations;
        this.threads = threads;
        this.threadGroups = threadGroups;
        this.threadGroupLabels = threadGroupLabels;
        this.forks = forks;
        this.warmupForks = warmupForks;
        this.warmup = warmup;
        this.measurement = measurement;
        this.mode = mode;
        this.params = params;
        this.timeUnit = timeUnit;
        this.opsPerInvocation = opsPerInvocation;
        this.jvm = jvm;
        this.jvmArgs = jvmArgs;
        this.jdkVersion = jdkVersion;
        this.vmName = vmName;
        this.vmVersion = vmVersion;
        this.jmhVersion = jmhVersion;
        this.timeout = timeout;
    }

    /**
     * @return how long to wait for iteration to complete
     */
    public TimeValue getTimeout() {
        return timeout;
    }

    /**
     * @return do we synchronize iterations?
     */
    public boolean shouldSynchIterations() {
        return synchIterations;
    }

    /**
     * @return iteration parameters for warmup phase
     */
    public IterationParams getWarmup() {
        return warmup;
    }

    /**
     * @return iteration parameters for measurement phase
     */
    public IterationParams getMeasurement() {
        return measurement;
    }

    /**
     * @return total measurement thread count
     */
    public int getThreads() {
        return threads;
    }

    /**
     * @return thread distribution within the group
     * @see org.openjdk.jmh.runner.options.ChainedOptionsBuilder#threadGroups(int...)
     */
    public int[] getThreadGroups() {
        return Arrays.copyOf(threadGroups, threadGroups.length);
    }

    /**
     * @return subgroup thread labels
     * @see #getThreadGroups()
     */
    public Collection<String> getThreadGroupLabels() {
        return Collections.unmodifiableCollection(threadGroupLabels);
    }

    /**
     * @return number of forked VM runs, which we measure
     */
    public int getForks() {
        return forks;
    }

    /**
     * @return number of forked VM runs, which we discard from the result
     */
    public int getWarmupForks() {
        return warmupForks;
    }

    /**
     * @return benchmark mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * @return benchmark name
     */
    public String getBenchmark() {
        return benchmark;
    }

    /**
     * @return timeUnit used in results
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * @return operations per invocation used
     */
    public int getOpsPerInvocation() {
        return opsPerInvocation;
    }

    /**
     * @return all workload parameters
     */
    public Collection<String> getParamsKeys() {
        return params.keys();
    }

    /**
     * @param key parameter key; usually the field name
     * @return parameter value for given key
     */
    public String getParam(String key) {
        if (params != null) {
            return params.get(key);
        } else {
            return null;
        }
    }

    /**
     * @return generated benchmark name
     */
    public String generatedBenchmark() {
        return generatedTarget;
    }

    /**
     * @return JVM executable path
     */
    public String getJvm() {
        return jvm;
    }

    /**
     * @return JMH version identical to {@link Version#getPlainVersion()}, but output format should
     *          get there input via bean for testing purposes.
     */
    public String getJmhVersion() {
        return jmhVersion;
    }

    /**
     * @return JVM options
     */
    public Collection<String> getJvmArgs() {
        return Collections.unmodifiableCollection(jvmArgs);
    }

    /**
     * @return version information as returned by the effective target JVM,
     *         via system property {@code java.version} and {@code java.vm.version}
     */
    public String getJdkVersion() {
        return jdkVersion;
    }

    /**
     * @return version information as returned by the effective target JVM,
     *         via system property {@code java.vm.version}
     */
    public String getVmVersion() {
        return vmVersion;
    }

    /**
     * @return name information as returned by the effective target JVM,
     *         via system property {@code java.vm.name}
     */
    public String getVmName() {
        return vmName;
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

    public String id() {
        StringBuilder sb = new StringBuilder();
        appendSanitized(sb, benchmark);
        sb.append("-");
        sb.append(mode);
        for (String key : params.keys()) {
            sb.append("-");
            appendSanitized(sb, key);
            sb.append("-");
            appendSanitized(sb, params.get(key));
        }
        return sb.toString();
    }

    private static void appendSanitized(StringBuilder builder, String s) {
        try {
            builder.append(URLEncoder.encode(s, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}

abstract class BenchmarkParamsL1 {
    byte b1_00, b1_01, b1_02, b1_03, b1_04, b1_05, b1_06, b1_07, b1_08, b1_09, b1_0a, b1_0b, b1_0c, b1_0d, b1_0e, b1_0f;
    long b1_10, b1_11, b1_12, b1_13, b1_14, b1_15, b1_16, b1_17, b1_18, b1_19, b1_1a, b1_1b, b1_1c, b1_1d, b1_1e, b1_1f;
    long b1_20, b1_21, b1_22, b1_23, b1_24, b1_25, b1_26, b1_27, b1_28, b1_29, b1_2a, b1_2b, b1_2c, b1_2d, b1_2e, b1_2f;
}
