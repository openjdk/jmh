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
        Threads ann = getFinal(Threads.class);
        if (ann != null) {
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
        OperationsPerInvocation ann = getFinal(OperationsPerInvocation.class);
        return (ann != null) ? Optional.of(ann.value()) : Optional.<Integer>none();
    }

    public Optional<TimeUnit> getOutputTimeUnit() {
        OutputTimeUnit ann = getFinal(OutputTimeUnit.class);
        return (ann != null) ? Optional.of(ann.value()) : Optional.<TimeUnit>none();
    }

    public Optional<Integer> getWarmupIterations() {
        Warmup ann = getFinal(Warmup.class);
        if (ann != null && ann.iterations() != Warmup.BLANK_ITERATIONS) {
            return Optional.of(ann.iterations());
        }
        return Optional.none();
    }

    public Optional<TimeValue> getWarmupTime() {
        Warmup ann = getFinal(Warmup.class);
        if (ann != null && ann.time() != Warmup.BLANK_TIME) {
            return Optional.of(new TimeValue(ann.time(), ann.timeUnit()));
        }
        return Optional.none();
    }

    public Optional<Integer> getWarmupBatchSize() {
        Warmup ann = getFinal(Warmup.class);
        if (ann != null && ann.batchSize() != Warmup.BLANK_BATCHSIZE) {
            return Optional.of(ann.batchSize());
        }
        return Optional.none();
    }

    public Optional<Integer> getMeasurementIterations() {
        Measurement ann = getFinal(Measurement.class);
        if (ann != null && ann.iterations() != Measurement.BLANK_ITERATIONS) {
            return Optional.of(ann.iterations());
        }
        return Optional.none();
    }

    public Optional<TimeValue> getMeasurementTime() {
        Measurement ann = getFinal(Measurement.class);
        if (ann != null && ann.time() != Measurement.BLANK_TIME) {
            return Optional.of(new TimeValue(ann.time(), ann.timeUnit()));
        }
        return Optional.none();
    }

    public Optional<Integer> getMeasurementBatchSize() {
        Measurement ann = getFinal(Measurement.class);
        if (ann != null && ann.batchSize() != Measurement.BLANK_BATCHSIZE) {
            return Optional.of(ann.batchSize());
        }
        return Optional.none();
    }

    public Optional<Integer> getForks() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && ann.value() != Fork.BLANK_FORKS) {
            return Optional.of(ann.value());
        }
        return Optional.none();
    }

    public Optional<Integer> getWarmupForks() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && ann.warmups() != Fork.BLANK_FORKS) {
            return Optional.of(ann.warmups());
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgs() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !(ann.jvmArgs().length == 1 && ann.jvmArgs()[0].equals(Fork.BLANK_ARGS))) {
            return Optional.<Collection<String>>of(Arrays.asList(ann.jvmArgs()));
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgsAppend() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !(ann.jvmArgsAppend().length == 1 && ann.jvmArgsAppend()[0].equals(Fork.BLANK_ARGS))) {
            return Optional.<Collection<String>>of(Arrays.asList(ann.jvmArgsAppend()));
        }
        return Optional.none();
    }

    public Optional<Collection<String>> getJvmArgsPrepend() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !(ann.jvmArgsPrepend().length == 1 && ann.jvmArgsPrepend()[0].equals(Fork.BLANK_ARGS))) {
            return Optional.<Collection<String>>of(Arrays.asList(ann.jvmArgsPrepend()));
        }
        return Optional.none();
    }

    private <T extends Annotation> T getFinal(Class<T> annClass) {
        T finalAnn = null;
        for (MethodInvocation mi : methods) {
            T ann = BenchmarkGeneratorUtils.getAnnSuper(mi.method, ci, annClass);
            if (ann != null) {
                // FIXME: Temporalily disabled before we figure the proxy annotations equals/hashCode
                if (false && finalAnn != null && !finalAnn.equals(ann)) {
                    if (!finalAnn.equals(ann)) {
                        throw new GenerationException("Colliding annotations: " + ann + " vs. " + finalAnn, mi.method);
                    }
                }
                finalAnn = ann;
            }
        }
        return finalAnn;
    }

    public Optional<Map<String, String[]>> getParams() {
        Map<String, String[]> map = new TreeMap<String, String[]>();

        for (String key : params.keySet()) {
            String[] values = params.get(key);
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
