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
package org.openjdk.jmh.util.internal;

import java.util.Random;

public class BootstrappedStatistics extends Statistics {

    private static final long serialVersionUID = 2166351427129447724L;

    private static final int BOOTSTRAP_ITERS = Integer.getInteger("bootstrap.cycles", 100);

    private final int bootstrapIters;

    public BootstrappedStatistics(Statistics s) {
        this(s, BOOTSTRAP_ITERS);
    }

    public BootstrappedStatistics(Statistics s, int bootstrapIters) {
        super(s.getValues());
        this.bootstrapIters = bootstrapIters;
    }

    public BootstrappedStatistics(int bootstrapIters) {
        super();
        this.bootstrapIters = bootstrapIters;
    }

    /**
     * Gets the bootstrapped estimate of percentile.
     * @param p percentile to estimate
     * @return result
     */
    public Statistics getBootPercentile(double p) {
        if (bootstrapIters <= 1) {
            return this;
        }

        Statistics r = new Statistics();

        double[] values = getValues();

        Random rnd = new Random();
        for (int c = 0; c < bootstrapIters; c++) {
            double[] newValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[rnd.nextInt(values.length)];
            }
            Statistics s = new Statistics(newValues);
            r.addValue(s.getPercentile(p));
        }

        return r;
    }

    @Override
    public double getPercentile(double p) {
        return getBootPercentile(p).getMean();
    }

}
