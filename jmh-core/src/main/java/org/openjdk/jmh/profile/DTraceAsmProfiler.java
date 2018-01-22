/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Mac OS X perfasm profiler based on DTrace "profile-n" provider which samples program counter by timer interrupt.
 * Due to DTrace limitations on Mac OS X target JVM cannot be run directly under DTrace control, so DTrace is run separately,
 * all processes are sampled and irrelevant samples are filtered out in {@link #readEvents(double, double)} stage.
 * Super user privileges are required in order to run DTrace.
 * <p>
 * If you see a lot of "[unknown]" regions in profile then you are probably hitting kernel code, kernel sampling is not yet supported.
 *
 * @author Tolstopyatov Vsevolod
 * @since 18/10/2017
 */
public class DTraceAsmProfiler extends AbstractPerfAsmProfiler {

    private final long sampleFrequency;
    private volatile String pid;
    private volatile Process dtraceProcess;
    private OptionSpec<Long> optFrequency;

    public DTraceAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "sampled_pc");

        // Check DTrace availability
        Collection<String> messages = Utils.tryWith("sudo", "dtrace", "-V");
        if (!messages.isEmpty()) {
            throw new ProfilerException(messages.toString());
        }

        try {
            sampleFrequency = set.valueOf(optFrequency);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        super.beforeTrial(params);
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        if (pid == 0) {
            throw new IllegalStateException("DTrace needs the forked VM PID, but it is not initialized");
        }

        Collection<String> messages = Utils.destroy(dtraceProcess);
        if (!messages.isEmpty()) {
            throw new IllegalStateException(messages.toString());
        }

        this.pid = String.valueOf(pid);
        return super.afterTrial(br, pid, stdOut, stdErr);
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        dtraceProcess = Utils.runAsync("sudo", "dtrace", "-n", "profile-" + sampleFrequency +
                        " /arg1/ { printf(\"%d 0x%lx %d\", pid, arg1, timestamp); ufunc(arg1)}", "-o",
                perfBinData.getAbsolutePath());
        return Collections.emptyList();
    }

    @Override
    public String getDescription() {
        return "DTrace profile provider + PrintAssembly Profiler";
    }

    @Override
    protected void addMyOptions(OptionParser parser) {
        optFrequency = parser.accepts("frequency",
                "Sampling frequency. This is synonymous to profile-#")
                .withRequiredArg().ofType(Long.class).describedAs("freq").defaultsTo(1001L);
    }

    @Override
    protected void parseEvents() {
        // Do nothing because DTrace writes text output anyway
    }

    @Override
    protected PerfEvents readEvents(double skipMs, double lenMs) {
        long start = (long) skipMs;
        long end = (long) (skipMs + lenMs);

        try (FileReader fr = new FileReader(perfBinData.file());
             BufferedReader reader = new BufferedReader(fr)) {

            Deduplicator<MethodDesc> dedup = new Deduplicator<>();
            Multimap<MethodDesc, Long> methods = new HashMultimap<>();
            Multiset<Long> events = new TreeMultiset<>();

            long dtraceTimestampBase = 0L;
            String line;
            while ((line = reader.readLine()) != null) {

                // Filter out DTrace misc
                if (!line.contains(":profile")) {
                    continue;
                }

                line = line.trim();
                line = line.substring(line.indexOf(":profile"));
                String[] splits = line.split(" ", 5);
                String sampledPid = splits[1];

                if (!sampledPid.equals(pid)) {
                    continue;
                }

                // Sometimes DTrace ufunc fails and gives no information about symbols
                if (splits.length < 4) {
                    continue;
                }

                long timestamp = Long.valueOf(splits[3]);
                if (dtraceTimestampBase == 0) {
                    // Use first event timestamp as base for time comparison
                    dtraceTimestampBase = timestamp;
                    continue;
                }

                long elapsed = timestamp - dtraceTimestampBase;
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsed);

                if (elapsedMs < start || elapsedMs > end) {
                    continue;
                }

                long address = Long.decode(splits[2]);
                events.add(address);

                String methodLine = splits[4];
                // JIT-compiled code has address instead of symbol information
                if (methodLine.startsWith("0x")) {
                    continue;
                }

                String symbol = "[unknown]";
                String[] methodSplit = methodLine.split("`");
                String library = methodSplit[0];
                if ("".equals(library)) {
                    library = "[unknown]";
                }

                if (methodSplit.length == 2) {
                    symbol = methodSplit[1];
                }

                methods.put(dedup.dedup(MethodDesc.nativeMethod(symbol, library)), address);
            }

            IntervalMap<MethodDesc> methodMap = new IntervalMap<>();
            for (MethodDesc md : methods.keys()) {
                Collection<Long> longs = methods.get(md);
                methodMap.add(md, Utils.min(longs), Utils.max(longs));
            }

            Map<String, Multiset<Long>> allEvents = new TreeMap<>();
            assert this.events.size() == 1;
            allEvents.put(this.events.get(0), events);
            return new PerfEvents(this.events, allEvents, methodMap);

        } catch (IOException e) {
            return new PerfEvents(events);
        }

    }

    @Override
    protected String perfBinaryExtension() {
        // DTrace produces human-readable txt
        return ".txt";
    }
}
