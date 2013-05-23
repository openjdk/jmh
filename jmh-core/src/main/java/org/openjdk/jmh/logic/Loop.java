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

import org.openjdk.jmh.runner.Waiter;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The Loop logic class. Controls if we should iterate another lap in the benchmark loop via calls to done();
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com, aleksey.shipilev@oracle.com
 */
public class Loop {

    /** Timers */
    private static final ScheduledExecutorService timers = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setPriority(Thread.MAX_PRIORITY);
            t.setDaemon(true);
            t.setName("LoopTimer");
            return t;
        }
    });

    static class L1 {
        public int p01, p02, p03, p04, p05, p06, p07, p08;
        public int p11, p12, p13, p14, p15, p16, p17, p18;
        public int p21, p22, p23, p24, p25, p26, p27, p28;
        public int p31, p32, p33, p34, p35, p36, p37, p38;
    }

    /**
     * @see BlackHole for rationale
     */
    public static class Data extends L1 {
        /* Flag for if we are done or not.
         * This is specifically the public field, so to spare one virtual call.
         */
        public volatile boolean isDone;

        /** How long we should loop */
        public final long duration;
        /** Start timestamp */
        public long start;
        /** End timestamp */
        public long end;
        /** Start of pause */
        public long pauseStart;
        /** Total pause time */
        public long totalPause;

        public final Waiter warmupWaiter;
        public final Waiter warmdownWaiter;
        public final CountDownLatch preSetup;
        public final CountDownLatch preTearDown;
        public final boolean lastIteration;

        public Data(TimeValue loopTime, Waiter warmupWaiter, Waiter warmdownWaiter, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration) {
            this.warmupWaiter = warmupWaiter;
            this.warmdownWaiter = warmdownWaiter;
            this.preSetup = preSetup;
            this.preTearDown = preTearDown;
            this.duration = loopTime.convertTo(TimeUnit.NANOSECONDS);
            this.lastIteration = lastIteration;
        }

    }

    static class L3 extends Data {
        public int e01, e02, e03, e04, e05, e06, e07, e08;
        public int e11, e12, e13, e14, e15, e16, e17, e18;
        public int e21, e22, e23, e24, e25, e26, e27, e28;
        public int e31, e32, e33, e34, e35, e36, e37, e38;

        public L3(TimeValue loopTime, Waiter warmupWaiter, Waiter warmdownWaiter, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration) {
            super(loopTime, warmupWaiter, warmdownWaiter, preSetup, preTearDown, lastIteration);
        }
    }

    static class L4 extends L3 {
        public int marker;

        public L4(TimeValue loopTime, Waiter warmupWaiter, Waiter warmdownWaiter, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration) {
            super(loopTime, warmupWaiter, warmdownWaiter, preSetup, preTearDown, lastIteration);
        }
    }

    public final L4 data;

    /**
     * Constructor
     *
     * @param duration How long we should loop
     */
    public Loop(long duration) {
        this(new TimeValue(duration, TimeUnit.MILLISECONDS));
    }

    /**
     * Constructor
     *
     * @param loopTime How long we should loop
     */
    public Loop(TimeValue loopTime) {
        this(loopTime, null, null, null, null, false);
    }

    public Loop(TimeValue loopTime, Waiter warmupWaiter, Waiter warmdownWaiter, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration) {
        data = new L4(loopTime, warmupWaiter, warmdownWaiter, preSetup, preTearDown, lastIteration);
    }


    /** Start timer and record start time */
    @Deprecated // GMB generator emits the proper code instead
    public void start() {
        enable();

        assert data.start == 0;
        assert data.end == 0;
        data.start = System.nanoTime();
    }


    public void enable() {
        assert !data.isDone;
        data.isDone = false;

        timers.schedule(new Runnable() {
            @Override
            public void run() {
                data.isDone = true;
            }
        }, data.duration, TimeUnit.NANOSECONDS);
    }

    /**
     * Temporary pause the timing measurement, does NOT affect the iteration time for the loop completion
     * <p/>
     * To continue timing call resume()
     */
    @Deprecated // GMB generator emits the proper code instead
    public void pauseMeasurement() {
        assert data.pauseStart == 0;
        data.pauseStart = System.nanoTime();
    }

    /** Resume a paused timing */
    @Deprecated // GMB generator emits the proper code instead
    public void resumeMeasurement() {
        assert data.pauseStart != 0;
        data.totalPause += System.nanoTime() - data.pauseStart;
        data.pauseStart = 0;
    }

    /** End timer and record the end time */
    @Deprecated // GMB generator emits the proper code instead
    public void end() {
        assert data.isDone;
        data.end = System.nanoTime();
    }

    /**
     * Check if the timed duration has been completed
     *
     * @return if we are done
     */
    @Deprecated // GMB generator emits the proper code instead
    public boolean done() {
        assert data.start != 0;
        return data.isDone;
    }

    /**
     * The measured time a benchmark iteration took in nanoseconds
     * <p/>
     * If the measurement was paused and resumed during the benchmark the time spent between these calls are deducted
     * from the iteration time
     *
     * @return the time the iteration took
     */
    public long getTime() {
        assert data.start != 0;
        assert data.end != 0;
        assert data.isDone;
        return (data.end - data.start - data.totalPause);
    }

    /**
     * Get the total time spent with paused timing
     *
     * @return The total pause time in nanoseconds
     */
    public long getTotalPausetime() {
        assert data.start != 0;
        assert data.end != 0;
        assert data.isDone;
        return data.totalPause;
    }

    /**
     * Get the total time an iteration took without including any pauses
     *
     * @return The total iteration run time including pauses in nanoseconds
     */
    public long getTotalRuntime() {
        assert data.start != 0;
        assert data.end != 0;
        assert data.isDone;
        return (data.end - data.start);
    }

    /**
     * returns requested loop duration in milliseconds.
     * the primary purpose of the method - integration tests.
     * @return
     */
    public long getDuration() {
        return getDuration(TimeUnit.MILLISECONDS);
    }

    /**
     * returns requested loop duration in the requested unit.
     * the primary purpose of the method - integration tests.
     * @param unit
     * @return
     */
    public long getDuration(TimeUnit unit) {
        return unit.convert(data.duration, TimeUnit.NANOSECONDS);
    }

    public boolean shouldContinueWarmup() {
        return data.warmupWaiter.shouldWait();
    }

    public boolean shouldContinueWarmdown() {
        return data.warmdownWaiter.shouldWait();
    }

    public void announceWarmupReady() {
        data.warmupWaiter.announceReady();
    }

    public void announceWarmdownReady() {
        data.warmdownWaiter.announceReady();
    }

    public void preSetup() {
        try {
            data.preSetup.countDown();
            data.preSetup.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void preTearDown() {
        try {
            data.preTearDown.countDown();
            data.preTearDown.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isLastIteration() {
        return data.lastIteration;
    }
}
