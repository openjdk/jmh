/**
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
package org.openjdk.jmh.logic;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Black Hole.
 * <p/>
 * Harness is supposed to have a thread-local black hole available for every test.
 * Writes ("consumes") to black hole have the side effect for JIT to not to eliminate
 * computations resulted in blackholed value.
 *
 * @author aleksey.shipilev@oracle.com
 */
@State(Scope.Thread) // Blackholes are always acting like a thread-local state
public class BlackHole {

    /**
     * IMPLEMENTATION NOTES:
     * <p/>
     * These fields are public to trick JIT into believing
     * someone can finally read these values. There are fields to primitive types
     * as well to prevent auto-boxing.
     * <p/>
     * Note that side-effect is guaranteed only for latest object put in blackhole.
     * If you need to blackhole multiple things, either compute something on them,
     * and blackhole the result, or use different blackholes.
     * <p/>
     * Objects are consumed with the aliasing trick: we can't normally afford
     * to write the object reference, because Blackhole can already be in the old
     * gen, and writing the reference will entail GC write barrier.
     * <p/>
     * We make the trick of reading some "arbitrary" offset of Blackhole and
     * compare the incoming object against it. We inherently piggyback on JIT
     * inability to figure out the offset, being read from the raw memory each time
     * at runtime. The offset is always the same, so the read always returns the
     * unique object. In the face However, JIT can not predict the stored object would be
     * the same, and it should pessimistically keep the object around.
     */

    public byte b;
    public boolean bool;
    public char c;
    public short s;
    public int i;
    public long l;
    public float f;
    public double d;

    // globally reachable, volatile for a reason, see impl. note
    public volatile Object uniqueObj = new Object();

    private static Unsafe U;
    private static long OFFSET_ADDR;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        OFFSET_ADDR = U.allocateMemory(16);
        try {
            long offset = U.objectFieldOffset(BlackHole.class.getDeclaredField("uniqueObj"));
            U.putLong(OFFSET_ADDR, offset);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param obj object to consume.
     */
    public final void consume(Object obj) {
        if (obj == U.getObjectVolatile(this, U.getLong(OFFSET_ADDR))) {
            // VERY UNLIKELY, and will in the end result in the exception.
            StringBuilder sb = new StringBuilder();
            try {
                for (Field f : obj.getClass().getDeclaredFields()) {
                    sb.append(f.get(obj).toString());
                    sb.append(",");
                }
            } catch (Throwable t) {
                // do nothing
            }
            throw new Error("JMH infrastructure bug: obj = " + obj + ", sb = " + sb);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param objs objects to consume.
     */
    public final void consume(Object[] objs) {
        if (objs == U.getObjectVolatile(this, U.getLong(OFFSET_ADDR))) {
            // VERY UNLIKELY, and will in the end result in the exception.
            StringBuilder sb = new StringBuilder();
            for (Object obj : objs) {
                try {
                    for (Field f : obj.getClass().getDeclaredFields()) {
                        sb.append(f.get(obj).toString());
                        sb.append(",");
                    }
                } catch (Throwable t) {
                    // do nothing
                }
            }
            throw new Error("JMH infrastructure bug: objs = " + Arrays.toString(objs) + ", sb = " + sb);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param b object to consume.
     */
    public final void consume(byte b) {
        this.b = b;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param bool object to consume.
     */
    public final void consume(boolean bool) {
        this.bool = bool;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param c object to consume.
     */
    public final void consume(char c) {
        this.c = c;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param s object to consume.
     */
    public final void consume(short s) {
        this.s = s;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param i object to consume.
     */
    public final void consume(int i) {
        this.i = i;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param l object to consume.
     */
    public final void consume(long l) {
        this.l = l;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param f object to consume.
     */
    public final void consume(float f) {
        this.f = f;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param d object to consume.
     */
    public final void consume(double d) {
        this.d = d;
    }

    public static long consumedCPU;

    /**
     * Consume some amount of time tokens.
     * This call does the CPU work linear to the number of tokens.
     * <p>
     * One token is really small, around 40 clocks on 2.0 Ghz i5.
     * The method is also having static overhead of around 20ns per call.
     *
     * @param tokens CPU tokens to consume
     */
    public static void consumeCPU(long tokens) {
        // randomize start so that JIT could not memoize;
        // this costs significantly on low token count.
        long t = System.nanoTime();

        for (long i = 0; i < tokens; i++) {
            t += (t * 0x5DEECE66DL + 0xBL) & (0xFFFFFFFFFFFFL);
        }

        // need to guarantee side-effect on the result,
        // but can't afford contention; make sure we update the shared state
        // only in the unlikely even
        if (t == 42) {
            consumedCPU += t;
        }
    }

}
