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

import org.openjdk.jmh.logic.results.IterationResult;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.output.format.IterationType;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.parameters.BenchmarkParams;
import org.openjdk.jmh.runner.parameters.IterationParams;
import org.openjdk.jmh.util.ClassUtils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * abstract runner - base class for Runner & ForkedRunner
 *
 * @author sergey.kuksenko@oracle.com
 */

public abstract class BaseRunner {

    /** Class holding all our runtime options/arguments */
    protected final Options options;

    protected final OutputFormat out;

    public BaseRunner(Options options, OutputFormat handler) {
        this.options = options;
        this.out = handler;
    }

    RunResult runBenchmark(BenchmarkRecord benchmark, boolean doWarmup, boolean doMeasurement) {
        MicroBenchmarkHandler handler = null;
        try {
            Class<?> clazz = ClassUtils.loadClass(benchmark.generatedClass());
            Method method = MicroBenchmarkHandlers.findBenchmarkMethod(clazz, benchmark.generatedMethod());

            BenchmarkParams executionParams = BenchmarkParams.makeParams(options, benchmark, method, doWarmup, doMeasurement);
            handler = MicroBenchmarkHandlers.getInstance(out, benchmark, clazz, method, executionParams, options);

            return runBenchmark(executionParams, handler);
        } catch (Throwable ex) {
            out.exception(ex);
            if (this.options.shouldFailOnError()) {
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        } finally {
            if (handler != null) {
                handler.shutdown();
            }
        }
        // FIXME: Better handling here!
        return null;
    }

    protected RunResult runBenchmark(BenchmarkParams executionParams, MicroBenchmarkHandler handler) {
        List<IterationResult> allResults = new ArrayList<IterationResult>();

        out.startBenchmark(handler.getBenchmark(), executionParams, this.options.isVerbose());

        // warmup
        IterationParams wp = executionParams.getWarmup();
        for (int i = 1; i <= wp.getCount(); i++) {
            // will run system gc if we should
            if (runSystemGC()) {
                out.verbosePrintln("System.gc() executed");
            }

            out.iteration(handler.getBenchmark(), wp, i, IterationType.WARMUP);
            boolean isLastIteration = (executionParams.getIteration().getCount() == 0);
            IterationResult iterData = handler.runIteration(wp, isLastIteration);
            out.iterationResult(handler.getBenchmark(), wp, i, IterationType.WARMUP, iterData);
        }

        // measurement
        IterationParams mp = executionParams.getIteration();
        for (int i = 1; i <= mp.getCount(); i++) {
            // will run system gc if we should
            if (runSystemGC()) {
                out.verbosePrintln("System.gc() executed");
            }

            // run benchmark iteration
            out.iteration(handler.getBenchmark(), mp, i, IterationType.MEASUREMENT);

            boolean isLastIteration = (i == mp.getCount());
            IterationResult iterData = handler.runIteration(mp, isLastIteration);

            // might get an exception above, in which case the results list will be empty
            if (iterData.isResultsEmpty()) {
                out.println("WARNING: No results returned, benchmark payload threw exception?");
            } else {
                out.iterationResult(handler.getBenchmark(), mp, i, IterationType.MEASUREMENT, iterData);

                allResults.add(iterData);
            }
        }

        // only print end-of-run output if we have actual results
        if (!allResults.isEmpty()) {
            RunResult result = new RunResult(allResults);
            out.endBenchmark(handler.getBenchmark(), result);
            return result;
        } else {
            // FIXME: Better handling here!
            return null;
        }
    }


    /**
     * Execute System.gc() if we the System.gc option is set.
     *
     * @return true if we did
     */
    public boolean runSystemGC() {
        if (options.shouldDoGC()) {
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
            while (System.nanoTime() - start < MAX_WAIT_MSEC) {
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
