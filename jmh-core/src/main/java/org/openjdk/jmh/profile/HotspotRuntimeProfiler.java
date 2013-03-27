/**
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

import sun.management.HotspotRuntimeMBean;
import sun.management.counter.Counter;

import java.util.List;
import java.util.Map;

public class HotspotRuntimeProfiler extends AbstractHotspotProfiler {

    public HotspotRuntimeProfiler(String name, boolean verbose) {
        super(name, verbose);
    }

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotRuntimeMBean>getInstance("HotspotRuntimeMBean").getInternalRuntimeCounters();
    }

    @Override
    public HotspotRuntimeProfilerResult endProfile() {
        return new HotspotRuntimeProfilerResult(super.endProfile());
    }

    static class HotspotRuntimeProfilerResult extends HotspotInternalResult {

        public final String result;

        public HotspotRuntimeProfilerResult(HotspotInternalResult s) {
            super(s);

            Map<String, Long> diff = s.getDiff();
            Map<String, Long> current = s.getCurrent();

            StringBuilder builder = new StringBuilder();

            builder.append(
                    String.format("%d fat monitors remaining, %+d monitors inflated, %+d monitors deflated\n",
                            deNull(current.get("sun.rt._sync_MonExtant")),
                            deNull(diff.get("sun.rt._sync_Inflations")), deNull(diff.get("sun.rt._sync_Deflations"))
                    )
            );

            builder.append(
                    String.format("%14s %+d contended lock attempts, %+d parks, %+d notify()'s, %+d futile wakeup(s)\n",
                            "",
                            deNull(diff.get("sun.rt._sync_ContendedLockAttempts")),
                            deNull(diff.get("sun.rt._sync_Parks")),
                            deNull(diff.get("sun.rt._sync_Notifications")),
                            deNull(diff.get("sun.rt._sync_FutileWakeups"))
                    )
            );

            builder.append(
                    String.format("%14s %+d safepoints hit(s), %+d ms spent on sync safepoints, %+d ms spent on safepoints\n",
                            "",
                            deNull(diff.get("sun.rt.safepoints")),
                            deNull(diff.get("sun.rt.safepointSyncTime")),
                            deNull(diff.get("sun.rt.safepointTime"))
                    )
            );

            result = builder.toString();
        }

        @Override
        public String toString() {
            return result;
        }
    }

}
