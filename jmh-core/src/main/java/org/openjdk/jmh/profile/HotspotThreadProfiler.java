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

import sun.management.HotspotThreadMBean;
import sun.management.counter.Counter;

import java.util.List;
import java.util.Map;

public class HotspotThreadProfiler extends AbstractHotspotProfiler {

    public HotspotThreadProfiler(String name, boolean verbose) {
        super(name, verbose);
    }

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotThreadMBean>getInstance("HotspotThreadMBean").getInternalThreadingCounters();
    }

    @Override
    public HotspotThreadProfilerResult endProfile() {
        return new HotspotThreadProfilerResult(super.endProfile());
    }

    static class HotspotThreadProfilerResult extends HotspotInternalResult {

        private final Long curDaemon;
        private final Long curAlive;
        private final Long curStarted;

        private final Long diffDaemon;
        private final Long diffAlive;
        private final Long diffStarted;

        public HotspotThreadProfilerResult(HotspotInternalResult superResult) {
            super(superResult);

            Map<String, Long> current = superResult.getCurrent();
            curDaemon = current.get("java.threads.daemon");
            curAlive = current.get("java.threads.live");
            curStarted = current.get("java.threads.started");

            Map<String, Long> diff = superResult.getDiff();
            diffDaemon = diff.get("java.threads.daemon");
            diffAlive = diff.get("java.threads.live");
            diffStarted = diff.get("java.threads.started");
        }

        @Override
        public String toString() {
            return String.format("%d (%+d) threads started, %d (%+d) threads alive, %d (%+d) threads daemonized %s",
                    curStarted, deNull(diffStarted),
                    curAlive, deNull(diffAlive),
                    curDaemon, deNull(diffDaemon),
                    "");
        }
    }

}
