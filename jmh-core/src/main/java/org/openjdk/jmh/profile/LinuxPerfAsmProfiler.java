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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.util.Deduplicator;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.Multiset;
import org.openjdk.jmh.util.TreeMultiset;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LinuxPerfAsmProfiler extends AbstractPerfAsmProfiler {

    /**
     * Sampling frequency
     */
    private static final long SAMPLE_FREQUENCY = Long.getLong("jmh.perfasm.frequency", 1000);

    /**
     * Events to gather
     */
    private static final String[] EVENTS = System.getProperty("jmh.perfasm.events", "cycles,instructions").split(",");

    private static final boolean IS_SUPPORTED;
    private static final boolean IS_DELAYED;
    private static final Collection<String> FAIL_MSGS;

    static {
        FAIL_MSGS = Utils.tryWith("perf", "stat", "--log-fd", "2", "echo", "1");
        IS_SUPPORTED = FAIL_MSGS.isEmpty();

        Collection<String> delay = Utils.tryWith("perf", "stat", "--log-fd", "2", "-D", "1", "echo", "1");
        IS_DELAYED = delay.isEmpty();
    }

    @Override
    public boolean checkSupport(List<String> msgs) {
        if (IS_SUPPORTED) {
            return true;
        } else {
            msgs.addAll(FAIL_MSGS);
            return false;
        }
    }


    public LinuxPerfAsmProfiler() throws IOException {
        super(EVENTS);
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Arrays.asList("perf", "record", "-F" + SAMPLE_FREQUENCY, "-e" + Utils.join(EVENTS, ","), "-o" + perfBinData);
    }

    @Override
    public String label() {
        return "perfasm";
    }

    @Override
    public String getDescription() {
        return "Linux perf + PrintAssembly Profiler";
    }

    @Override
    protected void parseEvents() {
        try {
            Process p = Runtime.getRuntime().exec("perf script -f time,event,ip,sym,dso -i " + perfBinData);

            // drain streams, else we might lock up
            FileOutputStream fos = new FileOutputStream(perfParsedData);

            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fos);

            errDrainer.start();
            outDrainer.start();

            p.waitFor();

            errDrainer.join();
            outDrainer.join();

            FileUtils.safelyClose(fos);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected PerfEvents readEvents(double skipSec) {
        FileReader fr = null;
        try {
            Deduplicator<String> dedup = new Deduplicator<String>();

            fr = new FileReader(perfParsedData);
            BufferedReader reader = new BufferedReader(fr);

            Map<Long, String> methods = new HashMap<Long, String>();
            Map<Long, String> libs = new HashMap<Long, String>();
            Map<String, Multiset<Long>> events = new LinkedHashMap<String, Multiset<Long>>();
            for (String evName : EVENTS) {
                events.put(evName, new TreeMultiset<Long>());
            }

            Double startTime = null;

            // Demangled symbol names can contain spaces, so we need to parse the lines
            // in a complicated manner. Using regexps will not solve this without sacrificing
            // lots of performance, so we need to get tricky, and merge the symbol names back
            // after splitting.
            //
            // We are forcing perf to print: time event ip sym dso
            //

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;

                String[] elems = line.trim().split("[ ]+");

                if (elems.length < 4) continue;

                String strTime = elems[0].replace(":", "");
                String evName = elems[1].replace(":", "");
                String strAddr = elems[2];
                String symbol = Utils.join(Arrays.copyOfRange(elems, 3, elems.length - 1), " ");
                String lib = elems[elems.length - 1];
                lib = lib.substring(lib.lastIndexOf("/") + 1, lib.length()).replace("(", "").replace(")", "");

                try {
                    Double time = Double.valueOf(strTime);
                    if (startTime == null) {
                        startTime = time;
                    } else {
                        if (time - startTime < skipSec) {
                            continue;
                        }
                    }
                } catch (NumberFormatException e) {
                    // misformatted line, no timestamp
                    continue;
                }

                Multiset<Long> evs = events.get(evName);
                if (evs == null) {
                    // we are not prepared to handle this event, skip
                    continue;
                }

                try {
                    Long addr = Long.valueOf(strAddr, 16);
                    evs.add(addr);
                    methods.put(addr, dedup.dedup(symbol));
                    libs.put(addr, dedup.dedup(lib));
                } catch (NumberFormatException e) {
                    // kernel addresses like "ffffffff810c1b00" overflow signed long,
                    // record them as dummy address
                    evs.add(0L);
                }
            }

            methods.put(0L, "<kernel>");

            return new PerfEvents(tracedEvents, events, methods, libs);
        } catch (IOException e) {
            return new PerfEvents(tracedEvents);
        } finally {
            FileUtils.safelyClose(fr);
        }
    }

    @Override
    protected String perfBinaryExtension() {
        return ".perfbin";
    }
}
