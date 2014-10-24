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
import org.openjdk.jmh.util.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class BenchmarkListEntry implements Comparable<BenchmarkListEntry> {

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
    private final Optional<String> jvm;
    private final Optional<Collection<String>> jvmArgs;
    private final Optional<Collection<String>> jvmArgsPrepend;
    private final Optional<Collection<String>> jvmArgsAppend;
    private final Optional<Map<String, String[]>> params;
    private final Optional<TimeUnit> tu;
    private final Optional<Integer> opsPerInvocation;

    private WorkloadParams workloadParams;

    public BenchmarkListEntry(String userName, String generatedName, Mode mode, int[] threadGroups, Optional<Integer> threads,
                              Optional<Integer> warmupIterations, Optional<TimeValue> warmupTime, Optional<Integer> warmupBatchSize,
                              Optional<Integer> measurementIterations, Optional<TimeValue> measurementTime, Optional<Integer> measurementBatchSize,
                              Optional<Integer> forks, Optional<Integer> warmupForks,
                              Optional<String> jvm, Optional<Collection<String>> jvmArgs, Optional<Collection<String>> jvmArgsPrepend, Optional<Collection<String>> jvmArgsAppend,
                              Optional<Map<String, String[]>> params, Optional<TimeUnit> tu, Optional<Integer> opsPerInv) {
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
        this.jvm = jvm;
        this.jvmArgs = jvmArgs;
        this.jvmArgsPrepend = jvmArgsPrepend;
        this.jvmArgsAppend = jvmArgsAppend;
        this.params = params;
        this.workloadParams = new WorkloadParams();
        this.tu = tu;
        this.opsPerInvocation = opsPerInv;
    }

    public BenchmarkListEntry(String line) {
        String[] args = line.split(BR_SEPARATOR);

        if (args.length != 20) {
            throw new IllegalStateException("Mismatched format for the line: " + line);
        }

        this.workloadParams = new WorkloadParams();
        this.userName = args[0].trim();
        this.generatedName = args[1].trim();
        this.mode = Mode.deepValueOf(args[2].trim());
        this.threadGroups = Utils.unmarshalIntArray(args[3]);
        this.threads = Optional.of(args[4], INTEGER_UNMARSHALLER);
        this.warmupIterations = Optional.of(args[5], INTEGER_UNMARSHALLER);
        this.warmupTime = Optional.of(args[6], TIME_VALUE_UNMARSHALLER);
        this.warmupBatchSize = Optional.of(args[7], INTEGER_UNMARSHALLER);
        this.measurementIterations = Optional.of(args[8], INTEGER_UNMARSHALLER);
        this.measurementTime = Optional.of(args[9], TIME_VALUE_UNMARSHALLER);
        this.measurementBatchSize = Optional.of(args[10], INTEGER_UNMARSHALLER);
        this.forks = Optional.of(args[11], INTEGER_UNMARSHALLER);
        this.warmupForks = Optional.of(args[12], INTEGER_UNMARSHALLER);
        this.jvm = Optional.of(args[13], STRING_UNMARSHALLER);
        this.jvmArgs = Optional.of(args[14], STRING_COLLECTION_UNMARSHALLER);
        this.jvmArgsPrepend = Optional.of(args[15], STRING_COLLECTION_UNMARSHALLER);
        this.jvmArgsAppend = Optional.of(args[16], STRING_COLLECTION_UNMARSHALLER);
        this.params = Optional.of(args[17], PARAM_COLLECTION_UNMARSHALLER);
        this.tu = Optional.of(args[18], TIMEUNIT_UNMARSHALLER);
        this.opsPerInvocation = Optional.of(args[19], INTEGER_UNMARSHALLER);
    }

    public BenchmarkListEntry(String userName, String generatedName, Mode mode) {
        this(userName, generatedName, mode, new int[]{}, Optional.<Integer>none(),
                Optional.<Integer>none(), Optional.<TimeValue>none(), Optional.<Integer>none(), Optional.<Integer>none(), Optional.<TimeValue>none(), Optional.<Integer>none(),
                Optional.<Integer>none(), Optional.<Integer>none(),
                Optional.<String>none(), Optional.<Collection<String>>none(), Optional.<Collection<String>>none(), Optional.<Collection<String>>none(),
                Optional.<Map<String, String[]>>none(), Optional.<TimeUnit>none(), Optional.<Integer>none());
    }

    public String toLine() {
        return userName + BR_SEPARATOR + generatedName + BR_SEPARATOR + mode + BR_SEPARATOR + Utils.marshalIntArray(threadGroups) + BR_SEPARATOR +
                threads + BR_SEPARATOR + warmupIterations + BR_SEPARATOR + warmupTime + BR_SEPARATOR + warmupBatchSize + BR_SEPARATOR +
                measurementIterations + BR_SEPARATOR + measurementTime + BR_SEPARATOR + measurementBatchSize + BR_SEPARATOR +
                forks + BR_SEPARATOR + warmupForks + BR_SEPARATOR +
                jvm.toString(STRING_MARSHALLER) + BR_SEPARATOR +
                jvmArgs.toString(STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                jvmArgsPrepend.toString(STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                jvmArgsAppend.toString(STRING_COLLECTION_MARSHALLER) + BR_SEPARATOR +
                params.toString(PARAM_COLLECTION_MARSHALLER) + BR_SEPARATOR + tu.toString(TIMEUNIT_MARSHALLER) + BR_SEPARATOR +
                opsPerInvocation;
    }

    public BenchmarkListEntry cloneWith(Mode mode) {
        return new BenchmarkListEntry(userName, generatedName, mode, threadGroups, threads,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks,
                jvm, jvmArgs, jvmArgsPrepend, jvmArgsAppend,
                params, tu, opsPerInvocation);
    }

    public BenchmarkListEntry cloneWith(WorkloadParams p) {
        BenchmarkListEntry br = new BenchmarkListEntry(userName, generatedName, mode, threadGroups, threads,
                warmupIterations, warmupTime, warmupBatchSize,
                measurementIterations, measurementTime, measurementBatchSize,
                forks, warmupForks,
                jvm, jvmArgs, jvmArgsPrepend, jvmArgsAppend,
                params, tu, opsPerInvocation);
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

        int v1 = userName.compareTo(o.userName);
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
        if (userName != null ? !userName.equals(record.userName) : record.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (workloadParams != null ? workloadParams.hashCode() : 0);
        return result;
    }

    public String generatedTarget() {
        return generatedName + "_" + mode;
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
        return "BenchmarkListEntry{" +
                "userName='" + userName + '\'' +
                ", generatedName='" + generatedName + '\'' +
                ", mode=" + mode +
                ", workloadParams=" + workloadParams +
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

    static final Optional.Unmarshaller<Integer> INTEGER_UNMARSHALLER = new Optional.Unmarshaller<Integer>() {
        @Override
        public Integer valueOf(String s) {
            return Integer.valueOf(s);
        }
    };

    static final Optional.Unmarshaller<TimeValue> TIME_VALUE_UNMARSHALLER = new Optional.Unmarshaller<TimeValue>() {
        @Override
        public TimeValue valueOf(String s) {
            return TimeValue.fromString(s);
        }
    };

    static final Optional.Unmarshaller<TimeUnit> TIMEUNIT_UNMARSHALLER = new Optional.Unmarshaller<TimeUnit>() {
        @Override
        public TimeUnit valueOf(String s) {
            return TimeUnit.valueOf(s);
        }
    };

    static final Optional.Marshaller<TimeUnit> TIMEUNIT_MARSHALLER = new Optional.Marshaller<TimeUnit>() {
        @Override
        public String valueOf(TimeUnit val) {
            return val.toString();
        }
    };

    static final Optional.Unmarshaller<String> STRING_UNMARSHALLER = new Optional.Unmarshaller<String>() {
        @Override
        public String valueOf(String s) {
            return s;
        }
    };

    static final Optional.Marshaller<String> STRING_MARSHALLER = new Optional.Marshaller<String>() {
        @Override
        public String valueOf(String s) {
            return s;
        }
    };

    static final Optional.Unmarshaller<Collection<String>> STRING_COLLECTION_UNMARSHALLER = new Optional.Unmarshaller<Collection<String>>() {
        @Override
        public Collection<String> valueOf(String s) {
            return Arrays.asList(s.split("===SEP==="));
        }
    };

    static final Optional.Marshaller<Collection<String>> STRING_COLLECTION_MARSHALLER = new Optional.Marshaller<Collection<String>>() {
        @Override
        public String valueOf(Collection<String> src) {
            StringBuilder sb = new StringBuilder();
            for (String s : src) {
                sb.append(s).append("===SEP===");
            }
            return sb.toString();
        }
    };

    static final Optional.Unmarshaller<Map<String, String[]>> PARAM_COLLECTION_UNMARSHALLER = new Optional.Unmarshaller<Map<String, String[]>>() {
        @Override
        public Map<String, String[]> valueOf(String s) {
            Map<String, String[]> map = new TreeMap<String, String[]>();
            String[] pairs = s.split("===PAIR-SEP===");
            for (String pair : pairs) {
                String[] kv = pair.split("===SEP-K===");
                if (kv[1].equalsIgnoreCase("===EMPTY===")) {
                    map.put(kv[0], new String[0]);
                } else {
                    map.put(kv[0], kv[1].split("===SEP-V==="));
                }
            }
            return map;
        }
    };

    static final Optional.Marshaller<Map<String, String[]>> PARAM_COLLECTION_MARSHALLER = new Optional.Marshaller<Map<String, String[]>>() {
        @Override
        public String valueOf(Map<String, String[]> src) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String[]> e : src.entrySet()) {
                sb.append(e.getKey());
                sb.append("===SEP-K===");
                if (e.getValue().length == 0) {
                    sb.append("===EMPTY===");
                } else {
                    for (String v : e.getValue()) {
                        sb.append(v);
                        sb.append("===SEP-V===");
                    }
                }
                sb.append("===PAIR-SEP===");
            }
            return sb.toString();
        }
    };
}
