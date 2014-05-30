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
package org.openjdk.jmh.infra;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;

/*
    See the rationale for BlackholeL1..BlackholeL4 classes below.
 */

abstract class BlackholeL0 {
    public int markerBegin;
}

abstract class BlackholeL1 extends BlackholeL0 {
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

abstract class BlackholeL2 extends BlackholeL1 {
    public volatile byte b1, b2;
    public volatile boolean bool1, bool2;
    public volatile char c1, c2;
    public volatile short s1, s2;
    public volatile int i1, i2;
    public volatile long l1, l2;
    public volatile float f1, f2;
    public volatile double d1, d2;
    public volatile Object obj1;
    public volatile Object[] objs1;
    public volatile BlackholeL2 nullBait = null;
    public int tlr;
    public int tlrMask;

    public BlackholeL2() {
        Random r = new Random(System.nanoTime());
        tlr = r.nextInt();
        tlrMask = 1;
        obj1 = new Object();
        objs1 = new Object[]{new Object()};

        b1 = (byte) r.nextInt(); b2 = (byte) (b1 + 1);
        bool1 = r.nextBoolean(); bool2 = !bool1;
        c1 = (char) r.nextInt(); c2 = (char) (c1 + 1);
        s1 = (short) r.nextInt(); s2 = (short) (s1 + 1);
        i1 = r.nextInt(); i2 = i1 + 1;
        l1 = r.nextLong(); l2 = l1 + 1;
        f1 = r.nextFloat(); f2 = f1 + Math.ulp(f1);
        d1 = r.nextDouble(); d2 = d1 + Math.ulp(d1);

        if (b1 == b2) {
            throw new IllegalStateException("byte tombstones are equal");
        }
        if (bool1 == bool2) {
            throw new IllegalStateException("boolean tombstones are equal");
        }
        if (c1 == c2) {
            throw new IllegalStateException("char tombstones are equal");
        }
        if (s1 == s2) {
            throw new IllegalStateException("short tombstones are equal");
        }
        if (i1 == i2) {
            throw new IllegalStateException("int tombstones are equal");
        }
        if (l1 == l2) {
            throw new IllegalStateException("long tombstones are equal");
        }
        if (f1 == f2) {
            throw new IllegalStateException("float tombstones are equal");
        }
        if (d1 == d2) {
            throw new IllegalStateException("double tombstones are equal");
        }
    }
}

abstract class BlackholeL3 extends BlackholeL2 {
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

abstract class BlackholeL4 extends BlackholeL3 {
    public int markerEnd;
}

/**
 * Black Hole.
 *
 * <p>Black hole "consumes" the values, conceiving no information to JIT whether the
 * value is actually used afterwards. This can save from the dead-code elimination
 * of the computations resulting in the given values.</p>
 */
@State(Scope.Thread) // Blackholes are always acting like a thread-local state
public class Blackhole extends BlackholeL4 {

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
            Field f = Blackhole.class.getField(fieldName);
            return U.objectFieldOffset(f);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }


    public Blackhole() {
        /*
         * Prevent instantiation by user code. Without additional countermeasures
         * to properly escape Blackhole, its magic is not working. The instances
         * of Blackholes which are injected into benchmark methods are treated by JMH,
         * and users are supposed to only use the injected instances.
         *
         * It only *seems* simple to make the constructor non-public, but then
         * there is a lot of infrastructure code which assumes @State has a default
         * constructor. One might suggest doing the internal factory method to instantiate,
         * but that does not help when extending the Blackhole. There is a *messy* way to
         * special-case most of these problems within the JMH code, but it does not seem
         * to worth the effort.
         *
         * Therefore, we choose to fail at runtime. It will only affect the users who thought
         * "new Blackhole()" is a good idea, and these users are rare. If you are reading this
         * comment, you might be one of those users. Stay cool! Don't instantiate Blackholes
         * directly though.
         */

        IllegalStateException iae = new IllegalStateException("Blackholes should not be instantiated directly.");
        for (StackTraceElement el : iae.getStackTrace()) {
            // Either we instantiate from the JMH generated code,
            // or our user is a tricky bastard, and gets what's coming to him.
            if (el.getMethodName().startsWith("_jmh_tryInit_") &&
                    el.getClassName().contains("generated"))
                return;
        }
        throw iae;
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
        int tlr = (this.tlr = (this.tlr * 1664525 + 1013904223));
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.obj1 = obj;
            this.tlrMask = (this.tlrMask << 1) + 1;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param objs objects to consume.
     */
    public final void consume(Object[] objs) {
        int tlr = (this.tlr = (this.tlr * 1664525 + 1013904223));
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.objs1 = objs;
            this.tlrMask = (tlrMask << 1) + 1;
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

    private static volatile long consumedCPU = System.nanoTime();

    /**
     * Consume some amount of time tokens.
     *
     * This method does the CPU work almost linear to the number of tokens.
     * The token cost may vary from system to system, and may change in
     * future. (Translation: it is as reliable as we can get, but not absolutely
     * reliable).
     *
     * See JMH samples for the complete demo, and core benchmarks for
     * the performance assessments.
     *
     * @param tokens CPU tokens to consume
     */
    public static void consumeCPU(long tokens) {
        // If you are looking at this code trying to understand
        // the non-linearity on low token counts, know this:
        // we are pretty sure the generated assembly for almost all
        // cases is the same, and the only explanation for the
        // performance difference is hardware-specific effects.
        // Be wary to waste more time on this. If you know more
        // advanced and clever option to implement consumeCPU, let us
        // know.

        // Randomize start so that JIT could not memoize; this helps
        // to break the loop optimizations if the method is called
        // from the external loop body.
        long t = consumedCPU;

        // One of the rare cases when counting backwards is meaningful:
        // for the forward loop HotSpot/x86 generates "cmp" with immediate
        // on the hot path, while the backward loop tests against zero
        // with "test". The immediate can have different lengths, which
        // attribute to different machine code for different cases. We
        // counter that with always counting backwards. We also mix the
        // induction variable in, so that reversing the loop is the
        // non-trivial optimization.
        for (long i = tokens; i > 0; i--) {
            t += (t * 0x5DEECE66DL + 0xBL + i) & (0xFFFFFFFFFFFFL);
        }

        // Need to guarantee side-effect on the result, but can't afford
        // contention; make sure we update the shared state only in the
        // unlikely case, so not to do the furious writes, but still
        // dodge DCE.
        if (t == 42) {
            consumedCPU += t;
        }
    }

}
