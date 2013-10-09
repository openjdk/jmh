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
package org.openjdk.jmh.logic;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/*
    See the rationale for BlackHoleL1..BlackHoleL4 classes below.
 */

class BlackHoleL0 {
    public int markerBegin;
}

class BlackHoleL1 extends BlackHoleL0 {
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;
    public int p21, p22, p23, p24, p25, p26, p27, p28;
    public int p31, p32, p33, p34, p35, p36, p37, p38;
}

class BlackHoleL2 extends BlackHoleL1 {
    public volatile byte b1 = 1, b2 = 2;
    public volatile boolean bool1 = false, bool2 = true;
    public volatile char c1 = 'A', c2 = 'B';
    public volatile short s1 = 1, s2 = 2;
    public volatile int i1 = 1, i2 = 2;
    public volatile long l1 = 1, l2 = 2;
    public volatile float f1 = 1.0f, f2 = 2.0f;
    public volatile double d1 = 1.0d, d2 = 2.0d;
    public volatile Object obj1 = new Object();
    public volatile Object[] objs1 = new Object[]{new Object()};
    public volatile BlackHoleL2 nullBait = null;
    public long tlr = System.nanoTime();
    public long tlrMask = 1;
}

class BlackHoleL3 extends BlackHoleL2 {
    public int e01, e02, e03, e04, e05, e06, e07, e08;
    public int e11, e12, e13, e14, e15, e16, e17, e18;
    public int e21, e22, e23, e24, e25, e26, e27, e28;
    public int e31, e32, e33, e34, e35, e36, e37, e38;
}

class BlackHoleL4 extends BlackHoleL3 {
    public int markerEnd;
}

/**
 * Black Hole.
 * <p/>
 * Black hole "consumes" the values, conceiving no information to JIT whether the
 * value is actually used afterwards. This can save from the dead-code elimination
 * of the computations resulting in the given values.
 *
 * @author aleksey.shipilev@oracle.com
 */
@State(Scope.Thread) // Blackholes are always acting like a thread-local state
public class BlackHole extends BlackHoleL4 {

    /**
     * IMPLEMENTATION NOTES:
     * <p/>
     * The major things to dodge with Blackholes are:
     *   a) dead-code elimination: the arguments should be used on every call,
     *      so that compilers are unable to fold them into constants or
     *      otherwise optimize them away along with the computations resulted
     *      in them.
     *   b) false sharing: reading/writing the state may disturb the cache
     *      lines. We need to isolate the critical fields to achieve tolerable
     *      performance.
     *   c) write wall: we need to ease off on writes as much as possible,
     *      since it disturbs the caches, pollutes the write buffers, etc.
     *      This may very well result in hitting the memory wall prematurely.
     *      Reading memory is fine as long as it is cacheable.
     * <p/>
     * To achieve these goals, we are piggybacking on several things in the
     * compilers:
     * <p/>
     * 1. Superclass fields are not reordered with the subclass' fields.
     * No practical VM that we are aware of is doing this. It is unpractical,
     * because if the superclass fields are at the different offsets in two
     * subclasses, the VMs would then need to do the polymorphic access for
     * the superclass fields.
     * <p/>
     * This allows us to "squash" the protected fields in the inheritance
     * hierarchy so that the padding in super- and sub-class are laid out
     * right before and right after the protected fields.
     * <p/>
     * We also pad with "int"-s so that dense layout in superclass does not
     * have the gap where runtime can fit the subclass field.
     * <p/>
     * 2. Compilers are unable to predict the value of the volatile read.
     * While the compilers can speculatively optimize until the relevant
     * volatile write happens, it is unlikely to be practical to be able to stop
     * all the threads the instant that write had happened.
     * <p/>
     * This allows us to compare the incoming values against the relevant
     * volatile fields. The values in those volatile fields are never changing,
     * but due to (2), we should re-read the values again and again.
     * <p/>
     * Primitives are a bit hard, because we can't predict what values we
     * will be fed. But we can compare the incoming value with *two* distinct
     * known values, and both checks will never be true at the same time.
     * Note the bitwise AND in all the predicates: both to spare additional
     * branch, and also to provide more uniformity in the performance.
     * <p/>
     * Objects should normally abide the Java's referential semantics, i.e. the
     * incoming objects will never be equal to the distinct object we have, and
     * volatile read will break the speculation about what we compare with.
     * However, smart compilers may deduce that the distinct non-escaped object
     * on the other side is not equal to anything we have, and fold the comparison
     * to "false". We do inlined thread-local random to get those objects escaped
     * with infinitesimal probability. Then again, smart compilers may skip from
     * generating the slow path, and apply the previous logic to constant-fold
     * the condition to "false". We are warming up the slow-path in the beginning
     * to evade that effect.
     */

    private static final Unsafe U;

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

        consistencyCheck();
    }

    static void consistencyCheck() {
        // checking the fields are not reordered
        check("b1");
        check("b2");
        check("bool1");
        check("bool2");
        check("c1");
        check("c2");
        check("s1");
        check("s2");
        check("i1");
        check("i2");
        check("l1");
        check("l2");
        check("f1");
        check("f2");
        check("d1");
        check("d2");
        check("obj1");
        check("objs1");
    }

    static void check(String fieldName) {
        final long requiredGap = 128;
        long markerBegin = getOffset("markerBegin");
        long markerEnd = getOffset("markerEnd");
        long off = getOffset(fieldName);
        if (markerEnd - off < requiredGap || off - markerBegin < requiredGap) {
            throw new IllegalStateException("Consistency check failed for " + fieldName + ", off = " + off + ", markerBegin = " + markerBegin + ", markerEnd = " + markerEnd);
        }
    }

    static long getOffset(String fieldName) {
        try {
            Field f = BlackHole.class.getField(fieldName);
            return U.objectFieldOffset(f);
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
        // let's play the optimizing compiler, dude!
        long tlr = this.tlr;
        long tlrMask = this.tlrMask;

        this.tlr = (tlr * 0x5DEECE66DL + 0xBL) & (0xFFFFFFFFFFFFL);
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.tlrMask = (tlrMask << 1) + 1;
            this.obj1 = obj;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param objs objects to consume.
     */
    public final void consume(Object[] objs) {
        // let's play the optimizing compiler, dude!
        long tlr = this.tlr;
        long tlrMask = this.tlrMask;

        this.tlr = (tlr * 0x5DEECE66DL + 0xBL) & (0xFFFFFFFFFFFFL);
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.tlrMask = (tlrMask << 1) + 1;
            this.objs1 = objs;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param b object to consume.
     */
    public final void consume(byte b) {
        if (b == b1 & b == b2) {
            // SHOULD NEVER HAPPEN
            nullBait.b1 = b; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param bool object to consume.
     */
    public final void consume(boolean bool) {
        if (bool == bool1 & bool == bool2) {
            // SHOULD NEVER HAPPEN
            nullBait.bool1 = bool; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param c object to consume.
     */
    public final void consume(char c) {
        if (c == c1 & c == c2) {
            // SHOULD NEVER HAPPEN
            nullBait.c1 = c; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param s object to consume.
     */
    public final void consume(short s) {
        if (s == s1 & s == s2) {
            // SHOULD NEVER HAPPEN
            nullBait.s1 = s; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param i object to consume.
     */
    public final void consume(int i) {
        if (i == i1 & i == i2) {
            // SHOULD NEVER HAPPEN
            nullBait.i1 = i; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param l object to consume.
     */
    public final void consume(long l) {
        if (l == l1 & l == l2) {
            // SHOULD NEVER HAPPEN
            nullBait.l1 = l; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param f object to consume.
     */
    public final void consume(float f) {
        if (f == f1 & f == f2) {
            // SHOULD NEVER HAPPEN
            nullBait.f1 = f; // implicit null pointer exception
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param d object to consume.
     */
    public final void consume(double d) {
        if (d == d1 & d == d2) {
            // SHOULD NEVER HAPPEN
            nullBait.d1 = d; // implicit null pointer exception
        }
    }

    public static volatile long consumedCPU = 42;

    /**
     * Consume some amount of time tokens.
     * This method does the CPU work almost linear to the number of tokens.
     * One token is really small, around 3 clocks on 2.0 Ghz i5,
     * see JMH samples for the complete demo.
     *
     * @param tokens CPU tokens to consume
     */
    public static void consumeCPU(long tokens) {
        // randomize start so that JIT could not memoize;
        long t = consumedCPU;

        for (long i = 0; i < tokens; i++) {
            t += (t * 0x5DEECE66DL + 0xBL) & (0xFFFFFFFFFFFFL);
        }

        // need to guarantee side-effect on the result,
        // but can't afford contention; make sure we update the shared state
        // only in the unlikely case, so not to do the furious writes,
        // but still dodge DCE.
        if (t == 42) {
            consumedCPU += t;
        }
    }

}
