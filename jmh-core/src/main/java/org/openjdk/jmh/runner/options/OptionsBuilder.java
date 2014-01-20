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
import org.openjdk.jmh.util.internal.Optional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OptionsBuilder implements Options, ChainedOptionsBuilder {

    private boolean finalized;

    private void checkFinalized() {
        if (finalized) {
            throw new IllegalStateException("The builder is already finalized");
        }
    }

    @Override
    public Options build() {
        finalized = true;
        return this;
    }

    // ---------------------------------------------------------------------------

    private final List<String> regexps = new ArrayList<String>();

    @Override
    public ChainedOptionsBuilder include(String regexp) {
        checkFinalized();
        regexps.add(regexp);
        return this;
    }

    @Override
    public List<String> getIncludes() {
        if (regexps.isEmpty()) {
            return Collections.singletonList(".*");
        }
        return regexps;
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
        return excludes;
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
        return output;
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
        return rfType;
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
        return result;
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
        return shouldDoGC;
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
        return verbosity;
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
        return shouldFailOnError;
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
        return threads;
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
        return threadGroups;
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
        return syncIterations;
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
        return warmupIterations;
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
        return warmupTime;
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
        return warmupMode;
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
        return warmupMicros;
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
        return iterations;
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
        return measurementTime;
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
        return benchModes;
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
        return timeUnit;
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
        return forks;
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
        return warmupForks;
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
        return jvmBinary;
    }

    // ---------------------------------------------------------------------------

    private Optional<Collection<String>> jvmArgs = Optional.none();

    @Override
    public ChainedOptionsBuilder jvmArgs(String... value) {
        this.jvmArgs = Optional.<Collection<String>>of(Arrays.asList(value));
        return this;
    }

    @Override
    public Optional<Collection<String>> getJvmArgs() {
        return jvmArgs;
    }

}
