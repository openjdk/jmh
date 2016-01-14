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
package org.openjdk.jmh.it.ccontrol;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.CompilerHints;

import java.util.Collection;

public class CompilerControlUtils {

    public static boolean check(RunResult runResult, String... checkFor) {
        LogConsumeProfiler.LogConsumeResult r = (LogConsumeProfiler.LogConsumeResult) runResult.getSecondaryResults().get("logout");
        Collection<String> lines = r.getLines();

        line: for (String line : lines) {
            System.out.println(line);
            for (String m : checkFor) {
                if (!line.contains(m)) continue line;
            }
            return true;
        }
        return false;
    }

    public static boolean hasHint(String hint, String... filters) {
        nextHint:
        for (String s : CompilerHints.defaultList().get()) {
            for (String f : filters) {
                if (!s.contains(f)) continue nextHint;
            }
            if (s.startsWith(hint)) {
                return true;
            }
        }
        return false;
    }

}
