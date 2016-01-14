/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.util;

public class ScoreFormatter {

    private static final int PRECISION = Integer.getInteger("jmh.scorePrecision", 3);
    private static final double ULP = 1.0 / Math.pow(10, PRECISION);
    private static final double THRESHOLD = ULP / 2;

    public static boolean isApproximate(double score) {
        return (score < THRESHOLD);
    }

    public static String format(double score) {
        if (isApproximate(score)) {
            int power = (int) Math.round(Math.log10(score));
            return "\u2248 " + ((power != 0) ? "10" + superscript("" + power) : "0");
        } else {
            return String.format("%." + PRECISION + "f", score);
        }
    }

    public static String format(int width, double score) {
        if (isApproximate(score)) {
            int power = (int) Math.round(Math.log10(score));
            return String.format("%" + width + "s", "\u2248 " + ((power != 0) ? "10" + superscript("" + power) : "0"));
        } else {
            return String.format("%" + width + "." + PRECISION + "f", score);
        }
    }

    public static String formatExact(int width, double score) {
        return String.format("%" + width + "." + PRECISION + "f", score);
    }

    public static String formatLatex(double score) {
        if (isApproximate(score)) {
            int power = (int) Math.round(Math.log10(score));
            return "$\\approx " + ((power != 0) ? "10^{" + power + "}" : "0") + "$";
        } else {
            return String.format("%." + PRECISION + "f", score);
        }
    }

    public static String formatError(double error) {
        return String.format("%." + PRECISION + "f", Math.max(error, ULP));
    }

    public static String formatError(int width, double error) {
        return String.format("%" + width + "." + PRECISION + "f", Math.max(error, ULP));
    }

    public static String superscript(String str) {
        str = str.replaceAll("-", "\u207b");
        str = str.replaceAll("0", "\u2070");
        str = str.replaceAll("1", "\u00b9");
        str = str.replaceAll("2", "\u00b2");
        str = str.replaceAll("3", "\u00b3");
        str = str.replaceAll("4", "\u2074");
        str = str.replaceAll("5", "\u2075");
        str = str.replaceAll("6", "\u2076");
        str = str.replaceAll("7", "\u2077");
        str = str.replaceAll("8", "\u2078");
        str = str.replaceAll("9", "\u2079");
        return str;
    }

}
