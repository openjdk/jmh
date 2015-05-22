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
package org.openjdk.jmh.runner.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import joptsimple.internal.Reflection;

/**
 * Converts option value from {@link String} to {@link Integer} and makes sure the value exceeds given minimal threshold.
 */
public class IntegerValueConverter implements ValueConverter<Integer> {
    private final static ValueConverter<Integer> TO_INT_CONVERTER = Reflection.findConverter(int.class);

    public final static IntegerValueConverter POSITIVE = new IntegerValueConverter(1);
    public final static IntegerValueConverter NON_NEGATIVE = new IntegerValueConverter(0);

    private final int minValue;

    public IntegerValueConverter(int minValue) {
        this.minValue = minValue;
    }

    @Override
    public Integer convert(String value) {
        Integer newValue = TO_INT_CONVERTER.convert(value);
        if (newValue == null) {
            // should not get here
            throw new ValueConversionException("value should not be null");
        }

        if (newValue < minValue) {
            String message = "The given value " + value + " should be ";
            if (minValue == 0) {
                message += "non-negative";
            } else if (minValue == 1) {
                message += "positive";
            } else {
                message += "greater or equal than " + minValue;
            }
            throw new ValueConversionException(message);
        }
        return newValue;
    }

    @Override
    public Class<Integer> valueType() {
        return TO_INT_CONVERTER.valueType();
    }

    @Override
    public String valuePattern() {
        return "int";
    }
}
