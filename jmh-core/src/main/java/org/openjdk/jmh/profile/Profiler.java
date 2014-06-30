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

import java.util.List;

/**
 * Root profiler interface.
 *
 * <p>Profilers normally implement one of the subinterfaces.</p>
 * @see org.openjdk.jmh.profile.ExternalProfiler
 * @see org.openjdk.jmh.profile.InternalProfiler
 */
public interface Profiler {

    /**
     * Check the support for a given profiler.
     * @param msgs where to put the diagnostic messages
     * @return true, if suppported
     */
    boolean checkSupport(List<String> msgs);

    /**
     * Unique label for the profiler.
     * @return label
     */
    String label();

    /**
     * Human-readable one-line description of the profiler.
     * @return description
     */
    String getDescription();

}
