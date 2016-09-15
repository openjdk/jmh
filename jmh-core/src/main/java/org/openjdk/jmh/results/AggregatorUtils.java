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
package org.openjdk.jmh.results;

import java.util.Collection;

public final class AggregatorUtils {

    private AggregatorUtils() {
        // prevent instantation
    }

    static ResultRole aggregateRoles(Collection<? extends Result> results) {
        ResultRole result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.role;
            } else if (result != r.role) {
                throw new IllegalStateException("Combining the results with different roles");
            }
        }
        return result;
    }

    static String aggregateUnits(Collection<? extends Result> results) {
        String result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.unit;
            } else if (!result.equals(r.unit)) {
                throw new IllegalStateException("Combining the results with different units");
            }
        }
        return result;
    }

    static String aggregateLabels(Collection<? extends Result> results) {
        String result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.label;
            } else if (!result.equals(r.label)) {
                throw new IllegalStateException("Combining the results with different labels");
            }
        }
        return result;
    }

    static AggregationPolicy aggregatePolicies(Collection<? extends Result> results) {
        AggregationPolicy result = null;
        for (Result r : results) {
            if (result == null) {
                result = r.policy;
            } else if (!result.equals(r.policy)) {
                throw new IllegalStateException("Combining the results with different aggregation policies");
            }
        }
        return result;
    }

}
