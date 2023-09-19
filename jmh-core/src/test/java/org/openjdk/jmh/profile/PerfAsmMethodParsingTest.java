/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.util.JDKVersion;
import org.openjdk.jmh.util.Utils;

import java.util.*;

public class PerfAsmMethodParsingTest {

    private static final String PAYLOAD_CLASS_NAME = PerfAsmMethodPayload.class.getName();

    private static AbstractPerfAsmProfiler selectProfiler() throws ProfilerException {
        if (Utils.isWindows()) {
            return new WinPerfAsmProfiler("");
        } else if (Utils.isMacos()) {
            return new DTraceAsmProfiler("sudo=false");
        } else if (Utils.isLinux()) {
            return new LinuxPerfAsmProfiler("");
        }
        throw new RuntimeException("Unknown OS: " + System.getProperty("os.name"));
    }

    public static void checkFor(String log, String... msgs) {
        for (String msg : msgs) {
            if (log.contains(msg)) return;
        }
        System.out.println(log);
        Assert.fail("Cannot find any entry: " + Arrays.toString(msgs));
    }

    @Test
    public void checkJDK() {
        AbstractPerfAsmProfiler profiler;
        try {
            profiler = selectProfiler();
        } catch (ProfilerException e) {
            // Profiler exception in the test, not available?
            e.printStackTrace();
            return;
        }

        List<String> args = new ArrayList<>();
        args.add(Utils.getCurrentJvm());
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.addAll(profiler.addJVMOptions(null));
        args.add(PAYLOAD_CLASS_NAME);

        Collection<String> out = Utils.runWith(args);
        System.out.println(out);

        AbstractPerfAsmProfiler.Assembly assembly = profiler.readAssembly();

        StringBuilder sb = new StringBuilder();
        for (AbstractPerfAsmProfiler.MethodDesc md : assembly.methodMap.allValues()) {
            sb.append(md);
            sb.append(System.lineSeparator());
        }

        String methods = sb.toString();

        if (JDKVersion.parseMajor(System.getProperty("java.version")) >= 22) {
            // These rely on logging available in up-to-date JDKs.
            // At the time of writing, only JDK 22 contained all these fixes.
            // TODO: As the relevant JDK updates get backported, consider bumping the versions down.

            // Added by JDK-8316514
            checkFor(methods, "runtime stub: VtableStub vtbl[");
            checkFor(methods, "runtime stub: VtableStub itbl[");

            // Added by JDK-8316178
            checkFor(methods, "runtime stub: ExceptionBlob");
            checkFor(methods, "runtime stub: _complete_monitor_locking_Java");
            checkFor(methods, "runtime stub: StackOverflowError throw_exception");
        }

        // StubRoutines
        checkFor(methods, "runtime stub: StubRoutines::call_stub");

        // Interpreter: bytecode stub
        checkFor(methods, "interpreter: iconst_0");

        // Interpreter: auxiliary stubs
        checkFor(methods, "interpreter: native method entry point (kind = native)",
                          "interpreter: method entry point (kind = native)");
        checkFor(methods, "interpreter: exception handling");

        // Native method
        checkFor(methods, "Unknown, level 0: java.lang.System::currentTimeMillis");

        // Hot Java method passing the tiered compilation pipeline
        checkFor(methods, "C1, level 2: " + PAYLOAD_CLASS_NAME + "$C1::doWork",
                          "C1, level 3: " + PAYLOAD_CLASS_NAME + "$C1::doWork");
        checkFor(methods, "C1, level 2: " + PAYLOAD_CLASS_NAME + "$C2::doWork",
                          "C1, level 3: " + PAYLOAD_CLASS_NAME + "$C2::doWork");
        checkFor(methods, "C1, level 2: " + PAYLOAD_CLASS_NAME + "$C3::doWork",
                          "C1, level 3: " + PAYLOAD_CLASS_NAME + "$C3::doWork");

        checkFor(methods, "C2, level 4: " + PAYLOAD_CLASS_NAME + "$C1::doWork");
        checkFor(methods, "C2, level 4: " + PAYLOAD_CLASS_NAME + "$C2::doWork");
        checkFor(methods, "C2, level 4: " + PAYLOAD_CLASS_NAME + "$C3::doWork");
    }

}
