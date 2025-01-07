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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.infra.IterationParams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The InfraControl logic class.
 * This is the rendezvous class for benchmark handler and JMH.
 */
public final class InfraControl extends InfraControlL2 {
    byte b3_00, b3_01, b3_02, b3_03, b3_04, b3_05, b3_06, b3_07, b3_08, b3_09, b3_0a, b3_0b, b3_0c, b3_0d, b3_0e, b3_0f;
    long b3_10, b3_11, b3_12, b3_13, b3_14, b3_15, b3_16, b3_17, b3_18, b3_19, b3_1a, b3_1b, b3_1c, b3_1d, b3_1e, b3_1f;
    long b3_20, b3_21, b3_22, b3_23, b3_24, b3_25, b3_26, b3_27, b3_28, b3_29, b3_2a, b3_2b, b3_2c, b3_2d, b3_2e, b3_2f;

    public InfraControl(BenchmarkParams benchmarkParams, IterationParams iterationParams,
                        CountDownLatch preSetup, CountDownLatch preTearDown,
                        boolean firstIteration, boolean lastIteration,
                        boolean shouldYield,
                        Control notifyControl) {
        super(benchmarkParams, iterationParams, preSetup, preTearDown, firstIteration, lastIteration, shouldYield, notifyControl);
    }

    /**
     * @return requested loop duration in milliseconds.
     */
    public int getDurationMs() {
        long ms = getDuration(TimeUnit.MILLISECONDS);
        int ims = (int) ms;
        if (ms != ims) {
            throw new IllegalStateException("Integer truncation problem");
        }
        return ims;
    }

    /**
     * @param unit timeunit to use
     * @return requested loop duration in the requested unit.
     */
    public long getDuration(TimeUnit unit) {
        return iterationParams.getTime().convertTo(unit);
    }

    public void preSetup() {
        preSetup.countDown();

        while (true) {
            try {
                preSetup.await();
                return;
            } catch (InterruptedException e) {
                // Do not accept interrupts here.
            }
        }
    }

    public void preTearDown() {
        preTearDown.countDown();

        while (true) {
            try {
                preTearDown.await();
                return;
            } catch (InterruptedException e) {
                // Do not accept interrupts here.
            }
        }
    }

    public void preSetupForce() {
        preSetup.countDown();
    }

    public void preTearDownForce() {
        preTearDown.countDown();
    }

    public boolean isLastIteration() {
        return lastIteration;
    }

    public void announceDone() {
        isDone = true;
        notifyControl.stopMeasurement = true;
    }

}

abstract class InfraControlL2 extends InfraControlL1 {
    /**
     * Flag that checks for time expiration.
     * This is specifically the public field, so to spare one virtual call.
     */
    public volatile boolean isDone;

    /**
     * Flag that checks for failure experienced by any measurement thread.
     * This is specifically the public field, so to spare one virtual call.
     */
    public volatile boolean isFailing;

    public volatile boolean volatileSpoiler;

    public final CountDownLatch preSetup;
    public final CountDownLatch preTearDown;
    public final boolean firstIteration;
    public final boolean lastIteration;
    public final boolean shouldYield;

    public final AtomicInteger warmupVisited, warmdownVisited;
    public volatile boolean warmupShouldWait, warmdownShouldWait;
    public final CountDownLatch warmupDone, warmdownDone;

    public final BenchmarkParams benchmarkParams;
    public final IterationParams iterationParams;
    public final Control notifyControl;

    private final boolean shouldSynchIterations;
    private final int threads;

    public InfraControlL2(BenchmarkParams benchmarkParams, IterationParams iterationParams,
                          CountDownLatch preSetup, CountDownLatch preTearDown,
                          boolean firstIteration, boolean lastIteration,
                          boolean shouldYield,
                          Control notifyControl) {
        warmupVisited = new AtomicInteger();
        warmdownVisited = new AtomicInteger();

        warmupDone = new CountDownLatch(1);
        warmdownDone = new CountDownLatch(1);

        shouldSynchIterations = benchmarkParams.shouldSynchIterations();
        threads = benchmarkParams.getThreads();

        warmupShouldWait = shouldSynchIterations;
        warmdownShouldWait = shouldSynchIterations;

        this.notifyControl = notifyControl;

        this.preSetup = preSetup;
        this.preTearDown = preTearDown;
        this.firstIteration = firstIteration;
        this.lastIteration = lastIteration;
        this.shouldYield = shouldYield;

        this.benchmarkParams = benchmarkParams;
        this.iterationParams = iterationParams;
    }

    public void announceWarmupReady() {
        if (!shouldSynchIterations) return;
        int v = warmupVisited.incrementAndGet();

        if (v == threads) {
            warmupShouldWait = false;
            warmupDone.countDown();
        }

        if (v > threads) {
            throw new IllegalStateException("More threads than expected");
        }
    }

    public void announceWarmdownReady() {
        if (!shouldSynchIterations) return;
        int v = warmdownVisited.incrementAndGet();
        if (v == threads) {
            warmdownShouldWait = false;
            warmdownDone.countDown();
        }

        if (v > threads) {
            throw new IllegalStateException("More threads than expected");
        }
    }

    public void awaitWarmupReady() {
        if (warmupShouldWait) {
            try {
                warmupDone.await();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public void awaitWarmdownReady() {
        if (warmdownShouldWait) {
            try {
                warmdownDone.await();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public String getParam(String name) {
        String param = benchmarkParams.getParam(name);
        if (param == null) {
            throw new IllegalStateException("The value for the parameter \"" + name + "\" is not set.");
        }
        return param;
    }

}

abstract class InfraControlL1 {
    byte b1_00, b1_01, b1_02, b1_03, b1_04, b1_05, b1_06, b1_07, b1_08, b1_09, b1_0a, b1_0b, b1_0c, b1_0d, b1_0e, b1_0f;
    long b1_10, b1_11, b1_12, b1_13, b1_14, b1_15, b1_16, b1_17, b1_18, b1_19, b1_1a, b1_1b, b1_1c, b1_1d, b1_1e, b1_1f;
    long b1_20, b1_21, b1_22, b1_23, b1_24, b1_25, b1_26, b1_27, b1_28, b1_29, b1_2a, b1_2b, b1_2c, b1_2d, b1_2e, b1_2f;
}
