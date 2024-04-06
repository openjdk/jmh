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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Parses xctrace profiling results tables and invokes a callback on parsed samples.
 * <p/>
 * All supported sampling tables ({@link XCTraceTableHandler.ProfilingTableType})
 * share almost identical (for our purposes) schema where only an element with sample weight differs.
 * <p/>
 * Here's an example with some unused elements being omitted:
 * <pre>
 * <row>
 *    <sample-time id="1" fmt="00:00.464.248">464248740</sample-time>
 *    ...
 *    <cycle-weight id="9" fmt="322.13 Kc">322133</cycle-weight>
 *    <backtrace id="10">
 *       <frame id="11" name="dyld4::PrebuiltLoader::isValid(dyld4::RuntimeState const&amp;) const" addr="0x7ff805e6c784">
 *          <binary id="12" name="dyld" UUID="28FD2071-57F3-3873-87BF-E4F674A82DE6" arch="x86_64" load-addr="0x7ff805e48000" path="/usr/lib/dyld" />
 *       </frame>
 *       ...
 *    </backtrace>
 * </row>
 * </pre>
 * Depending on a table type, there might be "weight" or "pmc-event" elements instead if cycle-weight.
 * <p/>
 * The format deduplicates identical elements by referencing them (an attribute "ref" matching
 * a corresponding "id" attribute of the original element) instead of placing a copy.
 */
final class XCTraceTableProfileHandler extends XCTraceTableHandler {
    private final ProfilingTableType tableType;

    // Cache of previously parsed elements to use in place of ref-elements.
    private final Map<Long, TraceElement> entriesCache = new HashMap<>();

    // Stack of xml elements currently being parsed.
    // A new value pushed on an element start and popped on an element end.
    private final List<TraceElement> entriesStack = new ArrayList<>();

    private final Consumer<XCTraceSample> callback;

    private XCTraceSample currentSample = null;

    /**
     * Constructs the handler.
     *
     * @param tableType The type of the table that needs to be pared (used for validation only).
     * @param onSample A callback that will be invoked on a sample once it parsed.
     */
    public XCTraceTableProfileHandler(ProfilingTableType tableType, Consumer<XCTraceSample> onSample) {
        this.tableType = tableType;
        callback = onSample;
    }

    private static long parseId(Attributes attributes) {
        return Long.parseLong(attributes.getValue(XCTraceTableHandler.ID));
    }

    private static long parseRef(Attributes attributes) {
        return Long.parseLong(attributes.getValue(XCTraceTableHandler.REF));
    }

    private static long parseAddress(Attributes attributes) {
        String val = attributes.getValue(XCTraceTableHandler.ADDRESS);
        if (!val.startsWith("0x")) throw new IllegalStateException("Unexpected address format: " + val);
        try {
            return Long.parseUnsignedLong(val.substring(2), 16);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + val, e);
        }
    }

    private static String parseName(Attributes attributes) {
        return attributes.getValue(XCTraceTableHandler.NAME);
    }

    private static boolean hasRef(Attributes attributes) {
        return attributes.getValue(XCTraceTableHandler.REF) != null;
    }

    private <T extends TraceElement> void cache(T e) {
        TraceElement old = entriesCache.put(e.id, e);
        if (old != null) {
            throw new IllegalStateException("Duplicate entry for key " + e.id + ". New value: "
                    + e + ", old value: " + old);
        }
    }

    private <T extends TraceElement> T get(long id) {
        TraceElement value = entriesCache.get(id);
        if (value == null) {
            throw new IllegalStateException("Entry not found in cache for id " + id);
        }
        @SuppressWarnings("unchecked")
        T res = (T) value;
        return res;
    }

    private <T extends TraceElement> void pushCachedOrNew(Attributes attributes, Function<Long, T> factory) {
        if (!hasRef(attributes)) {
            T value = factory.apply(parseId(attributes));
            cache(value);
            entriesStack.add(value);
            return;
        }
        entriesStack.add(get(parseRef(attributes)));
    }

    private <T extends TraceElement> T pop() {
        @SuppressWarnings("unchecked")
        T res = (T) entriesStack.remove(entriesStack.size() - 1);
        return res;
    }

    private <T extends TraceElement> T peek() {
        @SuppressWarnings("unchecked")
        T res = (T) entriesStack.get(entriesStack.size() - 1);
        return res;
    }

    private LongHolder popAndUpdateLongHolder() {
        LongHolder value = pop();
        if (isNeedToParseCharacters()) {
            value.value = Long.parseLong(getCharacters());
        }
        return value;
    }

    private static Frame tryParseLegacyBacktrace(Attributes attributes) {
        String fmt = attributes.getValue("fmt");
        if (fmt == null) {
            return null;
        }

        String nameOrAddr = fmt.split("←")[0].trim();
        Frame frame = new Frame(-1 /* fake frame */, nameOrAddr, -1 /* need to parse nested text-addresses */);
        // Legacy backtraces missing info about a library a symbol belongs to. But if a symbol's name is known,
        // then it's definitely not JIT-compiled code. In that case [unknown] binary name is used to improve profiling
        // results.
        if (!nameOrAddr.startsWith("0x")) {
            frame.binary = "[unknown]";
        }
        return frame;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // check that <schema> has required table name
        if (qName.equals(XCTraceTableHandler.SCHEMA)) {
            String schemaName = parseName(attributes);
            if (!tableType.tableName.equals(schemaName)) {
                throw new IllegalStateException("Results contains schema with unexpected name: " + schemaName);
            }
            return;
        }
        switch (qName) {
            case XCTraceTableHandler.SAMPLE:
                currentSample = new XCTraceSample();
                break;
            case XCTraceTableHandler.SAMPLE_TIME:
            case XCTraceTableHandler.CYCLE_WEIGHT:
            case XCTraceTableHandler.WEIGHT:
            case XCTraceTableHandler.PMC_EVENT:
            case XCTraceTableHandler.TEXT_ADDRESSES:
                pushCachedOrNew(attributes, id -> {
                    setNeedParseCharacters(true);
                    return new LongHolder(id);
                });
                break;
            case XCTraceTableHandler.BACKTRACE:
                // Starting from version ~14.3 backtraces contains all required details and saved as a sequnces
                // of <frame> elements.
                // For older versions, there are no frames. Instead, backtraces have "fmt" attribute which containes
                // the name of the symbol on the top of the call stack. Addresses are stored in a few nested
                // <text-addresses> elements.
                pushCachedOrNew(attributes, id -> {
                    ValueHolder<Frame> holder = new ValueHolder<Frame>(id);
                    holder.value = tryParseLegacyBacktrace(attributes);
                    return holder;
                });
                break;
            case XCTraceTableHandler.BINARY:
                pushCachedOrNew(attributes, id -> new ValueHolder<>(id, parseName(attributes)));
                break;
            case XCTraceTableHandler.FRAME:
                // Addresses in cpu-* tables are always biased by 1, on both X86_64 and AArch64.
                // At the same type, corresponding source tables contain correct addressed.
                // See: https://developer.apple.com/forums/thread/748112
                pushCachedOrNew(attributes, id -> new Frame(id, parseName(attributes),
                        parseAddress(attributes) - 1L));
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals(XCTraceTableHandler.NODE)) {
            return;
        }
        switch (qName) {
            case XCTraceTableHandler.SAMPLE:
                callback.accept(currentSample);
                currentSample = null;
                break;
            case XCTraceTableHandler.SAMPLE_TIME: {
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setTime(value.value);
                break;
            }
            case XCTraceTableHandler.CYCLE_WEIGHT:
            case XCTraceTableHandler.WEIGHT:
            case XCTraceTableHandler.PMC_EVENT:
                // in practice, a sample's row will contain only one of these
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setWeight(value.value);
                break;
            case XCTraceTableHandler.BACKTRACE:
                Frame topFrame = this.<ValueHolder<Frame>>pop().value;
                currentSample.setTopFrame(topFrame.address, topFrame.name, topFrame.binary);
                break;
            case XCTraceTableHandler.BINARY:
                ValueHolder<String> bin = pop();
                this.<Frame>peek().binary = bin.value;
                break;
            case XCTraceTableHandler.FRAME:
                Frame frame = pop();
                ValueHolder<Frame> backtrace = peek();
                // we only need a top frame
                if (backtrace.value == null) {
                    backtrace.value = frame;
                }
                break;
            case XCTraceTableHandler.TEXT_ADDRESSES: {
                LongHolder addresses = pop();
                if (isNeedToParseCharacters()) {
                    // peek only the first address as we're not interested in the whole backtrace
                    addresses.value = Arrays.stream(getCharacters().split(" "))
                            .mapToLong(Long::parseUnsignedLong).findFirst().orElse(-1);
                }
                ValueHolder<Frame> bt = peek();
                // For legacy backtraces, the address is initially -1. It is then updated by the top-most address
                // extracted from text-addresses elements.
                if (bt.value.address == -1 && addresses.value != -1) {
                    bt.value.address = addresses.value;
                }
                break;
            }

        }
        setNeedParseCharacters(false);
    }

    @Override
    public void endDocument() {
        entriesCache.clear();
        entriesStack.clear();
    }

    private static abstract class TraceElement {
        public final long id;

        public TraceElement(long id) {
            this.id = id;
        }
    }

    private static final class ValueHolder<T> extends TraceElement {
        public T value;

        ValueHolder(long id, T value) {
            super(id);
            this.value = value;
        }

        ValueHolder(long id) {
            this(id, null);
        }
    }

    private static final class LongHolder extends TraceElement {
        public long value = 0;

        public LongHolder(long id) {
            super(id);
        }
    }

    private static final class Frame extends TraceElement {
        public final String name;

        public long address;

        public String binary = null;

        public Frame(long id, String name, long address) {
            super(id);
            this.name = name;
            this.address = address;
        }
    }

    static class XCTraceSample {
        public static final String TIME_SAMPLE_TRIGGER_NAME = "TIME_MICRO_SEC";

        private long timeFromStartNs = 0;
        private long weight = 0;
        private String symbol = null;
        private long address = 0;
        private String binary = null;

        public void setTopFrame(long address, String symbol, String binary) {
            this.address = address;
            this.symbol = symbol;
            this.binary = binary;
        }

        public void setWeight(long weight) {
            this.weight = weight;
        }

        public void setTime(long time) {
            timeFromStartNs = time;
        }

        public long getTimeFromStartNs() {
            return timeFromStartNs;
        }

        public long getWeight() {
            return weight;
        }

        public long getAddress() {
            return address;
        }

        public String getBinary() {
            return binary;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
