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

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;
import org.openjdk.jmh.util.Utils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract runner, the base class for Runner and ForkedRunner.
 */
abstract class BaseRunner {

    private long projectedTotalTime;
    private long projectedRunningTime;
    private long actualRunningTime;
    private long benchmarkStart;

    protected final Options options;
    protected final OutputFormat out;

    public BaseRunner(Options options, OutputFormat handler) {
        if (options == null) {
            throw new IllegalArgumentException("Options is null.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler is null.");
        }
        this.options = options;
        this.out = handler;
    }

    protected Multimap<BenchmarkParams, BenchmarkResult> runBenchmarks(boolean forked, ActionPlan actionPlan) {
        Multimap<BenchmarkParams, BenchmarkResult> results = new TreeMultimap<BenchmarkParams, BenchmarkResult>();

        for (Action action : actionPlan.getActions()) {

            BenchmarkParams params = action.getParams();
            ActionMode mode = action.getMode();

            if (!forked) {
                etaBeforeBenchmark();
                out.startBenchmark(params);
                out.println("# Fork: N/A, test runs in the existing VM");
            }

            BenchmarkResult r = null;
            try {
                switch (mode) {
                    case WARMUP: {
                        runBenchmark(params);
                        out.println("");
                        break;
                    }
                    case WARMUP_MEASUREMENT:
                    case MEASUREMENT: {
                        r = runBenchmark(params);
                        results.put(params, r);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unknown mode: " + mode);

                }
            } catch (BenchmarkException be) {
                out.println("<failure>");
                out.println("");
                out.println(Utils.throwableToString(be.getCause()));
                out.println("");

                if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR)) {
                    throw be;
                }
            }

            if (!forked) {
                etaAfterBenchmark(params);
                out.endBenchmark(r);
            }
        }

        return results;
    }

    protected void etaAfterBenchmark(BenchmarkParams params) {
        long current = System.nanoTime();
        projectedRunningTime += estimateTimeSingleFork(params);
        actualRunningTime += (current - benchmarkStart);
        benchmarkStart = current;
    }

    protected void etaBeforeBenchmarks(Collection<ActionPlan> plans) {
        projectedTotalTime = 0;
        for (ActionPlan plan : plans) {
            for (Action act : plan.getActions()) {
                BenchmarkParams params = act.getParams();
                projectedTotalTime += (Math.max(1, params.getForks()) + params.getWarmupForks()) * estimateTimeSingleFork(params);
            }
        }
    }

    private long estimateTimeSingleFork(BenchmarkParams params) {
        IterationParams wp = params.getWarmup();
        IterationParams mp = params.getMeasurement();

        long estimatedTime;
        if (params.getMode() == Mode.SingleShotTime) {
            // No way to tell how long it will execute,
            // guess anything, and let ETA compensation to catch up.
            estimatedTime = (wp.getCount() + mp.getCount()) * TimeUnit.MILLISECONDS.toNanos(1);
        } else {
            estimatedTime =
                    (wp.getCount() * wp.getTime().convertTo(TimeUnit.NANOSECONDS) +
                     mp.getCount() * mp.getTime().convertTo(TimeUnit.NANOSECONDS));
        }
        return estimatedTime;
    }

    protected void etaBeforeBenchmark() {
        if (benchmarkStart == 0) {
            benchmarkStart = System.nanoTime();
        }

        long totalETA;
        double partsDone = 1.0D * projectedRunningTime / projectedTotalTime;
        if (partsDone != 0) {
            totalETA = (long) (actualRunningTime * (1.0D / partsDone - 1));
        } else {
            totalETA = projectedTotalTime;
        }

        out.println(String.format("# Run progress: %.2f%% complete, ETA %s", partsDone * 100, formatDuration(totalETA)));
    }

    protected void etaAfterBenchmarks() {
        out.println(String.format("# Run complete. Total time: %s", formatDuration(actualRunningTime)));
        out.println("");
    }

    private String formatDuration(long nanos) {
        long days = TimeUnit.NANOSECONDS.toDays(nanos);
        nanos -= days * TimeUnit.DAYS.toNanos(1);

        long hrs = TimeUnit.NANOSECONDS.toHours(nanos);
        nanos -= hrs * TimeUnit.HOURS.toNanos(1);

        long mins = TimeUnit.NANOSECONDS.toMinutes(nanos);
        nanos -= mins * TimeUnit.MINUTES.toNanos(1);

        long secs = TimeUnit.NANOSECONDS.toSeconds(nanos);

        return String.format("%s%02d:%02d:%02d", (days > 0) ? days + " days, " : "", hrs, mins, secs);
    }

    BenchmarkResult runBenchmark(BenchmarkParams benchParams) {
        BenchmarkHandler handler = null;
        try {
            String target = benchParams.generatedBenchmark();
            int lastDot = target.lastIndexOf('.');
            Class<?> clazz = ClassUtils.loadClass(target.substring(0, lastDot));
            Method method = BenchmarkHandlers.findBenchmarkMethod(clazz, target.substring(lastDot + 1));

            handler = BenchmarkHandlers.getInstance(out, clazz, method, benchParams, options);

            return runBenchmark(benchParams, handler);
        } catch (BenchmarkException be) {
            throw be;
        } catch (Throwable ex) {
            throw new BenchmarkException(ex);
        } finally {
            if (handler != null) {
                handler.shutdown();
            }
        }
    }

    protected BenchmarkResult runBenchmark(BenchmarkParams benchParams, BenchmarkHandler handler) {
        List<IterationResult> allResults = new ArrayList<IterationResult>();

        // warmup
        IterationParams wp = benchParams.getWarmup();
        for (int i = 1; i <= wp.getCount(); i++) {
            // will run system gc if we should
            if (runSystemGC()) {
                out.verbosePrintln("System.gc() executed");
            }

            out.iteration(benchParams, wp, i);
            boolean isLastIteration = (benchParams.getMeasurement().getCount() == 0);
            IterationResult iterData = handler.runIteration(benchParams, wp, isLastIteration);
            out.iterationResult(benchParams, wp, i, iterData);
        }

        // measurement
        IterationParams mp = benchParams.getMeasurement();
        for (int i = 1; i <= mp.getCount(); i++) {
            // will run system gc if we should
            if (runSystemGC()) {
                out.verbosePrintln("System.gc() executed");
            }

            // run benchmark iteration
            out.iteration(benchParams, mp, i);

            boolean isLastIteration = (i == mp.getCount());
            IterationResult iterData = handler.runIteration(benchParams, mp, isLastIteration);
            out.iterationResult(benchParams, mp, i, iterData);
            allResults.add(iterData);
        }

        if (!allResults.isEmpty()) {
            return new BenchmarkResult(allResults);
        } else {
            // should be ignored in the caller
            return null;
        }
    }

    /**
     * Execute System.gc() if we the System.gc option is set.
     *
     * @return true if we did
     */
    public boolean runSystemGC() {
        if (options.shouldDoGC().orElse(Defaults.DO_GC)) {
            List<GarbageCollectorMXBean> enabledBeans = new ArrayList<GarbageCollectorMXBean>();

            long beforeGcCount = 0;
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = bean.getCollectionCount();
                if (count != -1) {
                    enabledBeans.add(bean);
                }
            }

            for (GarbageCollectorMXBean bean : enabledBeans) {
                beforeGcCount += bean.getCollectionCount();
            }

            // this call is asynchronous, should check whether it completes
            System.gc();

            final int MAX_WAIT_MSEC = 20 * 1000;

            if (enabledBeans.isEmpty()) {
                out.println("WARNING: MXBeans can not report GC info. System.gc() invoked, pessimistically waiting " + MAX_WAIT_MSEC + " msecs");
                try {
                    TimeUnit.MILLISECONDS.sleep(MAX_WAIT_MSEC);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }

            long start = System.nanoTime();
            while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < MAX_WAIT_MSEC) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                long afterGcCount = 0;
                for (GarbageCollectorMXBean bean : enabledBeans) {
                    afterGcCount += bean.getCollectionCount();
                }

                if (afterGcCount > beforeGcCount) {
                    return true;
                }
            }

            out.println("WARNING: System.gc() was invoked but couldn't detect a GC occuring, is System.gc() disabled?");
            return false;
        }
        return false;
    }

}
