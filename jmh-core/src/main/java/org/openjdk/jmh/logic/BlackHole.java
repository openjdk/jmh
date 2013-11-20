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

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/*
    See the rationale for BlackHoleL1..BlackHoleL4 classes below.
 */

class BlackHoleL0 {
    public int markerBegin;
}

class BlackHoleL1 extends BlackHoleL0 {
    private boolean p001, p002, p003, p004, p005, p006, p007, p008;
    private boolean p011, p012, p013, p014, p015, p016, p017, p018;
    private boolean p021, p022, p023, p024, p025, p026, p027, p028;
    private boolean p031, p032, p033, p034, p035, p036, p037, p038;
    private boolean p041, p042, p043, p044, p045, p046, p047, p048;
    private boolean p051, p052, p053, p054, p055, p056, p057, p058;
    private boolean p061, p062, p063, p064, p065, p066, p067, p068;
    private boolean p071, p072, p073, p074, p075, p076, p077, p078;
    private boolean p101, p102, p103, p104, p105, p106, p107, p108;
    private boolean p111, p112, p113, p114, p115, p116, p117, p118;
    private boolean p121, p122, p123, p124, p125, p126, p127, p128;
    private boolean p131, p132, p133, p134, p135, p136, p137, p138;
    private boolean p141, p142, p143, p144, p145, p146, p147, p148;
    private boolean p151, p152, p153, p154, p155, p156, p157, p158;
    private boolean p161, p162, p163, p164, p165, p166, p167, p168;
    private boolean p171, p172, p173, p174, p175, p176, p177, p178;
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
    private boolean q001, q002, q003, q004, q005, q006, q007, q008;
    private boolean q011, q012, q013, q014, q015, q016, q017, q018;
    private boolean q021, q022, q023, q024, q025, q026, q027, q028;
    private boolean q031, q032, q033, q034, q035, q036, q037, q038;
    private boolean q041, q042, q043, q044, q045, q046, q047, q048;
    private boolean q051, q052, q053, q054, q055, q056, q057, q058;
    private boolean q061, q062, q063, q064, q065, q066, q067, q068;
    private boolean q071, q072, q073, q074, q075, q076, q077, q078;
    private boolean q101, q102, q103, q104, q105, q106, q107, q108;
    private boolean q111, q112, q113, q114, q115, q116, q117, q118;
    private boolean q121, q122, q123, q124, q125, q126, q127, q128;
    private boolean q131, q132, q133, q134, q135, q136, q137, q138;
    private boolean q141, q142, q143, q144, q145, q146, q147, q148;
    private boolean q151, q152, q153, q154, q155, q156, q157, q158;
    private boolean q161, q162, q163, q164, q165, q166, q167, q168;
    private boolean q171, q172, q173, q174, q175, q176, q177, q178;
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
     * We also pad with booleans so that dense layout in superclass does not
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

    /*
     * Need to clear the sinks to break the GC from keeping the
     * consumed objects forever.
     */

    @Setup(Level.Iteration)
    @TearDown(Level.Iteration)
    public void clearSinks() {
        obj1 = new Object();
        objs1 = new Object[]{new Object()};
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

        tlr = (tlr * 6364136223846793005L + 1442695040888963407L);
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.obj1 = obj;
            this.tlrMask = (tlrMask << 1) + 1;
            tlr = 0;
        }
        this.tlr = tlr;
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

        tlr = (tlr * 6364136223846793005L + 1442695040888963407L);
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.objs1 = objs;
            this.tlrMask = (tlrMask << 1) + 1;
            tlr = 0;
        }
        this.tlr = tlr;
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
