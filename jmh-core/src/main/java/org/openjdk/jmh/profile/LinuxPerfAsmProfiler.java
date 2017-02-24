/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.util.*;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class LinuxPerfAsmProfiler extends AbstractPerfAsmProfiler {

    private final long sampleFrequency;

    private OptionSpec<Long> optFrequency;

    public LinuxPerfAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "cycles", "instructions");

        Collection<String> failMsg = Utils.tryWith(PerfSupport.PERF_EXEC, "stat", "--log-fd", "2", "echo", "1");
        if (!failMsg.isEmpty()) {
            throw new ProfilerException(failMsg.toString());
        }

        try {
            sampleFrequency = set.valueOf(optFrequency);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    protected void addMyOptions(OptionParser parser) {
        optFrequency = parser.accepts("frequency",
                "Sampling frequency. This is synonymous to perf record --freq #")
                .withRequiredArg().ofType(Long.class).describedAs("freq").defaultsTo(1000L);
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Arrays.asList(PerfSupport.PERF_EXEC, "record", "--freq", String.valueOf(sampleFrequency), "--event", Utils.join(events, ","), "--output", perfBinData.getAbsolutePath());
    }

    @Override
    public String getDescription() {
        return "Linux perf + PrintAssembly Profiler";
    }

    @Override
    protected void parseEvents() {
        try (FileOutputStream fos = new FileOutputStream(perfParsedData.file())) {
            ProcessBuilder pb = new ProcessBuilder(PerfSupport.PERF_EXEC, "script", "--fields", "time,event,ip,sym,dso", "--input", perfBinData.getAbsolutePath());
            Process p = pb.start();

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fos);

            errDrainer.start();
            outDrainer.start();

            p.waitFor();

            errDrainer.join();
            outDrainer.join();
        } catch (IOException | InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static PerfLine parsePerfLine(String line) {
        if (line.startsWith("#")) {
            return null;
        }

        // Demangled symbol names can contain spaces, so we need to parse the lines
        // in a complicated manner. Using regexps will not solve this without sacrificing
        // lots of performance, so we need to get tricky.
        //
        // We are forcing perf to print: time event ip sym dso
        //
        //  328992.235251: instructions:      7fa85da61a09 match_symbol (/lib/x86_64-linux-gnu/ld-2.23.so)
        //

        // Remove excess spaces
        int lastLength = -1;
        while (line.length() != lastLength) {
            lastLength = line.length();
            line = line.replace("  ", " ");
        }

        // Chomp the time
        int timeIdx = line.indexOf(": ");
        if (timeIdx == -1) return null;
        String strTime = line.substring(0, timeIdx);
        line = line.substring(timeIdx + 2);

        double time;
        try {
            time = Double.valueOf(strTime);
        } catch (NumberFormatException e) {
            return null;
        }

        // Chomp the library, handling spaces properly:
        int libIdx = line.lastIndexOf(" (");
        if (libIdx == -1) return null;
        String lib = line.substring(libIdx);
        lib = lib.substring(lib.lastIndexOf("/") + 1, lib.length()).replace("(", "").replace(")", "");
        line = line.substring(0, libIdx);

        // Chomp the event name:
        int evIdx = line.indexOf(": ");
        if (evIdx == -1) return null;
        String evName = line.substring(0, evIdx);
        int tagIdx = evName.lastIndexOf(":");
        if (tagIdx != -1) {
            evName = evName.substring(0, tagIdx);
        }
        line = line.substring(evIdx + 2);

        // Chomp the addr:
        int addrIdx = line.indexOf(" ");
        if (addrIdx == -1) return null;
        String strAddr = line.substring(0, addrIdx);
        line = line.substring(addrIdx + 1);

        // Try to parse the positive address lightly first.
        // If that fails, try to parse the negative address.
        // If that fails as well, then address is unresolvable.
        long addr;
        try {
            addr = Long.valueOf(strAddr, 16);
        } catch (NumberFormatException e) {
            try {
                addr = new BigInteger(strAddr, 16).longValue();
                if (addr < 0L && lib.contains("unknown")) {
                    lib = "kernel";
                }
            } catch (NumberFormatException e1) {
                addr = 0L;
            }
        }

        // Whatever is left is symbol:
        String symbol = line;

        return new PerfLine(time, evName, addr, symbol, lib);
    }

    static class PerfLine {
        final double time;
        final String event;
        final long addr;
        final String symbol;
        final String lib;

        public PerfLine(double time, String event, long addr, String symbol, String lib) {
            this.time = time;
            this.event = event;
            this.addr = addr;
            this.symbol = symbol;
            this.lib = lib;
        }

        public double time() {
            return time;
        }

        public String eventName() {
            return event;
        }

        public long addr() {
            return addr;
        }

        public String symbol() {
            return symbol;
        }

        public String lib() {
            return lib;
        }
    }

    @Override
    protected PerfEvents readEvents(double skipMs, double lenMs) {
        double readFrom = skipMs / 1000D;
        double readTo = (skipMs + lenMs) / 1000D;

        try (FileReader fr = new FileReader(perfParsedData.file());
             BufferedReader reader = new BufferedReader(fr)) {
            Deduplicator<MethodDesc> dedup = new Deduplicator<>();

            Multimap<MethodDesc, Long> methods = new HashMultimap<>();
            Map<String, Multiset<Long>> events = new LinkedHashMap<>();
            for (String evName : this.events) {
                events.put(evName, new TreeMultiset<Long>());
            }

            Double startTime = null;

            String line;
            while ((line = reader.readLine()) != null) {
                PerfLine perfline = parsePerfLine(line);
                if (perfline == null) {
                    continue;
                }

                if (startTime == null) {
                    startTime = perfline.time();
                } else {
                    if (perfline.time() - startTime < readFrom) {
                        continue;
                    }
                    if (perfline.time() - startTime > readTo) {
                        continue;
                    }
                }

                Multiset<Long> evs = events.get(perfline.eventName());
                if (evs == null) {
                    // we are not prepared to handle this event, skip
                    continue;
                }

                evs.add(perfline.addr());
                MethodDesc desc = dedup.dedup(MethodDesc.nativeMethod(perfline.symbol(), perfline.lib()));
                methods.put(desc, perfline.addr());
            }

            IntervalMap<MethodDesc> methodMap = new IntervalMap<>();
            for (MethodDesc md : methods.keys()) {
                Collection<Long> addrs = methods.get(md);
                methodMap.add(md, Utils.min(addrs), Utils.max(addrs));
            }

            return new PerfEvents(this.events, events, methodMap);
        } catch (IOException e) {
            return new PerfEvents(events);
        }
    }


    @Override
    protected String perfBinaryExtension() {
        return ".perfbin";
    }
}
