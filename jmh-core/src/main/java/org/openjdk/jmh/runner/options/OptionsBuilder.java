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

import java.util.concurrent.TimeUnit;

public interface OptionsBuilder {
    Options end();

    OptionsBuilder addBenchmark(String regexp);

    OptionsBuilder addExclude(String regexp);

    OptionsBuilder outputFormat(OutputFormatType type);

    OptionsBuilder setOutput(String filename);

    OptionsBuilder shouldDoGC(boolean value);

    OptionsBuilder addProfiler(ProfilerType prof);

    OptionsBuilder shouldBeVerbose(boolean value);

    OptionsBuilder shouldFailOnError(boolean value);

    OptionsBuilder shouldOutputDetails(boolean value);

    OptionsBuilder threads(int count);

    OptionsBuilder shouldSyncIterations(boolean value);

    OptionsBuilder warmupIterations(int value);

    OptionsBuilder warmupTime(TimeValue value);

    OptionsBuilder warmupMode(WarmupMode mode);

    OptionsBuilder addWarmupMicro(String regexp);

    OptionsBuilder iterations(int count);

    OptionsBuilder measurementTime(TimeValue value);

    OptionsBuilder addMode(Mode mode);

    OptionsBuilder setTimeUnit(TimeUnit tu);

    OptionsBuilder forks(int value);

    OptionsBuilder warmupForks(int value);

    OptionsBuilder classpath(String value);

    OptionsBuilder jvmBinary(String path);

    OptionsBuilder jvmArgs(String value);

}
