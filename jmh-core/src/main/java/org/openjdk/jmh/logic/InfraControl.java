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

import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The InfraControl logic class.
 * This is the rendezvous class for benchmark handler and JMH.
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com, aleksey.shipilev@oracle.com
 */
public class InfraControl extends InfraControlL4 {

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

    public InfraControl(int threads, boolean syncIterations, TimeValue loopTime, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration, TimeUnit timeUnit) {
        super(threads, syncIterations, loopTime, preSetup, preTearDown, lastIteration, timeUnit);
    }

    public void enableTimer() {
        timers.schedule(new Runnable() {
            @Override
            public void run() {
                isDone = true;
            }
        }, duration, TimeUnit.NANOSECONDS);
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
        return unit.convert(duration, TimeUnit.NANOSECONDS);
    }

    public void preSetup() {
        try {
            preSetup.countDown();
            preSetup.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void preTearDown() {
        try {
            preTearDown.countDown();
            preTearDown.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
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
}

class InfraControl_L1 {
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;
    public int p21, p22, p23, p24, p25, p26, p27, p28;
    public int p31, p32, p33, p34, p35, p36, p37, p38;
}

/**
 * @see BlackHole for rationale
 */
class InfraControlL2 extends InfraControl_L1 {
    /* Flag for if we are done or not.
     * This is specifically the public field, so to spare one virtual call.
     */
    public volatile boolean isDone;

    /** How long we should loop */
    public final long duration;

    public final CountDownLatch preSetup;
    public final CountDownLatch preTearDown;
    public final boolean lastIteration;
    public final TimeUnit timeUnit;
    public final int threads;
    public final boolean syncIterations;

    public final AtomicInteger warmupVisited, warmdownVisited;
    public volatile boolean warmupShouldWait, warmdownShouldWait;

    public InfraControlL2(int threads, boolean syncIterations, TimeValue loopTime, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration, TimeUnit timeUnit) {
        this.threads = threads;
        this.syncIterations = syncIterations;
        this.warmupVisited = new AtomicInteger();
        this.warmdownVisited = new AtomicInteger();

        warmupShouldWait = syncIterations;
        warmdownShouldWait = syncIterations;
        this.preSetup = preSetup;
        this.preTearDown = preTearDown;
        this.duration = loopTime.convertTo(TimeUnit.NANOSECONDS);
        this.lastIteration = lastIteration;
        this.timeUnit = timeUnit;
    }

    public void announceWarmupReady() {
        if (!syncIterations) return;
        int v = warmupVisited.incrementAndGet();
        if (v == threads) {
            warmupShouldWait = false;
        }

        if (v > threads) {
            throw new IllegalStateException("More threads than expected");
        }
    }

    public void announceWarmdownReady() {
        if (!syncIterations) return;
        int v = warmdownVisited.incrementAndGet();
        if (v == threads) {
            warmdownShouldWait = false;
        }

        if (v > threads) {
            throw new IllegalStateException("More threads than expected");
        }
    }

}

class InfraControlL3 extends InfraControlL2 {
    public int e01, e02, e03, e04, e05, e06, e07, e08;
    public int e11, e12, e13, e14, e15, e16, e17, e18;
    public int e21, e22, e23, e24, e25, e26, e27, e28;
    public int e31, e32, e33, e34, e35, e36, e37, e38;

    public InfraControlL3(int threads, boolean syncIterations, TimeValue loopTime, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration, TimeUnit timeUnit) {
        super(threads, syncIterations, loopTime, preSetup, preTearDown, lastIteration, timeUnit);
    }
}

class InfraControlL4 extends InfraControlL3 {
    public int marker;

    public InfraControlL4(int threads, boolean syncIterations, TimeValue loopTime, CountDownLatch preSetup, CountDownLatch preTearDown, boolean lastIteration, TimeUnit timeUnit) {
        super(threads, syncIterations, loopTime, preSetup, preTearDown, lastIteration, timeUnit);
    }
}

