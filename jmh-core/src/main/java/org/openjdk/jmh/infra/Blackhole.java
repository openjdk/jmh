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

import org.openjdk.jmh.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Random;

/*
    See the rationale for BlackholeL1..BlackholeL4 classes below.
 */

abstract class BlackholeL0 {
    private int markerBegin;
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
    public volatile byte b1;
    public volatile boolean bool1;
    public volatile char c1;
    public volatile short s1;
    public volatile int i1;
    public volatile long l1;
    public volatile float f1;
    public volatile double d1;
    public byte b2;
    public boolean bool2;
    public char c2;
    public short s2;
    public int i2;
    public long l2;
    public float f2;
    public double d2;
    public volatile Object obj1;
    public volatile BlackholeL2 nullBait = null;
    public int tlr;
    public volatile int tlrMask;

    public BlackholeL2() {
        Random r = new Random(System.nanoTime());
        tlr = r.nextInt();
        tlrMask = 1;
        obj1 = new Object();

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
    private int markerEnd;
}

/**
 * Black Hole.
 *
 * <p>Black hole "consumes" the values, conceiving no information to JIT whether the
 * value is actually used afterwards. This can save from the dead-code elimination
 * of the computations resulting in the given values.</p>
 */
public final class Blackhole extends BlackholeL4 {

    /**
     * IMPLEMENTATION NOTES:
     *
     * The major things to dodge with Blackholes are:
     *
     *   a) Dead-code elimination: the arguments should be used on every call,
     *      so that compilers are unable to fold them into constants or
     *      otherwise optimize them away along with the computations resulted
     *      in them.
     *
     *   b) False sharing: reading/writing the state may disturb the cache
     *      lines. We need to isolate the critical fields to achieve tolerable
     *      performance.
     *
     *   c) Write wall: we need to ease off on writes as much as possible,
     *      since it disturbs the caches, pollutes the write buffers, etc.
     *      This may very well result in hitting the memory wall prematurely.
     *      Reading memory is fine as long as it is cacheable.
     *
     * To achieve these goals, we are piggybacking on several things in the
     * compilers:
     *
     *  1. Superclass fields are not reordered with the subclass' fields.
     *     No practical VM that we are aware of is doing this. It is unpractical,
     *     because if the superclass fields are at the different offsets in two
     *     subclasses, the VMs would then need to do the polymorphic access for
     *     the superclass fields.
     *
     *  2. Compilers are unable to predict the value of the volatile read.
     *     While the compilers can speculatively optimize until the relevant
     *     volatile write happens, it is unlikely to be practical to be able to stop
     *     all the threads the instant that write had happened.
     *
     *  3. Compilers' code motion usually respects data dependencies, and they would
     *     not normally schedule the consumer block before the code that generated
     *     a value.
     *
     *  4. Compilers are not doing aggressive inter-procedural optimizations,
     *     and/or break them when the target method is forced to be non-inlineable.
     *
     * Observation (1) allows us to "squash" the protected fields in the inheritance
     * hierarchy so that the padding in super- and sub-class are laid out right before
     * and right after the protected fields. We also pad with booleans so that dense
     * layout in superclass does not have the gap where runtime can fit the subclass field.
     *
     * Observation (2) allows us to compare the incoming primitive values against
     * the relevant volatile-guarded fields. The values in those guarded fields are
     * never changing, but due to (2), we should re-read the values again and again.
     * Also, observation (3) requires us to to use the incoming value in the computation,
     * thus anchoring the Blackhole code after the generating expression.
     *
     * Primitives are a bit hard, because we can't predict what values we
     * will be fed. But we can compare the incoming value with *two* distinct
     * known values, and both checks will never be true at the same time.
     * Note the bitwise AND in all the predicates: both to spare additional
     * branch, and also to provide more uniformity in the performance. Where possible,
     * we are using a specific code shape to force generating a single branch, e.g.
     * making compiler to evaluate the predicate in full, not speculate on components.
     *
     * Objects should normally abide the Java's referential semantics, i.e. the
     * incoming objects will never be equal to the distinct object we have, and
     * volatile read will break the speculation about what we compare with.
     * However, smart compilers may deduce that the distinct non-escaped object
     * on the other side is not equal to anything we have, and fold the comparison
     * to "false". We do inlined thread-local random to get those objects escaped
     * with infinitesimal probability. Then again, smart compilers may skip from
     * generating the slow path, and apply the previous logic to constant-fold
     * the condition to "false". We are warming up the slow-path in the beginning
     * to evade that effect. Some caution needs to be exercised not to retain the
     * captured objects forever: this is normally achieved by calling evaporate()
     * regularly, but we also additionally protect with retaining the object on
     * weak reference (contrary to phantom-ref, publishing object still has to
     * happen, because reference users might need to discover the object).
     *
     * Observation (4) provides us with an opportunity to create a safety net in case
     * either (1), (2) or (3) fails. This is why Blackhole methods are prohibited from
     * being inlined. This is treated specially in JMH runner code (see CompilerHints).
     * Conversely, both (1), (2), (3) are covering in case (4) fails. This provides
     * a defense in depth for Blackhole implementation, where a point failure is a
     * performance nuisance, but not a correctness catastrophe.
     *
     * In all cases, consumes do the volatile reads to have a consistent memory
     * semantics across all consume methods.
     *
     * An utmost caution should be exercised when changing the Blackhole code. Nominally,
     * the JMH Core Benchmarks should be run on multiple platforms (and their generated code
     * examined) to check the effects are still in place, and the overheads are not prohibitive.
     * Or, in other words:
     *
     * IMPLEMENTING AN EFFICIENT / CORRECT BLACKHOLE IS NOT A SIMPLE TASK YOU CAN
     * DO OVERNIGHT. IT REQUIRES A SIGNIFICANT JVM/COMPILER/PERFORMANCE EXPERTISE,
     * AND LOTS OF TIME OVER THAT. ADJUST YOUR PLANS ACCORDINGLY.
     */

    static {
        Utils.check(Blackhole.class, "b1", "b2");
        Utils.check(Blackhole.class, "bool1", "bool2");
        Utils.check(Blackhole.class, "c1", "c2");
        Utils.check(Blackhole.class, "s1", "s2");
        Utils.check(Blackhole.class, "i1", "i2");
        Utils.check(Blackhole.class, "l1", "l2");
        Utils.check(Blackhole.class, "f1", "f2");
        Utils.check(Blackhole.class, "d1", "d2");
        Utils.check(Blackhole.class, "obj1");
    }

    public Blackhole(String challengeResponse) {
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

        if (!challengeResponse.equals("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")) {
            throw new IllegalStateException("Blackholes should not be instantiated directly.");
        }
    }

    /**
     * Make any consumed data begone.
     *
     * WARNING: This method should only be called by the infrastructure code, in clearly understood cases.
     * Even though it is public, it is not supposed to be called by users.
     */
    public void evaporate(String challengeResponse) {
        if (!challengeResponse.equals("Yes, I am Stephen Hawking, and know a thing or two about black holes.")) {
            throw new IllegalStateException("Who are you?");
        }
        obj1 = null;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param obj object to consume.
     */
    public final void consume(Object obj) {
        int tlrMask = this.tlrMask; // volatile read
        int tlr = (this.tlr = (this.tlr * 1664525 + 1013904223));
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.obj1 = new WeakReference<>(obj);
            this.tlrMask = (tlrMask << 1) + 1;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param b object to consume.
     */
    public final void consume(byte b) {
        byte b1 = this.b1; // volatile read
        byte b2 = this.b2;
        if ((b ^ b1) == (b ^ b2)) {
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
        boolean bool1 = this.bool1; // volatile read
        boolean bool2 = this.bool2;
        if ((bool ^ bool1) == (bool ^ bool2)) {
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
        char c1 = this.c1; // volatile read
        char c2 = this.c2;
        if ((c ^ c1) == (c ^ c2)) {
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
        short s1 = this.s1; // volatile read
        short s2 = this.s2;
        if ((s ^ s1) == (s ^ s2)) {
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
        int i1 = this.i1; // volatile read
        int i2 = this.i2;
        if ((i ^ i1) == (i ^ i2)) {
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
        long l1 = this.l1; // volatile read
        long l2 = this.l2;
        if ((l ^ l1) == (l ^ l2)) {
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
        float f1 = this.f1; // volatile read
        float f2 = this.f2;
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
        double d1 = this.d1; // volatile read
        double d2 = this.d2;
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
