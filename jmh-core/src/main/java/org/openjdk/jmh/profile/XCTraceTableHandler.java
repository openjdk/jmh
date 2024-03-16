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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Base class for SAX handlers responsible for xctrace exported table parsing.
 */
class XCTraceTableHandler extends DefaultHandler {
    static final String SAMPLE = "row";
    static final String CYCLE_WEIGHT = "cycle-weight";
    static final String WEIGHT = "weight";
    static final String PMC_EVENT = "pmc-event";
    static final String PMI_EVENT = "pmi-event";
    static final String PMI_THRESHOLD = "pmi-threshold";
    static final String SAMPLE_RATE = "sample-rate-micro-seconds";
    static final String FRAME = "frame";
    static final String BACKTRACE = "backtrace";
    static final String BINARY = "binary";
    static final String SAMPLE_TIME = "sample-time";
    static final String PMC_EVENTS = "pmc-events";
    static final String ADDRESS = "addr";
    static final String SCHEMA = "schema";
    static final String TABLE = "table";
    static final String START_DATE = "start-date";
    static final String NAME = "name";
    static final String NODE = "node";
    static final String REF = "ref";
    static final String ID = "id";
    static final String TRIGGER = "trigger";

    private final StringBuilder builder = new StringBuilder();
    private boolean isNeedToParseCharacters = false;

    @Override
    public void characters(char[] ch, int start, int length) {
        if (isNeedToParseCharacters) {
            builder.append(ch, start, length);
        }
    }

    protected final String getCharacters() {
        String str = builder.toString();
        builder.setLength(0);
        return str;
    }

    protected final void setNeedParseCharacters(boolean need) {
        isNeedToParseCharacters = need;
    }

    protected final boolean isNeedToParseCharacters() {
        return isNeedToParseCharacters;
    }

    public final void parse(File file) {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file, this);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Type of supported profiling results tables.
     */
    enum ProfilingTableType {
        TIME_PROFILE("time-profile"),
        CPU_PROFILE("cpu-profile"),
        COUNTERS_PROFILE("counters-profile");

        ProfilingTableType(String name) {
            tableName = name;
        }

        public final String tableName;
    }

    /**
     * Type on an event triggering sampling
     */
    enum TriggerType {
        TIME,
        PMI,
        UNKNOWN
    }

    static final class XCTraceTableDesc {
        public static final XCTraceTableDesc CPU_PROFILE = new XCTraceTableDesc(ProfilingTableType.CPU_PROFILE);
        public static final XCTraceTableDesc TIME_PROFILE = new XCTraceTableDesc(ProfilingTableType.TIME_PROFILE);

        private final ProfilingTableType tableType;
        private final XCTraceTableHandler.TriggerType triggerType;
        private final List<String> counters;
        private final String trigger;
        private final long threshold;

        XCTraceTableDesc(ProfilingTableType tableType, XCTraceTableHandler.TriggerType triggerType,
                         List<String> counters, String trigger, long threshold) {
            this.tableType = tableType;
            this.triggerType = triggerType;
            this.counters = counters;
            this.trigger = trigger;
            this.threshold = threshold;
        }

        XCTraceTableDesc(ProfilingTableType tableType) {
            this(tableType, XCTraceTableHandler.TriggerType.UNKNOWN, Collections.emptyList(), "", -1);
        }

        public ProfilingTableType getTableType() {
            return tableType;
        }

        public XCTraceTableHandler.TriggerType getTriggerType() {
            return triggerType;
        }

        public List<String> counters() {
            return counters;
        }

        public String triggerEvent() {
            return trigger;
        }

        public long triggerThreshold() {
            return threshold;
        }
    }
}
