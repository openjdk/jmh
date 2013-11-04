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
package org.openjdk.jmh.logic.results;

import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.util.internal.SampleBuffer;
import org.openjdk.jmh.util.internal.Statistics;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Result class that samples operation time.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class SampleTimePerOp extends Result {

    /** Sample buffer */
    private final SampleBuffer buffer;
    /** The TimeUnit to use when calculating the score */
    private final TimeUnit outputTimeUnit;

    /** Sets up the result with the default output unit MilliSeconds */
    public SampleTimePerOp(ResultRole mode, String label, SampleBuffer buffer) {
        this(mode, label, buffer, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets up the result
     *
     * @param outputTimeUnit The TimeUnit to use when calculating the score
     */
    public SampleTimePerOp(ResultRole mode, String label, SampleBuffer buffer, TimeUnit outputTimeUnit) {
        super(mode, label, null);
        this.buffer = buffer;
        this.outputTimeUnit = outputTimeUnit;
    }

    /** {@inheritDoc} */
    @Override
    public String getScoreUnit() {
        return TimeValue.tuToString(outputTimeUnit) + "/op";
    }

    /** {@inheritDoc} */
    @Override
    public double getScore() {
        return convertNs(buffer.getStatistics().getMean());
    }

    @Override
    public Statistics getStatistics() {
        return buffer.getStatistics();
    }

    @Override
    public Aggregator getIterationAggregator() {
        return new JoiningAggregator();
    }

    @Override
    public Aggregator getRunAggregator() {
        return new JoiningAggregator();
    }

    @Override
    public String toString() {
        Statistics stats = buffer.getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("n = ").append(stats.getN()).append(", ");
        sb.append(String.format("mean = %.0f %s",
                convertNs(stats.getMean()),
                getScoreUnit()));
        sb.append(String.format(", p{0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 1.00} = %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f %s",
                convertNs(stats.getPercentile(0)),
                convertNs(stats.getPercentile(50)),
                convertNs(stats.getPercentile(90)),
                convertNs(stats.getPercentile(95)),
                convertNs(stats.getPercentile(99)),
                convertNs(stats.getPercentile(99.9)),
                convertNs(stats.getPercentile(99.99)),
                convertNs(stats.getPercentile(100)),
                getScoreUnit()));
        return sb.toString();
    }

    @Override
    public String extendedInfo(String label) {
        Statistics stats = buffer.getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("Run result ").append((label == null) ? "" : "\"" + label + "\"").append(": \n");
        sb.append("  samples = ").append(stats.getN()).append("\n");

        if (stats.getN() > 2) {
            double[] interval95 = stats.getConfidenceInterval(0.05);
            double[] interval99 = stats.getConfidenceInterval(0.01);
            sb.append(String.format("       mean = %10.3f \u00B1(95%%) %.3f \u00B1(99%%) %.3f",
                    convertNs(stats.getMean()),
                    convertNs((interval95[1] - interval95[0]) / 2),
                    convertNs((interval99[1] - interval99[0]) / 2)
            ));
        } else {
            sb.append(String.format("       mean = %10.3f (<= 2 iterations)",
                    convertNs(stats.getMean())
            ));
        }
        sb.append(" ").append(getScoreUnit()).append("\n");

        sb.append(String.format("        min = %10.3f %s\n", convertNs(stats.getMin()), getScoreUnit()));

        for (double p : new double[] {0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 1.00}) {
            sb.append(String.format("  %9s = %10.3f %s\n",
                    "p(" + String.format("%.4f", p) + ")",
                    convertNs(stats.getPercentile(p*100)),
                    getScoreUnit()
            ));
        }

        sb.append(String.format("        max = %10.3f %s\n",  convertNs(stats.getMax()), getScoreUnit()));

        return sb.toString();
    }

    private double convertNs(double time) {
        return convert(time, TimeUnit.NANOSECONDS, outputTimeUnit);
    }

    public static double convert(double time, TimeUnit from, TimeUnit to) {
        return time * to.convert(1, TimeUnit.DAYS) / from.convert(1, TimeUnit.DAYS);
    }

    /**
     * Always add up all the samples into final result.
     * This will allow aggregate result to achieve better accuracy.
     */
    public static class JoiningAggregator implements Aggregator<SampleTimePerOp> {

        @Override
        public Result aggregate(Collection<SampleTimePerOp> results) {
            // generate new sample buffer
            ResultRole mode = null;
            String label = null;
            SampleBuffer buffer = new SampleBuffer();
            TimeUnit tu = null;
            for (SampleTimePerOp r : results) {
                tu = r.outputTimeUnit;
                label = r.label;
                buffer.addAll(r.buffer);
            }

            return new SampleTimePerOp(mode, label, buffer, tu);
        }
    }

}

