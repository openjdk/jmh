/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmh.samples;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.ClassloaderProfiler;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JMHSample_35_Profilers {

    /*
     * This sample serves as the profiler overview.
     *
     * JMH has a few very handy profilers that help to understand your benchmarks. While
     * these profilers are not the substitute for full-fledged external profilers, in many
     * cases, these are handy to quickly dig into the benchmark behavior. When you are
     * doing many cycles of tuning up the benchmark code itself, it is important to have
     * a quick turnaround for the results.
     *
     * Use -lprof to list the profilers. There are quite a few profilers, and this sample
     * would expand on a handful of most useful ones. Many profilers have their own options,
     * usually accessible via -prof <profiler-name>:help.
     *
     * Since profilers are reporting on different things, it is hard to construct a single
     * benchmark sample that will show all profilers in action. Therefore, we have a couple
     * of benchmarks in this sample.
     */

    /*
     * ================================ MAPS BENCHMARK ================================
     */

    @State(Scope.Thread)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(3)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Maps {
        private Map<Integer, Integer> map;

        @Param({"hashmap", "treemap"})
        private String type;

        private int begin;
        private int end;

        @Setup
        public void setup() {
            switch (type) {
                case "hashmap":
                    map = new HashMap<>();
                    break;
                case "treemap":
                    map = new TreeMap<>();
                    break;
                default:
                    throw new IllegalStateException("Unknown type: " + type);
            }

            begin = 1;
            end = 256;
            for (int i = begin; i < end; i++) {
                map.put(i, i);
            }
        }

        @Benchmark
        public void test(Blackhole bh) {
            for (int i = begin; i < end; i++) {
                bh.consume(map.get(i));
            }
        }

        /*
         * ============================== HOW TO RUN THIS TEST: ====================================
         *
         * You can run this test:
         *
         * a) Via the command line:
         *    $ mvn clean install
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Maps -prof stack
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Maps -prof gc
         *
         * b) Via the Java API:
         *    (see the JMH homepage for possible caveats when running from IDE:
         *      http://openjdk.java.net/projects/code-tools/jmh/)
         */

        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(JMHSample_35_Profilers.Maps.class.getSimpleName())
                    .addProfiler(StackProfiler.class)
//                    .addProfiler(GCProfiler.class)
                    .build();

            new Runner(opt).run();
        }

        /*
            Running this benchmark will yield something like:

              Benchmark                              (type)  Mode  Cnt     Score    Error   Units
              JMHSample_35_Profilers.Maps.test     hashmap  avgt    5  1553.201 ±   6.199   ns/op
              JMHSample_35_Profilers.Maps.test     treemap  avgt    5  5177.065 ± 361.278   ns/op

            Running with -prof stack will yield:

              ....[Thread state: RUNNABLE]........................................................................
               99.0%  99.0% org.openjdk.jmh.samples.JMHSample_35_Profilers$Maps.test
                0.4%   0.4% org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Maps_test.test_avgt_jmhStub
                0.2%   0.2% sun.reflect.NativeMethodAccessorImpl.invoke0
                0.2%   0.2% java.lang.Integer.valueOf
                0.2%   0.2% sun.misc.Unsafe.compareAndSwapInt

              ....[Thread state: RUNNABLE]........................................................................
               78.0%  78.0% java.util.TreeMap.getEntry
               21.2%  21.2% org.openjdk.jmh.samples.JMHSample_35_Profilers$Maps.test
                0.4%   0.4% java.lang.Integer.valueOf
                0.2%   0.2% sun.reflect.NativeMethodAccessorImpl.invoke0
                0.2%   0.2% org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Maps_test.test_avgt_jmhStub

            Stack profiler is useful to quickly see if the code we are stressing actually executes. As many other
            sampling profilers, it is susceptible for sampling bias: it can fail to notice quickly executing methods,
            for example. In the benchmark above, it does not notice HashMap.get.

            Next up, GC profiler. Running with -prof gc will yield:

              Benchmark                                                            (type)  Mode  Cnt    Score     Error   Units

              JMHSample_35_Profilers.Maps.test                                   hashmap  avgt    5  1553.201 ±   6.199   ns/op
              JMHSample_35_Profilers.Maps.test:·gc.alloc.rate                    hashmap  avgt    5  1257.046 ±   5.675  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.alloc.rate.norm               hashmap  avgt    5  2048.001 ±   0.001    B/op
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Eden_Space           hashmap  avgt    5  1259.148 ± 315.277  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Eden_Space.norm      hashmap  avgt    5  2051.519 ± 520.324    B/op
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Survivor_Space       hashmap  avgt    5     0.175 ±   0.386  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Survivor_Space.norm  hashmap  avgt    5     0.285 ±   0.629    B/op
              JMHSample_35_Profilers.Maps.test:·gc.count                         hashmap  avgt    5    29.000            counts
              JMHSample_35_Profilers.Maps.test:·gc.time                          hashmap  avgt    5    16.000                ms

              JMHSample_35_Profilers.Maps.test                                   treemap  avgt    5  5177.065 ± 361.278   ns/op
              JMHSample_35_Profilers.Maps.test:·gc.alloc.rate                    treemap  avgt    5   377.251 ±  26.188  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.alloc.rate.norm               treemap  avgt    5  2048.003 ±   0.001    B/op
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Eden_Space           treemap  avgt    5   392.743 ± 174.156  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Eden_Space.norm      treemap  avgt    5  2131.767 ± 913.941    B/op
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Survivor_Space       treemap  avgt    5     0.131 ±   0.215  MB/sec
              JMHSample_35_Profilers.Maps.test:·gc.churn.PS_Survivor_Space.norm  treemap  avgt    5     0.709 ±   1.125    B/op
              JMHSample_35_Profilers.Maps.test:·gc.count                         treemap  avgt    5    25.000            counts
              JMHSample_35_Profilers.Maps.test:·gc.time                          treemap  avgt    5    26.000                ms

            There, we can see that the tests are producing quite some garbage. "gc.alloc" would say we are allocating 1257
            and 377 MB of objects per second, or 2048 bytes per benchmark operation. "gc.churn" would say that GC removes
            the same amount of garbage from Eden space every second. In other words, we are producing 2048 bytes of garbage per
            benchmark operation.

            If you look closely at the test, you can get a (correct) hypothesis this is due to Integer autoboxing.

            Note that "gc.alloc" counters generally produce more accurate data, but they can also fail when threads come and
            go over the course of the benchmark. "gc.churn" values are updated on each GC event, and so if you want a more accurate
            data, running longer and/or with small heap would help. But anyhow, always cross-reference "gc.alloc" and "gc.churn"
            values with each other to get a complete picture.

            It is also worth noticing that non-normalized counters are dependent on benchmark performance! Here, "treemap"
            tests are 3x slower, and thus both allocation and churn rates are also comparably lower. It is often useful to look
            into non-normalized counters to see if the test is allocation/GC-bound (figure the allocation pressure "ceiling"
            for your configuration!), and normalized counters to see the more precise benchmark behavior.

            As most profilers, both "stack" and "gc" profile are able to aggregate samples from multiple forks. It is a good
            idea to run multiple forks with the profilers enabled, as it improves results error estimates.
        */
    }

    /*
     * ================================ CLASSLOADER BENCHMARK ================================
     */


    @State(Scope.Thread)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(3)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Classy {

        /**
         * Our own crippled classloader, that can only load a simple class over and over again.
         */
        public static class XLoader extends URLClassLoader {
            private static final byte[] X_BYTECODE = new byte[]{
                    (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00, 0x00, 0x34, 0x00, 0x0D, 0x0A, 0x00, 0x03, 0x00,
                    0x0A, 0x07, 0x00, 0x0B, 0x07, 0x00, 0x0C, 0x01, 0x00, 0x06, 0x3C, 0x69, 0x6E, 0x69, 0x74, 0x3E, 0x01, 0x00, 0x03,
                    0x28, 0x29, 0x56, 0x01, 0x00, 0x04, 0x43, 0x6F, 0x64, 0x65, 0x01, 0x00, 0x0F, 0x4C, 0x69, 0x6E, 0x65, 0x4E, 0x75,
                    0x6D, 0x62, 0x65, 0x72, 0x54, 0x61, 0x62, 0x6C, 0x65, 0x01, 0x00, 0x0A, 0x53, 0x6F, 0x75, 0x72, 0x63, 0x65, 0x46,
                    0x69, 0x6C, 0x65, 0x01, 0x00, 0x06, 0x58, 0x2E, 0x6A, 0x61, 0x76, 0x61, 0x0C, 0x00, 0x04, 0x00, 0x05, 0x01, 0x00,
                    0x01, 0x58, 0x01, 0x00, 0x10, 0x6A, 0x61, 0x76, 0x61, 0x2F, 0x6C, 0x61, 0x6E, 0x67, 0x2F, 0x4F, 0x62, 0x6A, 0x65,
                    0x63, 0x74, 0x00, 0x20, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00,
                    0x05, 0x00, 0x01, 0x00, 0x06, 0x00, 0x00, 0x00, 0x1D, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x05, 0x2A,
                    (byte) 0xB7, 0x00, 0x01, (byte) 0xB1, 0x00, 0x00, 0x00, 0x01, 0x00, 0x07, 0x00, 0x00, 0x00, 0x06, 0x00, 0x01, 0x00,
                    0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x08, 0x00, 0x00, 0x00, 0x02, 0x00, 0x09,
            };

            public XLoader() {
                super(new URL[0], ClassLoader.getSystemClassLoader());
            }

            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                return defineClass(name, X_BYTECODE, 0, X_BYTECODE.length);
            }

        }

        @Benchmark
        public Class<?> load() throws ClassNotFoundException {
            return Class.forName("X", true, new XLoader());
        }

        /*
         * ============================== HOW TO RUN THIS TEST: ====================================
         *
         * You can run this test:
         *
         * a) Via the command line:
         *    $ mvn clean install
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Classy -prof cl
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Classy -prof comp
         *
         * b) Via the Java API:
         *    (see the JMH homepage for possible caveats when running from IDE:
         *      http://openjdk.java.net/projects/code-tools/jmh/)
         */

        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(JMHSample_35_Profilers.Classy.class.getSimpleName())
                    .addProfiler(ClassloaderProfiler.class)
//                    .addProfiler(CompilerProfiler.class)
                    .build();

            new Runner(opt).run();
        }

        /*
            Running with -prof cl will yield:

                Benchmark                                              Mode  Cnt      Score      Error        Units
                JMHSample_35_Profilers.Classy.load                     avgt   15  34215.363 ±  545.892        ns/op
                JMHSample_35_Profilers.Classy.load:·class.load         avgt   15  29374.097 ±  716.743  classes/sec
                JMHSample_35_Profilers.Classy.load:·class.load.norm    avgt   15      1.000 ±    0.001   classes/op
                JMHSample_35_Profilers.Classy.load:·class.unload       avgt   15  29598.233 ± 3420.181  classes/sec
                JMHSample_35_Profilers.Classy.load:·class.unload.norm  avgt   15      1.008 ±    0.119   classes/op

            Here, we can see the benchmark indeed load class per benchmark op, and this adds up to more than 29K classloads
            per second. We can also see the runtime is able to successfully keep the number of loaded classes at bay,
            since the class unloading happens at the same rate.

            This profiler is handy when doing the classloading performance work, because it says if the classes
            were actually loaded, and not reused across the Class.forName calls. It also helps to see if the benchmark
            performs any classloading in the measurement phase. For example, if you have non-classloading benchmark,
            you would expect these metrics be zero.

            Another useful profiler that could tell if compiler is doing a heavy work in background, and thus interfering
            with measurement, -prof comp:

                Benchmark                                                   Mode  Cnt      Score      Error  Units
                JMHSample_35_Profilers.Classy.load                          avgt    5  33523.875 ± 3026.025  ns/op
                JMHSample_35_Profilers.Classy.load:·compiler.time.profiled  avgt    5      5.000                ms
                JMHSample_35_Profilers.Classy.load:·compiler.time.total     avgt    5    479.000                ms

            We seem to be at proper steady state: out of 479 ms of total compiler work, only 5 ms happen during the
            measurement window. It is expected to have some level of background compilation even at steady state.

            As most profilers, both "cl" and "comp" are able to aggregate samples from multiple forks. It is a good
            idea to run multiple forks with the profilers enabled, as it improves results error estimates.
         */
    }

    /*
     * ================================ ATOMIC LONG BENCHMARK ================================
     */

    @State(Scope.Benchmark)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Atomic {
        private AtomicLong n;

        @Setup
        public void setup() {
            n = new AtomicLong();
        }

        @Benchmark
        public long test() {
            return n.incrementAndGet();
        }

        /*
         * ============================== HOW TO RUN THIS TEST: ====================================
         *
         * You can run this test:
         *
         * a) Via the command line:
         *    $ mvn clean install
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Atomic -prof perf     -f 1 (Linux)
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Atomic -prof perfnorm -f 3 (Linux)
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Atomic -prof perfasm  -f 1 (Linux)
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Atomic -prof xperfasm -f 1 (Windows)
         *
         * b) Via the Java API:
         *    (see the JMH homepage for possible caveats when running from IDE:
         *      http://openjdk.java.net/projects/code-tools/jmh/)
         */

        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(JMHSample_35_Profilers.Atomic.class.getSimpleName())
                    .addProfiler(LinuxPerfProfiler.class)
//                    .addProfiler(LinuxPerfNormProfiler.class)
//                    .addProfiler(LinuxPerfAsmProfiler.class)
//                    .addProfiler(WinPerfAsmProfiler.class)
                    .build();

            new Runner(opt).run();
        }

        /*
            Dealing with nanobenchmarks like these requires looking into the abyss of runtime, hardware, and
            generated code. Luckily, JMH has a few handy tools that ease the pain. If you are running Linux,
            then perf_events are probably available as standard package. This kernel facility taps into
            hardware counters, and provides the data for user space programs like JMH. Windows has less
            sophisticated facilities, but also usable, see below.

            One can simply run "perf stat java -jar ..." to get the first idea how the workload behaves. In
            JMH case, however, this will cause perf to profile both host and forked JVMs.

            -prof perf avoids that: JMH invokes perf for the forked VM alone. For the benchmark above, it
            would print something like:

                 Perf stats:
                                --------------------------------------------------

                       4172.776137 task-clock (msec)         #    0.411 CPUs utilized
                               612 context-switches          #    0.147 K/sec
                                31 cpu-migrations            #    0.007 K/sec
                               195 page-faults               #    0.047 K/sec
                    16,599,643,026 cycles                    #    3.978 GHz                     [30.80%]
                   <not supported> stalled-cycles-frontend
                   <not supported> stalled-cycles-backend
                    17,815,084,879 instructions              #    1.07  insns per cycle         [38.49%]
                     3,813,373,583 branches                  #  913.870 M/sec                   [38.56%]
                         1,212,788 branch-misses             #    0.03% of all branches         [38.91%]
                     7,582,256,427 L1-dcache-loads           # 1817.077 M/sec                   [39.07%]
                           312,913 L1-dcache-load-misses     #    0.00% of all L1-dcache hits   [38.66%]
                            35,688 LLC-loads                 #    0.009 M/sec                   [32.58%]
                   <not supported> LLC-load-misses:HG
                   <not supported> L1-icache-loads:HG
                           161,436 L1-icache-load-misses:HG  #    0.00% of all L1-icache hits   [32.81%]
                     7,200,981,198 dTLB-loads:HG             # 1725.705 M/sec                   [32.68%]
                             3,360 dTLB-load-misses:HG       #    0.00% of all dTLB cache hits  [32.65%]
                           193,874 iTLB-loads:HG             #    0.046 M/sec                   [32.56%]
                             4,193 iTLB-load-misses:HG       #    2.16% of all iTLB cache hits  [32.44%]
                   <not supported> L1-dcache-prefetches:HG
                                 0 L1-dcache-prefetch-misses:HG #    0.000 K/sec                   [32.33%]

                      10.159432892 seconds time elapsed

            We can already see this benchmark goes with good IPC, does lots of loads and lots of stores,
            all of them are more or less fulfilled without misses. The data like this is not handy though:
            you would like to normalize the counters per benchmark op.

            This is exactly what -prof perfnorm does:

                Benchmark                                                   Mode  Cnt   Score    Error  Units
                JMHSample_35_Profilers.Atomic.test                          avgt   15   6.551 ±  0.023  ns/op
                JMHSample_35_Profilers.Atomic.test:·CPI                     avgt    3   0.933 ±  0.026   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-load-misses   avgt    3   0.001 ±  0.022   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-loads         avgt    3  12.267 ±  1.324   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-store-misses  avgt    3   0.001 ±  0.006   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-stores        avgt    3   4.090 ±  0.402   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-icache-load-misses   avgt    3   0.001 ±  0.011   #/op
                JMHSample_35_Profilers.Atomic.test:·LLC-loads               avgt    3   0.001 ±  0.004   #/op
                JMHSample_35_Profilers.Atomic.test:·LLC-stores              avgt    3  ≈ 10⁻⁴            #/op
                JMHSample_35_Profilers.Atomic.test:·branch-misses           avgt    3  ≈ 10⁻⁴            #/op
                JMHSample_35_Profilers.Atomic.test:·branches                avgt    3   6.152 ±  0.385   #/op
                JMHSample_35_Profilers.Atomic.test:·bus-cycles              avgt    3   0.670 ±  0.048   #/op
                JMHSample_35_Profilers.Atomic.test:·context-switches        avgt    3  ≈ 10⁻⁶            #/op
                JMHSample_35_Profilers.Atomic.test:·cpu-migrations          avgt    3  ≈ 10⁻⁷            #/op
                JMHSample_35_Profilers.Atomic.test:·cycles                  avgt    3  26.790 ±  1.393   #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-load-misses        avgt    3  ≈ 10⁻⁴            #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-loads              avgt    3  12.278 ±  0.277   #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-store-misses       avgt    3  ≈ 10⁻⁵            #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-stores             avgt    3   4.113 ±  0.437   #/op
                JMHSample_35_Profilers.Atomic.test:·iTLB-load-misses        avgt    3  ≈ 10⁻⁵            #/op
                JMHSample_35_Profilers.Atomic.test:·iTLB-loads              avgt    3   0.001 ±  0.034   #/op
                JMHSample_35_Profilers.Atomic.test:·instructions            avgt    3  28.729 ±  1.297   #/op
                JMHSample_35_Profilers.Atomic.test:·minor-faults            avgt    3  ≈ 10⁻⁷            #/op
                JMHSample_35_Profilers.Atomic.test:·page-faults             avgt    3  ≈ 10⁻⁷            #/op
                JMHSample_35_Profilers.Atomic.test:·ref-cycles              avgt    3  26.734 ±  2.081   #/op

            It is customary to trim the lines irrelevant to the particular benchmark. We show all of them here for
            completeness.

            We can see that the benchmark does ~12 loads per benchmark op, and about ~4 stores per op, most of
            them fitting in the cache. There are also ~6 branches per benchmark op, all are predicted as well.
            It is also easy to see the benchmark op takes ~28 instructions executed in ~27 cycles.

            The output would get more interesting when we run with more threads, say, -t 8:

                Benchmark                                                   Mode  Cnt    Score     Error  Units
                JMHSample_35_Profilers.Atomic.test                          avgt   15  143.595 ±   1.968  ns/op
                JMHSample_35_Profilers.Atomic.test:·CPI                     avgt    3   17.741 ±  28.761   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-load-misses   avgt    3    0.175 ±   0.406   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-loads         avgt    3   11.872 ±   0.786   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-store-misses  avgt    3    0.184 ±   0.505   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-dcache-stores        avgt    3    4.422 ±   0.561   #/op
                JMHSample_35_Profilers.Atomic.test:·L1-icache-load-misses   avgt    3    0.015 ±   0.083   #/op
                JMHSample_35_Profilers.Atomic.test:·LLC-loads               avgt    3    0.015 ±   0.128   #/op
                JMHSample_35_Profilers.Atomic.test:·LLC-stores              avgt    3    1.036 ±   0.045   #/op
                JMHSample_35_Profilers.Atomic.test:·branch-misses           avgt    3    0.224 ±   0.492   #/op
                JMHSample_35_Profilers.Atomic.test:·branches                avgt    3    6.524 ±   2.873   #/op
                JMHSample_35_Profilers.Atomic.test:·bus-cycles              avgt    3   13.475 ±  14.502   #/op
                JMHSample_35_Profilers.Atomic.test:·context-switches        avgt    3   ≈ 10⁻⁴             #/op
                JMHSample_35_Profilers.Atomic.test:·cpu-migrations          avgt    3   ≈ 10⁻⁶             #/op
                JMHSample_35_Profilers.Atomic.test:·cycles                  avgt    3  537.874 ± 595.723   #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-load-misses        avgt    3    0.001 ±   0.006   #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-loads              avgt    3   12.032 ±   2.430   #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-store-misses       avgt    3   ≈ 10⁻⁴             #/op
                JMHSample_35_Profilers.Atomic.test:·dTLB-stores             avgt    3    4.557 ±   0.948   #/op
                JMHSample_35_Profilers.Atomic.test:·iTLB-load-misses        avgt    3   ≈ 10⁻³             #/op
                JMHSample_35_Profilers.Atomic.test:·iTLB-loads              avgt    3    0.016 ±   0.052   #/op
                JMHSample_35_Profilers.Atomic.test:·instructions            avgt    3   30.367 ±  15.052   #/op
                JMHSample_35_Profilers.Atomic.test:·minor-faults            avgt    3   ≈ 10⁻⁵             #/op
                JMHSample_35_Profilers.Atomic.test:·page-faults             avgt    3   ≈ 10⁻⁵             #/op
                JMHSample_35_Profilers.Atomic.test:·ref-cycles              avgt    3  538.697 ± 590.183   #/op

            Note how this time the CPI is awfully high: 17 cycles per instruction! Indeed, we are making almost the
            same ~30 instructions, but now they take >530 cycles. Other counters highlight why: we now have cache
            misses on both loads and stores, on all levels of cache hierarchy. With a simple constant-footprint
            like ours, that's an indication of sharing problems. Indeed, our AtomicLong is heavily-contended
            with 8 threads.

            "perfnorm", again, can (and should!) be used with multiple forks, to properly estimate the metrics.

            The last, but not the least player on our field is -prof perfasm. It is important to follow up on
            generated code when dealing with fine-grained benchmarks. We could employ PrintAssembly to dump the
            generated code, but it will dump *all* the generated code, and figuring out what is related to our
            benchmark is a daunting task. But we have "perf" that can tell what program addresses are really hot!
            This enables us to contrast the assembly output.

            -prof perfasm would indeed contrast out the hottest loop in the generated code! It will also point
            fingers at "lock xadd" as the hottest instruction in our code. Hardware counters are not very precise
            about the instruction addresses, so sometimes they attribute the events to the adjacent code lines.

                Hottest code regions (>10.00% "cycles" events):
                ....[Hottest Region 1]..............................................................................
                 [0x7f1824f87c45:0x7f1824f87c79] in org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub

                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@29 (line 201)
                                                                                  ; implicit exception: dispatches to 0x00007f1824f87d21
                                    0x00007f1824f87c25: test   %r11d,%r11d
                                    0x00007f1824f87c28: jne    0x00007f1824f87cbd  ;*ifeq
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@32 (line 201)
                                    0x00007f1824f87c2e: mov    $0x1,%ebp
                                    0x00007f1824f87c33: nopw   0x0(%rax,%rax,1)
                                    0x00007f1824f87c3c: xchg   %ax,%ax            ;*aload
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@13 (line 199)
                                    0x00007f1824f87c40: mov    0x8(%rsp),%r10
                  0.00%             0x00007f1824f87c45: mov    0xc(%r10),%r11d    ;*getfield n
                                                                                  ; - org.openjdk.jmh.samples.JMHSample_35_Profilers$Atomic::test@1 (line 280)
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@16 (line 199)
                  0.19%    0.02%    0x00007f1824f87c49: test   %r11d,%r11d
                                    0x00007f1824f87c4c: je     0x00007f1824f87cad
                                    0x00007f1824f87c4e: mov    $0x1,%edx
                                    0x00007f1824f87c53: lock xadd %rdx,0x10(%r12,%r11,8)
                                                                                  ;*invokevirtual getAndAddLong
                                                                                  ; - java.util.concurrent.atomic.AtomicLong::incrementAndGet@8 (line 200)
                                                                                  ; - org.openjdk.jmh.samples.JMHSample_35_Profilers$Atomic::test@4 (line 280)
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@16 (line 199)
                 95.20%   95.06%    0x00007f1824f87c5a: add    $0x1,%rdx          ;*ladd
                                                                                  ; - java.util.concurrent.atomic.AtomicLong::incrementAndGet@12 (line 200)
                                                                                  ; - org.openjdk.jmh.samples.JMHSample_35_Profilers$Atomic::test@4 (line 280)
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@16 (line 199)
                  0.24%    0.00%    0x00007f1824f87c5e: mov    0x10(%rsp),%rsi
                                    0x00007f1824f87c63: callq  0x00007f1824e2b020  ; OopMap{[0]=Oop [8]=Oop [16]=Oop [24]=Oop off=232}
                                                                                  ;*invokevirtual consume
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@19 (line 199)
                                                                                  ;   {optimized virtual_call}
                  0.20%    0.01%    0x00007f1824f87c68: mov    0x18(%rsp),%r10
                                    0x00007f1824f87c6d: movzbl 0x94(%r10),%r11d   ;*getfield isDone
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@29 (line 201)
                  0.00%             0x00007f1824f87c75: add    $0x1,%rbp          ; OopMap{r10=Oop [0]=Oop [8]=Oop [16]=Oop [24]=Oop off=249}
                                                                                  ;*ifeq
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@32 (line 201)
                  0.20%    0.01%    0x00007f1824f87c79: test   %eax,0x15f36381(%rip)        # 0x00007f183aebe000
                                                                                  ;   {poll}
                                    0x00007f1824f87c7f: test   %r11d,%r11d
                                    0x00007f1824f87c82: je     0x00007f1824f87c40  ;*aload_2
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@35 (line 202)
                                    0x00007f1824f87c84: mov    $0x7f1839be4220,%r10
                                    0x00007f1824f87c8e: callq  *%r10              ;*invokestatic nanoTime
                                                                                  ; - org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub@36 (line 202)
                                    0x00007f1824f87c91: mov    (%rsp),%r10
                ....................................................................................................
                 96.03%   95.10%  <total for region 1>

            perfasm would also print the hottest methods to show if we indeed spending time in our benchmark. Most of the time,
            it can demangle VM and kernel symbols as well:

                ....[Hottest Methods (after inlining)]..............................................................
                 96.03%   95.10%  org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_avgt_jmhStub
                  0.73%    0.78%  org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Atomic_test::test_AverageTime
                  0.63%    0.00%  org.openjdk.jmh.infra.Blackhole::consume
                  0.23%    0.25%  native_write_msr_safe ([kernel.kallsyms])
                  0.09%    0.05%  _raw_spin_unlock ([kernel.kallsyms])
                  0.09%    0.00%  [unknown] (libpthread-2.19.so)
                  0.06%    0.07%  _raw_spin_lock ([kernel.kallsyms])
                  0.06%    0.04%  _raw_spin_unlock_irqrestore ([kernel.kallsyms])
                  0.06%    0.05%  _IO_fwrite (libc-2.19.so)
                  0.05%    0.03%  __srcu_read_lock; __srcu_read_unlock ([kernel.kallsyms])
                  0.04%    0.05%  _raw_spin_lock_irqsave ([kernel.kallsyms])
                  0.04%    0.06%  vfprintf (libc-2.19.so)
                  0.04%    0.01%  mutex_unlock ([kernel.kallsyms])
                  0.04%    0.01%  _nv014306rm ([nvidia])
                  0.04%    0.04%  rcu_eqs_enter_common.isra.47 ([kernel.kallsyms])
                  0.04%    0.02%  mutex_lock ([kernel.kallsyms])
                  0.03%    0.07%  __acct_update_integrals ([kernel.kallsyms])
                  0.03%    0.02%  fget_light ([kernel.kallsyms])
                  0.03%    0.01%  fput ([kernel.kallsyms])
                  0.03%    0.04%  rcu_eqs_exit_common.isra.48 ([kernel.kallsyms])
                  1.63%    2.26%  <...other 319 warm methods...>
                ....................................................................................................
                100.00%   98.97%  <totals>

                ....[Distribution by Area]..........................................................................
                 97.44%   95.99%  <generated code>
                  1.60%    2.42%  <native code in ([kernel.kallsyms])>
                  0.47%    0.78%  <native code in (libjvm.so)>
                  0.22%    0.29%  <native code in (libc-2.19.so)>
                  0.15%    0.07%  <native code in (libpthread-2.19.so)>
                  0.07%    0.38%  <native code in ([nvidia])>
                  0.05%    0.06%  <native code in (libhsdis-amd64.so)>
                  0.00%    0.00%  <native code in (nf_conntrack.ko)>
                  0.00%    0.00%  <native code in (hid.ko)>
                ....................................................................................................
                100.00%  100.00%  <totals>

            Since program addresses change from fork to fork, it does not make sense to run perfasm with more than
            a single fork.
        */
    }
}
