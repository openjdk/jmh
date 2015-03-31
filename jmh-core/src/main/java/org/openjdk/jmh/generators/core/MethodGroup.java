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
package org.openjdk.jmh.generators.core;

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

class MethodGroup implements Comparable<MethodGroup> {
    private final ClassInfo ci;
    private final String name;
    private final Set<MethodInvocation> methods;
    private final EnumSet<Mode> modes;
    private final Map<String, String[]> params;
    private boolean strictFP;

    public MethodGroup(ClassInfo ci, String name) {
        this.ci = ci;
        this.name = name;
        this.methods = new TreeSet<MethodInvocation>();
        this.modes = EnumSet.noneOf(Mode.class);
        this.params = new TreeMap<String, String[]>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodGroup methodGroup = (MethodGroup) o;

        if (!name.equals(methodGroup.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(MethodGroup o) {
        return name.compareTo(o.name);
    }

    public void addMethod(MethodInfo method, int threads) {
        methods.add(new MethodInvocation(method, threads));
    }

    public Collection<MethodInfo> methods() {
        Collection<MethodInfo> result = new ArrayList<MethodInfo>();
        for (MethodInvocation m : methods) {
            result.add(m.method);
        }
        return result;
    }

    public Optional<Integer> getTotalThreadCount() {
        for (Threads ann : getAll(Threads.class)) {
            return Optional.of(ann.value());
        }
        return Optional.none();
    }

    public String getName() {
        return name;
    }

    public void addParamValues(String name, String[] value) {
        params.put(name, value);
    }

    public void addStrictFP(boolean sfp) {
        strictFP |= sfp;
    }

    public boolean isStrictFP() {
        return strictFP;
    }

    public void addModes(Mode eMode) {
        modes.add(eMode);
    }

    public void addModes(Mode[] eModes) {
        Collections.addAll(modes, eModes);
    }

    public Set<Mode> getModes() {
        return modes;
    }

    public int[] getThreads() {
        int[] threads = new int[methods.size()];
        int c = 0;
        for (MethodInvocation mi : methods) {
            threads[c++] = mi.threads;
        }
        return threads;
    }

    public Optional<Integer> getOperationsPerInvocation() {
        for (OperationsPerInvocation ann : getAll(OperationsPerInvocation.class)) {
            return Optional.of(ann.value());
        }
        return Optional.none();
    }

    public Optional<TimeUnit> getOutputTimeUnit() {
        for (OutputTimeUnit ann : getAll(OutputTimeUnit.class)) {
            return Optional.of(ann.value());
        }
        return Optional.none();
    }

    public Optional<Integer> getWarmupIterations() {
        for (Warmup ann : getAll(Warmup.class)) {
            if (ann.iterations() != Warmup.BLANK_ITERATIONS) {
                return Optional.of(ann.iterations());
            }
        }
        return Optional.none();
    }

    public Optional<TimeValue> getWarmupTime() {
        for (Warmup ann : getAll(Warmup.class)) {
            if (ann.time() != Warmup.BLANK_TIME) {
                return Optional.of(new TimeValue(ann.time(), ann.timeUnit()));
            }
        }
        return Optional.none();
    }

    public Optional<Integer> getWarmupBatchSize() {
        for (Warmup ann : getAll(Warmup.class)) {
            if (ann.batchSize() != Warmup.BLANK_BATCHSIZE) {
                return Optional.of(ann.batchSize());
            }
        }
        return Optional.none();
    }

    public Optional<Integer> getMeasurementIterations() {
        for (Measurement ann : getAll(Measurement.class)) {
            if (ann.iterations() != Measurement.BLANK_ITERATIONS) {
                return Optional.of(ann.iterations());
            }
        }
        return Optional.none();
    }

    public Optional<TimeValue> getMeasurementTime() {
        for (Measurement ann : getAll(Measurement.class)) {
            if (ann.time() != Measurement.BLANK_TIME) {
                return Optional.of(new TimeValue(ann.time(), ann.timeUnit()));
            }
        }
        return Optional.none();
    }

    public Optional<Integer> getMeasurementBatchSize() {
        for (Measurement ann : getAll(Measurement.class)) {
            if (ann.batchSize() != Measurement.BLANK_BATCHSIZE) {
                return Optional.of(ann.batchSize());
            }
        }
        return Optional.none();
    }

    public Optional<Integer> getForks() {
        for (Fork ann : getAll(Fork.class)) {
            if (ann.value() != Fork.BLANK_FORKS) {
                return Optional.of(ann.value());
            }
        }
        return Optional.none();
    }

    public Optional<Integer> getWarmupForks() {
        for (Fork ann : getAll(Fork.class)) {
            if (ann.warmups() != Fork.BLANK_FORKS) {
                return Optional.of(ann.warmups());
            }
        }
        return Optional.none();
    }

    public Optional<String> getJvm() {
        for (Fork ann : getAll(Fork.class)) {
            if (!ann.jvm().equals(Fork.BLANK_ARGS)) {
                return Optional.of(ann.jvm());
            }
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgs() {
        for (Fork ann : getAll(Fork.class)) {
            String[] args = ann.jvmArgs();
            if (!(args.length == 1 && args[0].equals(Fork.BLANK_ARGS))) {
                return Optional.<Collection<String>>of(Arrays.asList(args));
            }
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgsAppend() {
        for (Fork ann : getAll(Fork.class)) {
            String[] args = ann.jvmArgsAppend();
            if (!(args.length == 1 && args[0].equals(Fork.BLANK_ARGS))) {
                return Optional.<Collection<String>>of(Arrays.asList(args));
            }
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgsPrepend() {
        for (Fork ann : getAll(Fork.class)) {
            String[] args = ann.jvmArgsPrepend();
            if (!(args.length == 1 && args[0].equals(Fork.BLANK_ARGS))) {
                return Optional.<Collection<String>>of(Arrays.asList(args));
            }
        }
        return Optional.none();
    }

    public Optional<TimeValue> getTimeout() {
        for (Timeout ann : getAll(Timeout.class)) {
            return Optional.of(new TimeValue(ann.time(), ann.timeUnit()));
        }
        return Optional.none();
    }

    private <T extends Annotation> Collection<T> getAll(Class<T> annClass) {
        Collection<T> results = new ArrayList<T>();
        for (MethodInvocation mi : methods) {
            Collection<T> anns = BenchmarkGeneratorUtils.getAnnSuperAll(mi.method, ci, annClass);
            if (!(results.isEmpty() || anns.isEmpty() || results.equals(anns))) {
                throw new GenerationException("Colliding annotations: " + anns + " vs. " + results, mi.method);
            }
            results = anns;
        }
        return results;
    }

    public Optional<Map<String, String[]>> getParams() {
        Map<String, String[]> map = new TreeMap<String, String[]>();

        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = e.getKey();
            String[] values = e.getValue();
            if (values.length == 1 && values[0].equalsIgnoreCase(Param.BLANK_ARGS)) {
                map.put(key, new String[0]);
            } else {
                map.put(key, values);
            }
        }

        if (params.isEmpty()) {
            return Optional.none();
        } else {
            return Optional.of(map);
        }
    }

}
