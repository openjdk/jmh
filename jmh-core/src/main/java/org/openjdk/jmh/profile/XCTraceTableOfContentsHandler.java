/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.xml.sax.Attributes;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts information about xctrace result tables from trace file's table of contents.
 *
 * The most interesting part of the ToC is a set of tables containing information about table's format and
 * various recoding parameters, such as names of collected PMCs.
 */
class XCTraceTableOfContentsHandler extends XCTraceTableHandler {
    private static final DateTimeFormatter TOC_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final List<XCTraceTableDesc> supportedTables = new ArrayList<>();

    private long recordStartMs;

    private static List<String> parseEvents(Attributes attributes) {
        String events = attributes.getValue(XCTraceTableHandler.PMC_EVENTS);
        return Arrays.stream(events.split(" ")).map(e -> {
                    if (!e.startsWith("\"") && !e.endsWith("\"")) return e;
                    if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length() - 1);
                    throw new IllegalStateException("Can't parse pmc-events: " + events);
                }).filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }

    public List<XCTraceTableDesc> getSupportedTables() {
        return Collections.unmodifiableList(supportedTables);
    }

    public long getRecordStartMs() {
        return recordStartMs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        setNeedParseCharacters(qName.equals(XCTraceTableHandler.START_DATE));
        if (!qName.equals(XCTraceTableHandler.TABLE)) {
            return;
        }

        String schema = Objects.requireNonNull(attributes.getValue(XCTraceTableHandler.SCHEMA), "Schema not found");
        if (schema.equals(ProfilingTableType.CPU_PROFILE.tableName)) {
            supportedTables.add(XCTraceTableDesc.CPU_PROFILE);
        } else if (schema.equals(ProfilingTableType.TIME_PROFILE.tableName)) {
            supportedTables.add(XCTraceTableDesc.TIME_PROFILE);
        } else if (schema.equals(ProfilingTableType.COUNTERS_PROFILE.tableName)) {
            parseCountersProfile(attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!qName.equals(XCTraceTableHandler.START_DATE)) {
            return;
        }
        try {
            recordStartMs = Instant.from(TOC_DATE_FORMAT.parse(getCharacters())).toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(e);
        } finally {
            setNeedParseCharacters(false);
        }
    }

    private void parseCountersProfile(Attributes attributes) {
        String trigger = Objects.requireNonNull(attributes.getValue(XCTraceTableHandler.TRIGGER));
        TriggerType triggerType = TriggerType.valueOf(trigger.toUpperCase());

        if (triggerType == TriggerType.PMI) {
            parsePmiSampleTable(attributes);
        } else if (triggerType == TriggerType.TIME) {
            parseTimeSampleTable(attributes);
        } else {
            throw new IllegalStateException("Unsupported trigger type: " + triggerType);
        }
    }

    private void parsePmiSampleTable(Attributes attributes) {
        String pmiEvent = Objects.requireNonNull(attributes.getValue(XCTraceTableHandler.PMI_EVENT),
                "Trigger event not found");
        if (pmiEvent.startsWith("\"") && pmiEvent.endsWith("\"")) {
            pmiEvent = pmiEvent.substring(1, pmiEvent.length() - 1);
        }
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue(XCTraceTableHandler.PMI_THRESHOLD),
                "Trigger threshold not found"));
        XCTraceTableDesc table = new XCTraceTableDesc(ProfilingTableType.COUNTERS_PROFILE, TriggerType.PMI,
                pmiEvent, threshold, parseEvents(attributes));
        supportedTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue(XCTraceTableHandler.SAMPLE_RATE),
                "Trigger threshold not found"));
        XCTraceTableDesc table = new XCTraceTableDesc(ProfilingTableType.COUNTERS_PROFILE, TriggerType.TIME,
                XCTraceTableProfileHandler.XCTraceSample.TIME_SAMPLE_TRIGGER_NAME, threshold, parseEvents(attributes));
        supportedTables.add(table);
    }
}
