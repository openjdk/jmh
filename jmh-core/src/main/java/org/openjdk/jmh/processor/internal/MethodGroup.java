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
package org.openjdk.jmh.processor.internal;

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.AnnotationUtils;
import org.openjdk.jmh.util.internal.Option;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class MethodGroup implements Comparable<MethodGroup> {
    private final String name;
    private final Set<MethodInvocation> methods;
    private final EnumSet<Mode> modes;
    private boolean strictFP;

    MethodGroup(String name) {
        this.name = name;
        this.methods = new TreeSet<MethodInvocation>();
        this.modes = EnumSet.noneOf(Mode.class);
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

    public void addMethod(Element method, int threads) {
        methods.add(new MethodInvocation(method, threads));
    }

    public Collection<Element> methods() {
        Collection<Element> result = new ArrayList<Element>();
        for (MethodInvocation m : methods) {
            result.add(m.element);
        }
        return result;
    }

    public Option<Integer> getTotalThreadCount() {
        Threads ann = getFinal(Threads.class);
        if (ann != null) {
            return Option.of(ann.value());
        }
        return Option.none();
    }

    public String getName() {
        return name;
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

    public long getOperationsPerInvocation() {
        OperationsPerInvocation ann = getFinal(OperationsPerInvocation.class);
        return (ann != null) ? ann.value() : 1;
    }

    public TimeUnit getOutputTimeUnit() {
        OutputTimeUnit ann = getFinal(OutputTimeUnit.class);
        return (ann != null) ? ann.value() : TimeUnit.MILLISECONDS;
    }

    public Option<Integer> getWarmupIterations() {
        Warmup ann = getFinal(Warmup.class);
        if (ann != null && ann.iterations() != Warmup.BLANK_ITERATIONS) {
            return Option.of(ann.iterations());
        }
        return Option.none();
    }

    public Option<TimeValue> getWarmupTime() {
        Warmup ann = getFinal(Warmup.class);
        if (ann != null && ann.time() != Warmup.BLANK_TIME) {
            return Option.of(new TimeValue(ann.time(), ann.timeUnit()));
        }
        return Option.none();
    }

    public Option<Integer> getMeasurementIterations() {
        Measurement ann = getFinal(Measurement.class);
        if (ann != null && ann.iterations() != Measurement.BLANK_ITERATIONS) {
            return Option.of(ann.iterations());
        }
        return Option.none();
    }

    public Option<TimeValue> getMeasurementTime() {
        Measurement ann = getFinal(Measurement.class);
        if (ann != null && ann.time() != Measurement.BLANK_TIME) {
            return Option.of(new TimeValue(ann.time(), ann.timeUnit()));
        }
        return Option.none();
    }

    public Option<Integer> getForks() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && ann.value() != Fork.BLANK_FORKS) {
            return Option.of(ann.value());
        }
        return Option.none();
    }

    public Option<Integer> getWarmupForks() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && ann.warmups() != Fork.BLANK_FORKS) {
            return Option.of(ann.warmups());
        }
        return Option.none();
    }

    public Option<String> getJVMArgs() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !ann.jvmArgs().equals(AnnotationUtils.PARAM_NOT_SET)) {
            return Option.of(ann.jvmArgs());
        }
        return Option.none();
    }

    public Option<String> getJVMArgsAppend() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !ann.jvmArgsAppend().equals(AnnotationUtils.PARAM_NOT_SET)) {
            return Option.of(ann.jvmArgsAppend());
        }
        return Option.none();
    }

    public Option<String> getJVMArgsPrepend() {
        Fork ann = getFinal(Fork.class);
        if (ann != null && !ann.jvmArgsPrepend().equals(AnnotationUtils.PARAM_NOT_SET)) {
            return Option.of(ann.jvmArgsPrepend());
        }
        return Option.none();
    }

    private <T extends Annotation> T getFinal(Class<T> klass) {
        T finalAnn = null;
        for (MethodInvocation mi : methods) {
            T ann = AnnUtils.getAnnotationRecursive(mi.element, klass);
            if (ann != null && finalAnn != null) {
                if (!finalAnn.equals(ann)) {
                    throw new GenerationException("Colliding annotations: " + ann + " vs. " + finalAnn, mi.element);
                }
            }
            finalAnn = ann;
        }
        return finalAnn;
    }

}
