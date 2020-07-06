/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmh.util.lines;

/**
 * This implements a simple String armoring scheme that resembles Base64.
 * We cannot use Base64 implementations from JDK yet, because the lowest language level is at 7.
 */
class Armor {

    // 64 characters, plus a padding symbol at the end.
    static final String DICT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static void encodeStep(int[] ibuf, int[] obuf) {
        obuf[0] = ((ibuf[0] & 0x3F)     );
        obuf[1] = ((ibuf[0] & 0xFF) >> 6) + ((ibuf[1] & 0xF) << 2);
        obuf[2] = ((ibuf[1] & 0xFF) >> 4) + ((ibuf[2] & 0x3) << 4);
        obuf[3] = ((ibuf[2] & 0xFF) >> 2);

        obuf[4] = ((ibuf[3] & 0x3F)     );
        obuf[5] = ((ibuf[3] & 0xFF) >> 6) + ((ibuf[4] & 0xF) << 2);
        obuf[6] = ((ibuf[4] & 0xFF) >> 4) + ((ibuf[5] & 0x3) << 4);
        obuf[7] = ((ibuf[5] & 0xFF) >> 2);
    }

    private static void decodeStep(int[] ibuf, int[] obuf) {
        obuf[0] = (((ibuf[0] & 0xFF)     ) + ((ibuf[1] & 0x3)  << 6));
        obuf[1] = (((ibuf[1] & 0xFF) >> 2) + ((ibuf[2] & 0xF)  << 4));
        obuf[2] = (((ibuf[2] & 0xFF) >> 4) + ((ibuf[3] & 0x3F) << 2));

        obuf[3] = (((ibuf[4] & 0xFF)     ) + ((ibuf[5] & 0x3)  << 6));
        obuf[4] = (((ibuf[5] & 0xFF) >> 2) + ((ibuf[6] & 0xF)  << 4));
        obuf[5] = (((ibuf[6] & 0xFF) >> 4) + ((ibuf[7] & 0x3F) << 2));
    }

    public static String encode(String src) {
        StringBuilder sb = new StringBuilder();
        char[] chars = src.toCharArray();

        int[] ibuf = new int[6];
        int[] obuf = new int[8];

        for (int c = 0; c < chars.length / 3; c++) {
            for (int i = 0; i < 3; i++) {
                ibuf[i*2 + 0] =  chars[c*3 + i]       & 0xFF;
                ibuf[i*2 + 1] = (chars[c*3 + i] >> 8) & 0xFF;
            }

            encodeStep(ibuf, obuf);

            for (int i = 0; i < 8; i++) {
                sb.append(DICT.charAt(obuf[i]));
            }
        }

        int tail = chars.length % 3;
        if (tail != 0) {
            int tailStart = chars.length / 3 * 3;
            char PAD = DICT.charAt(DICT.length() - 1);

            for (int i = 0; i < tail; i++) {
                ibuf[i*2 + 0] =  chars[tailStart + i]       & 0xFF;
                ibuf[i*2 + 1] = (chars[tailStart + i] >> 8) & 0xFF;
            }
            for (int i = tail; i < 3; i++) {
                ibuf[i*2 + 0] = 0;
                ibuf[i*2 + 1] = 0;
            }

            encodeStep(ibuf, obuf);

            for (int i = 0; i < tail*3; i++) {
                sb.append(DICT.charAt(obuf[i]));
            }
            for (int i = tail*3; i < 8; i++) {
                sb.append(PAD);
            }
        }

        return sb.toString();
    }

    public static String decode(String encoded) {
        char[] encChars = encoded.toCharArray();
        char[] decChars = new char[encChars.length / 8 * 3];

        if (encChars.length % 8 != 0) {
            throw new IllegalArgumentException("The length should be multiple of 8");
        }

        final int PAD_IDX = DICT.length() - 1;

        int[] ibuf = new int[8];
        int[] obuf = new int[6];

        int oLen = 0;
        int cut = 0;
        for (int c = 0; c < encChars.length/8; c++) {
            for (int i = 0; i < 8; i++) {
                ibuf[i] = DICT.indexOf(encChars[c*8 + i]);
            }

            if (ibuf[3] == PAD_IDX) {
                for (int i = 3; i < 8; i++) {
                    ibuf[i] = 0;
                }
                cut = 2;
            } else if (ibuf[6] == PAD_IDX) {
                for (int i = 6; i < 8; i++) {
                    ibuf[i] = 0;
                }
                cut = 1;
            }

            decodeStep(ibuf, obuf);

            decChars[oLen++] = (char)(obuf[0] + (obuf[1] << 8));
            decChars[oLen++] = (char)(obuf[2] + (obuf[3] << 8));
            decChars[oLen++] = (char)(obuf[4] + (obuf[5] << 8));
        }

        return new String(decChars, 0, decChars.length - cut);
    }

}
