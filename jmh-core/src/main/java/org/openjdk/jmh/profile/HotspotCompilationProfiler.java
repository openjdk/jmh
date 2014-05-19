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

import sun.management.HotspotCompilationMBean;
import sun.management.counter.Counter;

import java.util.List;
import java.util.Map;

class HotspotCompilationProfiler extends AbstractHotspotProfiler {

    public HotspotCompilationProfiler(String name, boolean verbose) {
        super(name, verbose);
    }

    @Override
    public List<Counter> getCounters() {
        return AbstractHotspotProfiler.<HotspotCompilationMBean>getInstance("HotspotCompilationMBean").getInternalCompilerCounters();
    }

    @Override
    public HotspotInternalResult endProfile() {
        return new HotspotCompilationResult(super.endProfile());
    }

    static class HotspotCompilationResult extends HotspotInternalResult {

        public final String result;

        public HotspotCompilationResult(HotspotInternalResult s) {
            super(s);

            Map<String, Long> diff = s.getDiff();
            Map<String, Long> current = s.getCurrent();

            StringBuilder builder = new StringBuilder();

            builder.append(
                    String.format("wall time = %.3f secs, JIT time = %.3f secs, %d threads\n",
                            s.getDurationMsec() / 1000.0, deNull(diff.get("java.ci.totalTime")) / 1000.0 / 1000.0, deNull(current.get("sun.ci.threads"))
                    )
            );

            builder.append(
                    String.format("%14s %+d compiles, %+d bailouts, %+d invalidates, %+d native bytes generated, %+d instructions\n",
                            "",
                            deNull(diff.get("sun.ci.totalCompiles")),
                            deNull(diff.get("sun.ci.totalBailouts")),
                            deNull(diff.get("sun.ci.totalInvalidates")),
                            deNull(diff.get("sun.ci.nmethodCodeSize")),
                            deNull(diff.get("sun.ci.nmethodSize"))
                    )
            );

            builder.append(
                    String.format("%14s %+d OSR compiles, %+d bytecode bytes compiled, %+d us spent\n",
                            "",
                            deNull(diff.get("sun.ci.osrCompiles")),
                            deNull(diff.get("sun.ci.osrBytes")),
                            deNull(diff.get("sun.ci.osrTime"))
                    )
            );

            builder.append(
                    String.format("%14s %+d normal compiles, %+d bytecode bytes compiled, %+d us spent\n",
                            "",
                            deNull(diff.get("sun.ci.standardCompiles")),
                            deNull(diff.get("sun.ci.standardBytes")),
                            deNull(diff.get("sun.ci.standardTime"))
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

