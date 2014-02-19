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
package org.openjdk.jmh.runner.options;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.Optional;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OptionsBuilder implements Options, ChainedOptionsBuilder {

    @Override
    public Options build() {
        return this;
    }

    // ---------------------------------------------------------------------------

    private Options otherOptions;

    @Override
    public ChainedOptionsBuilder parent(Options other) {
        this.otherOptions = other;
        return this;
    }

    // ---------------------------------------------------------------------------

    private final List<String> regexps = new ArrayList<String>();

    @Override
    public ChainedOptionsBuilder include(String regexp) {
        regexps.add(regexp);
        return this;
    }

    @Override
    public List<String> getIncludes() {
        List<String> result = new ArrayList<String>();

        result.addAll(regexps);
        if (otherOptions != null) {
            result.addAll(otherOptions.getIncludes());
        }

        if (result.isEmpty()) {
            return Collections.singletonList(".*");
        } else {
            return result;
        }
    }

    // ---------------------------------------------------------------------------

    private final List<String> excludes = new ArrayList<String>();

    @Override
    public ChainedOptionsBuilder exclude(String regexp) {
        excludes.add(regexp);
        return this;
    }

    @Override
    public List<String> getExcludes() {
        List<String> result = new ArrayList<String>();

        result.addAll(excludes);
        if (otherOptions != null) {
            result.addAll(otherOptions.getExcludes());
        }

        return result;
    }

    // ---------------------------------------------------------------------------

    private Optional<String> output = Optional.none();

    @Override
    public ChainedOptionsBuilder output(String filename) {
        this.output = Optional.of(filename);
        return this;
    }

    @Override
    public Optional<String> getOutput() {
        if (otherOptions != null) {
            return output.orAnother(otherOptions.getOutput());
        } else {
            return output;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<ResultFormatType> rfType = Optional.none();

    @Override
    public ChainedOptionsBuilder resultFormat(ResultFormatType type) {
        rfType = Optional.of(type);
        return this;
    }

    @Override
    public Optional<ResultFormatType> getResultFormat() {
        if (otherOptions != null) {
            return rfType.orAnother(otherOptions.getResultFormat());
        } else {
            return rfType;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<String> result = Optional.none();

    @Override
    public ChainedOptionsBuilder result(String filename) {
        this.result = Optional.of(filename);
        return this;
    }

    @Override
    public Optional<String> getResult() {
        if (otherOptions != null) {
            return result.orAnother(otherOptions.getResult());
        } else {
            return result;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Boolean> shouldDoGC = Optional.none();

    @Override
    public ChainedOptionsBuilder shouldDoGC(boolean value) {
        shouldDoGC = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Boolean> shouldDoGC() {
        if (otherOptions != null) {
            return shouldDoGC.orAnother(otherOptions.shouldDoGC());
        } else {
            return shouldDoGC;
        }
    }

    // ---------------------------------------------------------------------------

    private EnumSet<ProfilerType> profilers = EnumSet.noneOf(ProfilerType.class);

    @Override
    public ChainedOptionsBuilder addProfiler(ProfilerType prof) {
        this.profilers.add(prof);
        return this;
    }

    @Override
    public Set<ProfilerType> getProfilers() {
        if (otherOptions != null) {
            profilers.addAll(otherOptions.getProfilers());
        }
        return profilers;
    }

    // ---------------------------------------------------------------------------

    private Optional<VerboseMode> verbosity = Optional.none();

    @Override
    public ChainedOptionsBuilder verbosity(VerboseMode mode) {
        verbosity = Optional.of(mode);
        return this;
    }

    @Override
    public Optional<VerboseMode> verbosity() {
        if (otherOptions != null) {
            return verbosity.orAnother(otherOptions.verbosity());
        } else {
            return verbosity;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Boolean> shouldFailOnError = Optional.none();

    @Override
    public ChainedOptionsBuilder shouldFailOnError(boolean value) {
        shouldFailOnError = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Boolean> shouldFailOnError() {
        if (otherOptions != null) {
            return shouldFailOnError.orAnother(otherOptions.shouldFailOnError());
        } else {
            return shouldFailOnError;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> threads = Optional.none();

    @Override
    public ChainedOptionsBuilder threads(int count) {
        this.threads = Optional.of(count);
        return this;
    }

    @Override
    public Optional<Integer> getThreads() {
        if (otherOptions != null) {
            return threads.orAnother(otherOptions.getThreads());
        } else {
            return threads;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<int[]> threadGroups = Optional.none();

    @Override
    public ChainedOptionsBuilder threadGroups(int... groups) {
        this.threadGroups = Optional.of(groups);
        return this;
    }

    @Override
    public Optional<int[]> getThreadGroups() {
        if (otherOptions != null) {
            return threadGroups.orAnother(otherOptions.getThreadGroups());
        } else {
            return threadGroups;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Boolean> syncIterations = Optional.none();

    @Override
    public ChainedOptionsBuilder syncIterations(boolean value) {
        this.syncIterations = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Boolean> shouldSyncIterations() {
        if (otherOptions != null) {
            return syncIterations.orAnother(otherOptions.shouldSyncIterations());
        } else {
            return syncIterations;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> warmupIterations = Optional.none();

    @Override
    public ChainedOptionsBuilder warmupIterations(int value) {
        this.warmupIterations = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Integer> getWarmupIterations() {
        if (otherOptions != null) {
            return warmupIterations.orAnother(otherOptions.getWarmupIterations());
        } else {
            return warmupIterations;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> warmupBatchSize = Optional.none();

    @Override
    public ChainedOptionsBuilder warmupBatchSize(int value) {
        this.warmupBatchSize = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Integer> getWarmupBatchSize() {
        if (otherOptions != null) {
            return warmupBatchSize.orAnother(otherOptions.getWarmupBatchSize());
        } else {
            return warmupBatchSize;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<TimeValue> warmupTime = Optional.none();

    @Override
    public ChainedOptionsBuilder warmupTime(TimeValue value) {
        this.warmupTime = Optional.of(value);
        return this;
    }

    @Override
    public Optional<TimeValue> getWarmupTime() {
        if (otherOptions != null) {
            return warmupTime.orAnother(otherOptions.getWarmupTime());
        } else {
            return warmupTime;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<WarmupMode> warmupMode = Optional.none();

    @Override
    public ChainedOptionsBuilder warmupMode(WarmupMode mode) {
        this.warmupMode = Optional.of(mode);
        return this;
    }

    @Override
    public Optional<WarmupMode> getWarmupMode() {
        if (otherOptions != null) {
            return warmupMode.orAnother(otherOptions.getWarmupMode());
        } else {
            return warmupMode;
        }
    }

    // ---------------------------------------------------------------------------

    private final List<String> warmupMicros = new ArrayList<String>();

    @Override
    public ChainedOptionsBuilder includeWarmup(String regexp) {
        warmupMicros.add(regexp);
        return this;
    }

    @Override
    public List<String> getWarmupIncludes() {
        List<String> result = new ArrayList<String>();
        result.addAll(warmupMicros);
        if (otherOptions != null) {
            result.addAll(otherOptions.getWarmupIncludes());
        }
        return result;
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> iterations = Optional.none();

    @Override
    public ChainedOptionsBuilder measurementIterations(int count) {
        this.iterations = Optional.of(count);
        return this;
    }

    @Override
    public Optional<Integer> getMeasurementIterations() {
        if (otherOptions != null) {
            return iterations.orAnother(otherOptions.getMeasurementIterations());
        } else {
            return iterations;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<TimeValue> measurementTime = Optional.none();

    @Override
    public ChainedOptionsBuilder measurementTime(TimeValue value) {
        this.measurementTime = Optional.of(value);
        return this;
    }

    @Override
    public Optional<TimeValue> getMeasurementTime() {
        if (otherOptions != null) {
            return measurementTime.orAnother(otherOptions.getMeasurementTime());
        } else {
            return measurementTime;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> measurementBatchSize = Optional.none();

    @Override
    public ChainedOptionsBuilder measurementBatchSize(int value) {
        this.measurementBatchSize = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Integer> getMeasurementBatchSize() {
        if (otherOptions != null) {
            return measurementBatchSize.orAnother(otherOptions.getMeasurementBatchSize());
        } else {
            return measurementBatchSize;
        }
    }


    // ---------------------------------------------------------------------------

    private EnumSet<Mode> benchModes = EnumSet.noneOf(Mode.class);

    @Override
    public ChainedOptionsBuilder mode(Mode mode) {
        benchModes.add(mode);
        return this;
    }

    @Override
    public Collection<Mode> getBenchModes() {
        if (otherOptions != null && benchModes.isEmpty()) {
            return otherOptions.getBenchModes();
        } else {
            return benchModes;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<TimeUnit> timeUnit = Optional.none();

    @Override
    public ChainedOptionsBuilder timeUnit(TimeUnit tu) {
        this.timeUnit = Optional.of(tu);
        return this;
    }

    @Override
    public Optional<TimeUnit> getTimeUnit() {
        if (otherOptions != null) {
            return timeUnit.orAnother(otherOptions.getTimeUnit());
        } else {
            return timeUnit;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> forks = Optional.none();

    @Override
    public ChainedOptionsBuilder forks(int value) {
        this.forks = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Integer> getForkCount() {
        if (otherOptions != null) {
            return forks.orAnother(otherOptions.getForkCount());
        } else {
            return forks;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Integer> warmupForks = Optional.none();

    @Override
    public ChainedOptionsBuilder warmupForks(int value) {
        this.warmupForks = Optional.of(value);
        return this;
    }

    @Override
    public Optional<Integer> getWarmupForkCount() {
        if (otherOptions != null) {
            return warmupForks.orAnother(otherOptions.getWarmupForkCount());
        } else {
            return warmupForks;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<String> jvmBinary = Optional.none();

    @Override
    public ChainedOptionsBuilder jvm(String path) {
        this.jvmBinary = Optional.of(path);
        return this;
    }

    @Override
    public Optional<String> getJvm() {
        if (otherOptions != null) {
            return jvmBinary.orAnother(otherOptions.getJvm());
        } else {
            return jvmBinary;
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Collection<String>> jvmArgs = Optional.none();

    @Override
    public ChainedOptionsBuilder jvmArgs(String... value) {
        if (jvmArgs.hasValue()) {
            jvmArgs.get().addAll(Arrays.asList(value));
        } else {
            Collection<String> vals = new ArrayList<String>(Arrays.asList(value));
            jvmArgs = Optional.of(vals);
        }
        return this;
    }

    @Override
    public Optional<Collection<String>> getJvmArgs() {
        Collection<String> result = new ArrayList<String>();

        if (otherOptions != null) {
            result.addAll(otherOptions.getJvmArgs().orElse(Collections.<String>emptyList()));
        }
        result.addAll(jvmArgs.orElse(Collections.<String>emptyList()));

        if (result.isEmpty()) {
            return Optional.none();
        } else {
            return Optional.of(result);
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Collection<String>> jvmArgsAppend = Optional.none();

    @Override
    public ChainedOptionsBuilder jvmArgsAppend(String... value) {
        if (jvmArgsAppend.hasValue()) {
            jvmArgsAppend.get().addAll(Arrays.asList(value));
        } else {
            Collection<String> vals = new ArrayList<String>(Arrays.asList(value));
            jvmArgsAppend = Optional.of(vals);
        }
        return this;
    }

    @Override
    public Optional<Collection<String>> getJvmArgsAppend() {
        Collection<String> result = new ArrayList<String>();

        if (otherOptions != null) {
            result.addAll(otherOptions.getJvmArgsAppend().orElse(Collections.<String>emptyList()));
        }
        result.addAll(jvmArgsAppend.orElse(Collections.<String>emptyList()));

        if (result.isEmpty()) {
            return Optional.none();
        } else {
            return Optional.of(result);
        }
    }

    // ---------------------------------------------------------------------------

    private Optional<Collection<String>> jvmArgsPrepend = Optional.none();

    @Override
    public ChainedOptionsBuilder jvmArgsPrepend(String... value) {
        if (jvmArgsPrepend.hasValue()) {
            jvmArgsPrepend.get().addAll(Arrays.asList(value));
        } else {
            Collection<String> vals = new ArrayList<String>(Arrays.asList(value));
            jvmArgsPrepend = Optional.of(vals);
        }
        return this;
    }

    @Override
    public Optional<Collection<String>> getJvmArgsPrepend() {
        Collection<String> result = new ArrayList<String>();

        if (otherOptions != null) {
            result.addAll(otherOptions.getJvmArgsPrepend().orElse(Collections.<String>emptyList()));
        }
        result.addAll(jvmArgsPrepend.orElse(Collections.<String>emptyList()));

        if (result.isEmpty()) {
            return Optional.none();
        } else {
            return Optional.of(result);
        }
    }

    // ---------------------------------------------------------------------------

    @Override
    public ChainedOptionsBuilder detectJvmArgs() {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        return jvmArgs(inputArguments.toArray(new String[inputArguments.size()]));
    }

    // ---------------------------------------------------------------------------

    private Multimap<String, String> params = new HashMultimap<String, String>();

    @Override
    public Optional<Collection<String>> getParameter(String name) {
        Collection<String> list = params.get(name);
        if (list == null || list.isEmpty()){
            if (otherOptions != null) {
                return otherOptions.getParameter(name);
            } else {
                return Optional.none();
            }
        } else {
            return Optional.of(list);
        }
    }

    @Override
    public ChainedOptionsBuilder param(String name, String... values) {
        params.putAll(name, Arrays.asList(values));
        return this;
    }
}
