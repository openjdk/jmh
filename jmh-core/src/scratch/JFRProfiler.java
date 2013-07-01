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
package org.openjdk.jmh.profile;

import com.oracle.jrockit.jfr.FlightRecorder;
import com.oracle.jrockit.jfr.client.FlightRecorderClient;
import com.oracle.jrockit.jfr.client.FlightRecordingClient;
import oracle.jrockit.jfr.parser.ChunkParser;
import oracle.jrockit.jfr.parser.FLREvent;
import oracle.jrockit.jfr.parser.Parser;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JFRProfiler implements Profiler {

    /*
     * Implementation note: (Claes Redestad):
     *
     * This profiler does not start the JFR default recording automatically,
     * since this interfere with micros specifically testing JFR with no way
     * to turn the default recording off.
     *
     * One should specifically supply:
     *   -XX:FlightRecorderOptions=defaultrecording=true
     *
     * ...to enable the default recording.
     */

    /** Cap on the number of event types to output per iteration. */
    private static final int MAX_EVENT_LINES = 50;

    static long TICKS_PER_SECOND;

    private final String name;
    private final boolean verbose;
    private FlightRecorderClient client;

    public JFRProfiler(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
    }

    @Override
    public void startProfile() {
        try {
            client = new FlightRecorderClient();
        } catch (InstanceNotFoundException ex) {
            Logger.getLogger(JFRProfiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            Logger.getLogger(JFRProfiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JFRProfiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public ProfilerResult endProfile() {
        try {
            List<FLREvent> recordedEvents = getRecordedEvents();
            JFRProfilerResult result = new JFRProfilerResult(name, verbose, TICKS_PER_SECOND);
            result.setEvents(recordedEvents);
            return result;
        } catch (Exception e) {
            return new EmptyResult();
        }
    }

    public static boolean isSupported() {
        try {
            Class.forName("com.oracle.jrockit.jfr.FlightRecorder");
            return FlightRecorder.isActive();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private List<FLREvent> getRecordedEvents() {
        try {
            List<FLREvent> list = new ArrayList<FLREvent>();
            for (FlightRecordingClient recording : client.getRecordingObjects()) {
                if (recording.isRunning()) {
                    File tempFile = File.createTempFile("parse", ".jfr");

                    ObjectName objectName = recording.cloneRecording("temp", true);
                    client.copyTo(objectName, tempFile.getAbsolutePath());
                    for (ChunkParser parser : new Parser(tempFile, false)) {
                        if (TICKS_PER_SECOND == 0) {
                            TICKS_PER_SECOND = parser.getTickFrequency();
                        } else if (parser.getTickFrequency() != TICKS_PER_SECOND) {
                            Logger.getLogger(JFRProfiler.class.getName()).log(Level.WARNING,
                                    "Tick frequency should not ever change");
                        }
                        for (FLREvent event : parser) {
                            list.add(event);
                        }
                    }
                    client.close(objectName);
                }
            }
            return list;
        } catch (Exception e) {
            throw new IllegalStateException("Could not retrieve events", e);
        }
    }

    private static class JFREntry implements Serializable {
        long totalDuration;
        int count;
    }

    static class JFRProfilerResult implements ProfilerResult {
        private final long ticksPerSecond;
        private final String name;
        private final boolean verbose;
        private final Map<String, JFREntry> eventHisto;

        public JFRProfilerResult(String name, boolean verbose, long ticksPerSecond) {
            this.name = name;
            this.verbose = verbose;
            this.ticksPerSecond = ticksPerSecond;
            this.eventHisto = new HashMap<String, JFREntry>();
        }

        @Override
        public String getProfilerName() {
            return name;
        }

        @Override
        public boolean hasData() {
            return true;
        }

        @Override
        public String toString() {
            boolean headerPrinted = false;
            int count = 0;
            long totalEvents = 0;
            for (JFREntry entry : eventHisto.values()) {
                totalEvents += entry.count;
            }
            StringBuilder sb = new StringBuilder(String.format("# events: %d", totalEvents));
            for (Map.Entry<String, JFREntry> entry : eventHisto.entrySet()) {
                if (entry.getValue().totalDuration == 0 && !verbose) continue;
                if (!headerPrinted) {
                    headerPrinted = true;
                    sb.append("\n\t    #\tDuration (ms)\tEvent");
                }
                if (count++ < MAX_EVENT_LINES) {
                    String duration =
                            (entry.getValue().totalDuration == 0) ?
                                "          N/A" :
                                String.format("%13.2f", (double) (entry.getValue().totalDuration * 1000.0 / (double) ticksPerSecond));
                    sb.append("\n\t")
                            .append(String.format("%5d", entry.getValue().count))
                            .append("\t")
                            .append(duration)
                            .append("\t").append(entry.getKey());
                }
            }
            if (count > MAX_EVENT_LINES) {
                sb.append("\n\t... skipping ").append(count - MAX_EVENT_LINES).append(" event types");
            }
            return sb.toString();
        }

        private void setEvents(List<FLREvent> recordedEvents) {
            for (FLREvent event : recordedEvents) {
                long duration = getDuration(event);
                JFREntry entry = eventHisto.get(event.getPath());
                if (entry == null) {
                    entry = new JFREntry();
                }
                entry.count++;
                entry.totalDuration += duration;
                eventHisto.put(event.getPath(), entry);
            }
        }

        private long getDuration(FLREvent event) {
            try {
                if (event.hasStartTime()) {
                    return event.getTimestamp() - event.getStartTime();
                } else {
                    return 0;
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Couldn't access fields", ex);
            }
        }
    }
}
