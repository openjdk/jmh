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
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.profile.ProfilerType;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DefaultBuilder implements Options, OptionsBuilder {

    public static OptionsBuilder start() {
        return new DefaultBuilder();
    }

    private boolean finalized;

    private void checkFinalized() {
        if (finalized) {
            throw new IllegalStateException("The builder is already finalized");
        }
    }

    @Override
    public Options end() {
        finalized = true;
        return this;
    }

    // ---------------------------------------------------------------------------

    private final List<String> regexps = new ArrayList<String>();

    @Override
    public OptionsBuilder addBenchmark(String regexp) {
        checkFinalized();
        regexps.add(regexp);
        return this;
    }

    @Override
    public List<String> getRegexps() {
        return regexps;
    }

    // ---------------------------------------------------------------------------

    private final List<String> excludes = new ArrayList<String>();

    @Override
    public OptionsBuilder addExclude(String regexp) {
        excludes.add(regexp);
        return this;
    }

    @Override
    public List<String> getExcludes() {
        return excludes;
    }

    // ---------------------------------------------------------------------------

    private OutputFormatType ofType = OutputFormatType.Pretty;

    @Override
    public OptionsBuilder outputFormat(OutputFormatType type) {
        ofType = type;
        return this;
    }

    @Override
    public OutputFormatType getOutputFormat() {
        return ofType;
    }

    // ---------------------------------------------------------------------------

    private String output;

    @Override
    public OptionsBuilder setOutput(String filename) {
        this.output = filename;
        return this;
    }

    @Override
    public String getOutput() {
        return output;
    }

    // ---------------------------------------------------------------------------

    private boolean shouldDoGC;

    @Override
    public OptionsBuilder shouldDoGC(boolean value) {
        shouldDoGC = value;
        return this;
    }

    @Override
    public boolean shouldDoGC() {
        return shouldDoGC;
    }

    // ---------------------------------------------------------------------------

    private EnumSet<ProfilerType> profilers = EnumSet.noneOf(ProfilerType.class);

    @Override
    public OptionsBuilder addProfiler(ProfilerType prof) {
        this.profilers.add(prof);
        return this;
    }

    @Override
    public Set<ProfilerType> getProfilers() {
        return profilers;
    }

    // ---------------------------------------------------------------------------

    private boolean isVerbose;

    @Override
    public OptionsBuilder shouldBeVerbose(boolean value) {
        isVerbose = value;
        return this;
    }

    @Override
    public boolean isVerbose() {
        return isVerbose;
    }

    // ---------------------------------------------------------------------------

    private boolean shouldFailOnError;

    @Override
    public OptionsBuilder shouldFailOnError(boolean value) {
        shouldFailOnError = value;
        return this;
    }

    @Override
    public boolean shouldFailOnError() {
        return shouldFailOnError;
    }

    // ---------------------------------------------------------------------------

    private boolean shouldOutputDetails;

    @Override
    public OptionsBuilder shouldOutputDetails(boolean value) {
        shouldOutputDetails = value;
        return this;
    }

    @Override
    public boolean shouldOutputDetailedResults() {
        return shouldOutputDetails;
    }

    // ---------------------------------------------------------------------------

    private int threads = -1;

    @Override
    public OptionsBuilder threads(int count) {
        this.threads = count;
        return this;
    }

    @Override
    public int getThreads() {
        return threads;
    }

    // ---------------------------------------------------------------------------

    private Boolean syncIterations;

    @Override
    public OptionsBuilder shouldSyncIterations(boolean value) {
        this.syncIterations = value;
        return this;
    }

    @Override
    public Boolean getSynchIterations() {
        return syncIterations;
    }

    // ---------------------------------------------------------------------------

    private int warmupIterations = -1;

    @Override
    public OptionsBuilder warmupIterations(int value) {
        this.warmupIterations = value;
        return this;
    }

    @Override
    public int getWarmupIterations() {
        return warmupIterations;
    }

    // ---------------------------------------------------------------------------

    private TimeValue warmupTime;

    @Override
    public OptionsBuilder warmupTime(TimeValue value) {
        this.warmupTime = value;
        return this;
    }

    @Override
    public TimeValue getWarmupTime() {
        return warmupTime;
    }

    // ---------------------------------------------------------------------------

    private WarmupMode warmupMode = WarmupMode.BEFOREEACH;

    @Override
    public OptionsBuilder warmupMode(WarmupMode mode) {
        this.warmupMode = mode;
        return this;
    }

    @Override
    public WarmupMode getWarmupMode() {
        return warmupMode;
    }

    // ---------------------------------------------------------------------------

    private final List<String> warmupMicros = new ArrayList<String>();

    @Override
    public OptionsBuilder addWarmupMicro(String regexp) {
        warmupMicros.add(regexp);
        return this;
    }

    @Override
    public List<String> getWarmupMicros() {
        return warmupMicros;
    }

    // ---------------------------------------------------------------------------

    private int iterations = -1;

    @Override
    public OptionsBuilder iterations(int count) {
        this.iterations = count;
        return this;
    }

    @Override
    public int getIterations() {
        return iterations;
    }

    // ---------------------------------------------------------------------------

    private TimeValue measurementTime;

    @Override
    public OptionsBuilder measurementTime(TimeValue value) {
        this.measurementTime = value;
        return this;
    }

    @Override
    public TimeValue getRuntime() {
        return measurementTime;
    }

    // ---------------------------------------------------------------------------

    private EnumSet<Mode> benchModes;

    @Override
    public OptionsBuilder addMode(Mode mode) {
        if (benchModes == null) {
            benchModes = EnumSet.noneOf(Mode.class);
        }
        benchModes.add(mode);
        return this;
    }

    @Override
    public Collection<Mode> getBenchModes() {
        return benchModes;
    }

    // ---------------------------------------------------------------------------

    private TimeUnit timeUnit;

    @Override
    public OptionsBuilder setTimeUnit(TimeUnit tu) {
        this.timeUnit = tu;
        return this;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    // ---------------------------------------------------------------------------

    private int forks = -1;

    @Override
    public OptionsBuilder forks(int value) {
        this.forks = value;
        return this;
    }

    @Override
    public int getForkCount() {
        return forks;
    }

    // ---------------------------------------------------------------------------

    private int warmupForks = -1;

    @Override
    public OptionsBuilder warmupForks(int value) {
        this.warmupForks = value;
        return this;
    }

    @Override
    public int getWarmupForkCount() {
        return warmupForks;
    }

    // ---------------------------------------------------------------------------

    private String jvmClassPath;

    @Override
    public OptionsBuilder classpath(String value) {
        this.jvmClassPath = value;
        return this;
    }

    @Override
    public String getJvmClassPath() {
        return jvmClassPath;
    }

    // ---------------------------------------------------------------------------

    private String jvmBinary;

    @Override
    public OptionsBuilder jvmBinary(String path) {
        this.jvmBinary = path;
        return this;
    }

    @Override
    public String getJvm() {
        return jvmBinary;
    }

    // ---------------------------------------------------------------------------

    private String jvmArgs;

    @Override
    public OptionsBuilder jvmArgs(String value) {
        this.jvmArgs = value;
        return this;
    }

    @Override
    public String getJvmArgs() {
        return jvmArgs;
    }

    // ---------------------------------------------------------------------------

    @Override
    public String[] toCommandLine() {
        // FIXME: Need to convert to proper representation
        List<String> results = new ArrayList<String>();

        return results.toArray(new String[results.size()]);
    }

    // ---------------------------------------------------------------------------

    @Override
    public String getHostName() {
        throw new UnsupportedOperationException();
    }

    // ---------------------------------------------------------------------------

    @Override
    public int getHostPort() {
        throw new UnsupportedOperationException();
    }

    // ---------------------------------------------------------------------------

    @Override
    public String getBenchmark() {
        throw new UnsupportedOperationException();
    }
}
