/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.generators.core;

import java.util.regex.Pattern;

/**
 * Java literals support for benchmark generator.
 * <p>
 * This class is designed for JMH-specific usages and is not
 * supposed to be stable, backwards-compatible or usable outside JMH machinery.
 */
public final class NumericLiteralsParser {
    /*
     * Regexes are written looking at JLS 3.10.x grammar.
     * Trailing 'L' in long literals is not supported.
     *
     * Grammar:
     *
     * ### Integers
     * IntegerLiteral = (DecimalNumeral | HexNumeral | OctalNumeral | BinaryNumeral) [lL]?
     * Digits = [0-9] ([0-9_]* [0-9])? // Same pattern for others
     * DecimalNumeral = 0 | [1-9] [0-9]* | [1-9] [_]+ Digits
     *
     * HexNumeral = 0 [xX] HexDigits
     * BinaryNumeral = 0 [bB] BinaryDigits
     * OctalNumeral = 0 [_]* OctalDigits
     *
     * ### Floating points
     * FloatingPointLiteral = (DecimalFloatingPointLiteral | HexadecimalFloatingPointLiteral) [fFdD]?
     *
     * DecimalFloatingPointLiteral =
     *     Digits . Digits? Exponent?
     *     . Digits Exponent?
     *     Digits Exponent?
     *
     * Exponent = [eE] [+-]? Digits
     *
     * HexadecimalFloatingPointLiteral = HexSignificand BinaryExponent
     * BinaryExponent = [pP] [+-]? Digits
     * HexSignificand = HexNumeral (. HexDigits)?
     */

    /*
     * Regex that checks whether the given string is a valid Java integer (not FP!) literal.
     */
    private static final Pattern INTEGRAL_PATTERN = Pattern.compile("(" +
            "0[bB][01]([01_]*[01])?|" + // Binary literals
            "0_*[0-7]([0-7_]*[0-7])?|" + // Oct literals
            "(0|[1-9]([0-9_]*[0-9])?)|" + // Dec literals
            "0[xX][0-9a-fA-F]([0-9a-fA-F_]*[0-9a-fA-F])?" + // Hex literals
            ")"
    );

    private NumericLiteralsParser() {
    }

    /*
     * parseX methods are invoked directly from the generated benchmarks.
     * throwIfInvalidLiteral is invoked first to filter out things like trailing underscores etc.
     * that parse function just unconditionally trim.
     * We cannot ensure that the given `value` was checked via `StateObjectHandler.isParamValueConforming`
     * because of user-supplied or cmd-overridden arguments.
     */

    public static byte parseByte(String value) {
        throwIfInvalidLiteral(value);
        return (byte) parseIntegral(value, Type.BYTE);
    }

    public static short parseShort(String value) {
        throwIfInvalidLiteral(value);
        return (short) parseIntegral(value, Type.SHORT);
    }

    public static int parseInt(String value) {
        throwIfInvalidLiteral(value);
        return (int) parseIntegral(value, Type.INT);
    }

    public static long parseLong(String value) {
        throwIfInvalidLiteral(value);
        return parseIntegral(value, Type.LONG);
    }

    public static float parseFloat(String value) {
        // TODO maybe just trim underscores?
        return Float.parseFloat(value);
    }

    public static double parseDouble(String value) {
        // TODO maybe just trim underscores?
        return Double.parseDouble(value);
    }

    private static void throwIfInvalidLiteral(String value) {
         if (!INTEGRAL_PATTERN.matcher(value).matches()) {
             // Mimic parse* exception. The exact type will be shown in the second frame
             throw new NumberFormatException("For input string: \"" + value + "\"");
         }
    }

    private static long parseIntegral(String value, Type type) {
        // Invariant: value matches INTEGRAL_PATTERN
        value = value.replace("_", "");
        if (value.startsWith("0b") || value.startsWith("0B")) { // Binary
            String trimmed = value
                    .replaceFirst("0b", "")
                    .replaceFirst("0B", "");
            return type.parse(trimmed, 2);
        } else if (value.startsWith("0x") || value.startsWith("0X")) { // Hex
            String trimmed = value
                    .replaceFirst("0x", "")
                    .replaceFirst("0X", "");
            return type.parse(trimmed, 16);
        } else if (value.startsWith("0") && value.length() > 1) { // Oct
            String trimmed = value
                    .replaceFirst("0", "");
            return type.parse(trimmed, 8);
        } else { // Dec
            return type.parse(value, 10);
        }
    }

    /**
     * Checks whether the given value (received from arguments of `@Param()` annotation)
     * complies the given primitive type.
     */
    static boolean isValidLiteral(String value, Type type) {
        if (!INTEGRAL_PATTERN.matcher(value).matches()) {
            return false;
        }

        try {
            // Can pass the regex, but still be e.g. too wide for the type
            parseIntegral(value, type);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Package-private, available only for StateObjectHandler for static check
    enum Type {

        BYTE {
            @Override
            public long parse(String value, int radix) {
                return Byte.parseByte(value, radix);
            }
        },

        SHORT {
            @Override
            public long parse(String value, int radix) {
                return Short.parseShort(value, radix);
            }
        },

        INT {
            @Override
            public long parse(String value, int radix) {
                return Integer.parseInt(value, radix);
            }
        },

        LONG {
            @Override
            public long parse(String value, int radix) {
                return Long.parseLong(value, radix);
            }
        };

        public abstract long parse(String value, int radix);
    }
}
