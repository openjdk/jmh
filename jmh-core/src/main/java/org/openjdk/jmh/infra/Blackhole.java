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
    byte b3_000, b3_001, b3_002, b3_003, b3_004, b3_005, b3_006, b3_007, b3_008, b3_009, b3_010, b3_011, b3_012, b3_013, b3_014, b3_015;
    byte b3_016, b3_017, b3_018, b3_019, b3_020, b3_021, b3_022, b3_023, b3_024, b3_025, b3_026, b3_027, b3_028, b3_029, b3_030, b3_031;
    byte b3_032, b3_033, b3_034, b3_035, b3_036, b3_037, b3_038, b3_039, b3_040, b3_041, b3_042, b3_043, b3_044, b3_045, b3_046, b3_047;
    byte b3_048, b3_049, b3_050, b3_051, b3_052, b3_053, b3_054, b3_055, b3_056, b3_057, b3_058, b3_059, b3_060, b3_061, b3_062, b3_063;
    byte b3_064, b3_065, b3_066, b3_067, b3_068, b3_069, b3_070, b3_071, b3_072, b3_073, b3_074, b3_075, b3_076, b3_077, b3_078, b3_079;
    byte b3_080, b3_081, b3_082, b3_083, b3_084, b3_085, b3_086, b3_087, b3_088, b3_089, b3_090, b3_091, b3_092, b3_093, b3_094, b3_095;
    byte b3_096, b3_097, b3_098, b3_099, b3_100, b3_101, b3_102, b3_103, b3_104, b3_105, b3_106, b3_107, b3_108, b3_109, b3_110, b3_111;
    byte b3_112, b3_113, b3_114, b3_115, b3_116, b3_117, b3_118, b3_119, b3_120, b3_121, b3_122, b3_123, b3_124, b3_125, b3_126, b3_127;
    byte b3_128, b3_129, b3_130, b3_131, b3_132, b3_133, b3_134, b3_135, b3_136, b3_137, b3_138, b3_139, b3_140, b3_141, b3_142, b3_143;
    byte b3_144, b3_145, b3_146, b3_147, b3_148, b3_149, b3_150, b3_151, b3_152, b3_153, b3_154, b3_155, b3_156, b3_157, b3_158, b3_159;
    byte b3_160, b3_161, b3_162, b3_163, b3_164, b3_165, b3_166, b3_167, b3_168, b3_169, b3_170, b3_171, b3_172, b3_173, b3_174, b3_175;
    byte b3_176, b3_177, b3_178, b3_179, b3_180, b3_181, b3_182, b3_183, b3_184, b3_185, b3_186, b3_187, b3_188, b3_189, b3_190, b3_191;
    byte b3_192, b3_193, b3_194, b3_195, b3_196, b3_197, b3_198, b3_199, b3_200, b3_201, b3_202, b3_203, b3_204, b3_205, b3_206, b3_207;
    byte b3_208, b3_209, b3_210, b3_211, b3_212, b3_213, b3_214, b3_215, b3_216, b3_217, b3_218, b3_219, b3_220, b3_221, b3_222, b3_223;
    byte b3_224, b3_225, b3_226, b3_227, b3_228, b3_229, b3_230, b3_231, b3_232, b3_233, b3_234, b3_235, b3_236, b3_237, b3_238, b3_239;
    byte b3_240, b3_241, b3_242, b3_243, b3_244, b3_245, b3_246, b3_247, b3_248, b3_249, b3_250, b3_251, b3_252, b3_253, b3_254, b3_255;

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
    byte b1_000, b1_001, b1_002, b1_003, b1_004, b1_005, b1_006, b1_007, b1_008, b1_009, b1_010, b1_011, b1_012, b1_013, b1_014, b1_015;
    byte b1_016, b1_017, b1_018, b1_019, b1_020, b1_021, b1_022, b1_023, b1_024, b1_025, b1_026, b1_027, b1_028, b1_029, b1_030, b1_031;
    byte b1_032, b1_033, b1_034, b1_035, b1_036, b1_037, b1_038, b1_039, b1_040, b1_041, b1_042, b1_043, b1_044, b1_045, b1_046, b1_047;
    byte b1_048, b1_049, b1_050, b1_051, b1_052, b1_053, b1_054, b1_055, b1_056, b1_057, b1_058, b1_059, b1_060, b1_061, b1_062, b1_063;
    byte b1_064, b1_065, b1_066, b1_067, b1_068, b1_069, b1_070, b1_071, b1_072, b1_073, b1_074, b1_075, b1_076, b1_077, b1_078, b1_079;
    byte b1_080, b1_081, b1_082, b1_083, b1_084, b1_085, b1_086, b1_087, b1_088, b1_089, b1_090, b1_091, b1_092, b1_093, b1_094, b1_095;
    byte b1_096, b1_097, b1_098, b1_099, b1_100, b1_101, b1_102, b1_103, b1_104, b1_105, b1_106, b1_107, b1_108, b1_109, b1_110, b1_111;
    byte b1_112, b1_113, b1_114, b1_115, b1_116, b1_117, b1_118, b1_119, b1_120, b1_121, b1_122, b1_123, b1_124, b1_125, b1_126, b1_127;
    byte b1_128, b1_129, b1_130, b1_131, b1_132, b1_133, b1_134, b1_135, b1_136, b1_137, b1_138, b1_139, b1_140, b1_141, b1_142, b1_143;
    byte b1_144, b1_145, b1_146, b1_147, b1_148, b1_149, b1_150, b1_151, b1_152, b1_153, b1_154, b1_155, b1_156, b1_157, b1_158, b1_159;
    byte b1_160, b1_161, b1_162, b1_163, b1_164, b1_165, b1_166, b1_167, b1_168, b1_169, b1_170, b1_171, b1_172, b1_173, b1_174, b1_175;
    byte b1_176, b1_177, b1_178, b1_179, b1_180, b1_181, b1_182, b1_183, b1_184, b1_185, b1_186, b1_187, b1_188, b1_189, b1_190, b1_191;
    byte b1_192, b1_193, b1_194, b1_195, b1_196, b1_197, b1_198, b1_199, b1_200, b1_201, b1_202, b1_203, b1_204, b1_205, b1_206, b1_207;
    byte b1_208, b1_209, b1_210, b1_211, b1_212, b1_213, b1_214, b1_215, b1_216, b1_217, b1_218, b1_219, b1_220, b1_221, b1_222, b1_223;
    byte b1_224, b1_225, b1_226, b1_227, b1_228, b1_229, b1_230, b1_231, b1_232, b1_233, b1_234, b1_235, b1_236, b1_237, b1_238, b1_239;
    byte b1_240, b1_241, b1_242, b1_243, b1_244, b1_245, b1_246, b1_247, b1_248, b1_249, b1_250, b1_251, b1_252, b1_253, b1_254, b1_255;
}
