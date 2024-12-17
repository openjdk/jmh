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

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Random;

/**
 * Black Hole.
 *
 * <p>Black hole "consumes" the values, conceiving no information to JIT whether the
 * value is actually used afterwards. This can save from the dead-code elimination
 * of the computations resulting in the given values.</p>
 */
public final class Blackhole extends BlackholeL2 {
    byte b3_00, b3_01, b3_02, b3_03, b3_04, b3_05, b3_06, b3_07, b3_08, b3_09, b3_0a, b3_0b, b3_0c, b3_0d, b3_0e, b3_0f;
    long b3_10, b3_11, b3_12, b3_13, b3_14, b3_15, b3_16, b3_17, b3_18, b3_19, b3_1a, b3_1b, b3_1c, b3_1d, b3_1e, b3_1f;
    long b3_20, b3_21, b3_22, b3_23, b3_24, b3_25, b3_26, b3_27, b3_28, b3_29, b3_2a, b3_2b, b3_2c, b3_2d;

    /*
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
     * There is an experimental compiler support for Blackholes that instructs compilers
     * to treat specific methods as blackholes: keeping their arguments alive. At some
     * point in the future, we hope to switch to that mode by default, thus greatly
     * simplifying the Blackhole code.
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

    private static final boolean COMPILER_BLACKHOLE;

    static {
        COMPILER_BLACKHOLE = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.getBoolean("compilerBlackholesEnabled");
            }
        });
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

        if (!challengeResponse.equals("Should not be calling this.")) {
            throw new IllegalStateException("Blackholes should not be instantiated directly.");
        }
    }

    /**
     * Make any consumed data begone.
     *
     * WARNING: This method should only be called by the infrastructure code, in clearly understood cases.
     * Even though it is public, it is not supposed to be called by users.
     *
     * @param challengeResponse arbitrary string
     */
    public void evaporate(String challengeResponse) {
        if (!challengeResponse.equals("Should not be calling this.")) {
            throw new IllegalStateException("Evaporate should not be called directly.");
        }
        obj1 = null;
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param obj object to consume.
     */
    public final void consume(Object obj) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(obj);
        } else {
            consumeFull(obj);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param b object to consume.
     */
    public final void consume(byte b) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(b);
        } else {
            consumeFull(b);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param bool object to consume.
     */
    public final void consume(boolean bool) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(bool);
        } else {
            consumeFull(bool);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param c object to consume.
     */
    public final void consume(char c) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(c);
        } else {
            consumeFull(c);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param s object to consume.
     */
    public final void consume(short s) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(s);
        } else {
            consumeFull(s);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param i object to consume.
     */
    public final void consume(int i) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(i);
        } else {
            consumeFull(i);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param l object to consume.
     */
    public final void consume(long l) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(l);
        } else {
            consumeFull(l);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param f object to consume.
     */
    public final void consume(float f) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(f);
        } else {
            consumeFull(f);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param d object to consume.
     */
    public final void consume(double d) {
        if (COMPILER_BLACKHOLE) {
            consumeCompiler(d);
        } else {
            consumeFull(d);
        }
    }

    // Compiler blackholes block: let compilers figure out how to deal with it.

    private static void consumeCompiler(boolean v) {}
    private static void consumeCompiler(byte v)    {}
    private static void consumeCompiler(short v)   {}
    private static void consumeCompiler(char v)    {}
    private static void consumeCompiler(int v)     {}
    private static void consumeCompiler(float v)   {}
    private static void consumeCompiler(double v)  {}
    private static void consumeCompiler(long v)    {}
    private static void consumeCompiler(Object v)  {}

    // Full blackholes block: confuse compilers to get blackholing effects.
    // See implementation comments at the top to understand what this code is doing.

    private void consumeFull(byte b) {
        byte b1 = this.b1; // volatile read
        byte b2 = this.b2;
        if ((b ^ b1) == (b ^ b2)) {
            // SHOULD NEVER HAPPEN
            nullBait.b1 = b; // implicit null pointer exception
        }
    }

    private void consumeFull(boolean bool) {
        boolean bool1 = this.bool1; // volatile read
        boolean bool2 = this.bool2;
        if ((bool ^ bool1) == (bool ^ bool2)) {
            // SHOULD NEVER HAPPEN
            nullBait.bool1 = bool; // implicit null pointer exception
        }
    }

    private void consumeFull(char c) {
        char c1 = this.c1; // volatile read
        char c2 = this.c2;
        if ((c ^ c1) == (c ^ c2)) {
            // SHOULD NEVER HAPPEN
            nullBait.c1 = c; // implicit null pointer exception
        }
    }

    private void consumeFull(short s) {
        short s1 = this.s1; // volatile read
        short s2 = this.s2;
        if ((s ^ s1) == (s ^ s2)) {
            // SHOULD NEVER HAPPEN
            nullBait.s1 = s; // implicit null pointer exception
        }
    }

    private void consumeFull(int i) {
        int i1 = this.i1; // volatile read
        int i2 = this.i2;
        if ((i ^ i1) == (i ^ i2)) {
            // SHOULD NEVER HAPPEN
            nullBait.i1 = i; // implicit null pointer exception
        }
    }

    private void consumeFull(long l) {
        long l1 = this.l1; // volatile read
        long l2 = this.l2;
        if ((l ^ l1) == (l ^ l2)) {
            // SHOULD NEVER HAPPEN
            nullBait.l1 = l; // implicit null pointer exception
        }
    }

    private void consumeFull(float f) {
        float f1 = this.f1; // volatile read
        float f2 = this.f2;
        if (f == f1 & f == f2) {
            // SHOULD NEVER HAPPEN
            nullBait.f1 = f; // implicit null pointer exception
        }
    }

    private void consumeFull(double d) {
        double d1 = this.d1; // volatile read
        double d2 = this.d2;
        if (d == d1 & d == d2) {
            // SHOULD NEVER HAPPEN
            nullBait.d1 = d; // implicit null pointer exception
        }
    }

    private void consumeFull(Object obj) {
        int tlrMask = this.tlrMask; // volatile read
        int tlr = (this.tlr = (this.tlr * 1664525 + 1013904223));
        if ((tlr & tlrMask) == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.obj1 = new WeakReference<>(obj);
            this.tlrMask = (tlrMask << 1) + 1;
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

abstract class BlackholeL1 {
    byte b1_00, b1_01, b1_02, b1_03, b1_04, b1_05, b1_06, b1_07, b1_08, b1_09, b1_0a, b1_0b, b1_0c, b1_0d, b1_0e, b1_0f;
    long b1_10, b1_11, b1_12, b1_13, b1_14, b1_15, b1_16, b1_17, b1_18, b1_19, b1_1a, b1_1b, b1_1c, b1_1d, b1_1e, b1_1f;
    long b1_20, b1_21, b1_22, b1_23, b1_24, b1_25, b1_26, b1_27, b1_28, b1_29, b1_2a, b1_2b, b1_2c, b1_2d;
}
