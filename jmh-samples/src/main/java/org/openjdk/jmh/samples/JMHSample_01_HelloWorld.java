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
package org.openjdk.jmh.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class JMHSample_01_HelloWorld {

    /*
     * This is our first benchmark method.
     *
     * The contract for the benchmark methods is very simple:
     * annotate it with @Benchmark, and you are set to go.
     * JMH will run the test by continuously calling this method, and measuring
     * the performance metrics for its execution.
     *
     * The method names are non-essential, it matters they are marked with
     * @Benchmark. You can have multiple benchmark methods
     * within the same class.
     *
     * Note: if the benchmark method never finishes, then JMH run never
     * finishes as well. If you throw the exception from the method body,
     * the JMH run ends abruptly for this benchmark, and JMH will run
     * the next benchmark down the list.
     *
     * Although this benchmark measures "nothing", it is the good showcase
     * for the overheads the infrastructure bear on the code you measure
     * in the method. There are no magical infrastructures which incur no
     * overhead, and it's important to know what are the infra overheads
     * you are dealing with. You might find this thought unfolded in future
     * examples by having the "baseline" measurements to compare against.
     */

    @Benchmark
    public void wellHelloThere() {
        // this method was intentionally left blank.
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You are expected to see the run with large number of iterations, and
     * very large throughput numbers. You can see that as the estimate of the
     * harness overheads per method call. In most of our measurements, it is
     * down to several cycles per call.
     *
     * a) Via command-line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar ".*JMHSample_01.*"
     *
     * JMH generates self-contained JARs, bundling JMH together with it.
     * The runtime options for the JMH are available with "-h":
     *    $ java -jar target/benchmarks.jar -h
     *
     * b) Via Java API:
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JMHSample_01_HelloWorld.class.getSimpleName() + ".*")
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
