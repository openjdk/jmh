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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
public class JMHSample_05_StateFixtures {

    double x;

    /*
     * Since @State objects are kept around during the lifetime of the
     * benchmark, it helps to have the methods which do state housekeeping.
     * These are usual fixture methods, you are probably familiar with them from
     * JUnit and TestNG.
     *
     * Fixture methods make sense only on @State objects, and JMH will fail to
     * compile the test otherwise.
     *
     * As with the State, fixture methods are only called by those benchmark
     * threads which are using the state. That means you can operate in the
     * thread-local context, and (not) use synchronization as if you are
     * executing in the context of benchmark thread.
     *
     * Note: fixture methods can also work with static fields, although the
     * semantics of these operations fall back out of State scope, and obey
     * usual Java rules (i.e. one static field per class).
     */

    /*
     * Ok, let's prepare our benchmark:
     */

    @Setup
    public void prepare() {
        x = Math.PI;
    }

    /*
     * And, check the benchmark went fine afterwards:
     */

    @TearDown
    public void check() {
        assert x > Math.PI : "Nothing changed?";
    }

    /*
     * This method obviously does the right thing, incrementing the field x
     * in the benchmark state. check() will never fail this way, because
     * we are always guaranteed to have at least one benchmark call.
     */

    @Benchmark
    public void measureRight() {
        x++;
    }

    /*
     * This method, however, will fail the check(), because we deliberately
     * have the "typo", and increment only the local variable. This should
     * not pass the check, and JMH will fail the run.
     */

    @Benchmark
    public void measureWrong() {
        double x = 0;
        x++;
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can see measureRight() yields the result, and measureWrong() fires
     * the assert at the end of the run.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -ea -jar target/benchmarks.jar JMHSample_05 -f 1
     *    (we requested single fork; there are also other options, see -h)
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_05_StateFixtures.class.getSimpleName())
                .forks(1)
                .jvmArgs("-ea")
                .build();

        new Runner(opt).run();
    }

}
