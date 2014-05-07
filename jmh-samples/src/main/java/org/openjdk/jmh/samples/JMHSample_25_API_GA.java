/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@State(Scope.Thread)
public class JMHSample_25_API_GA {

    /**
     * This example shows the rather convoluted, but fun way to exploit
     * JMH API in complex scenarios. Up to this point, we haven't consumed
     * the results programmatically, and hence we are missing all the fun.
     *
     * Let's consider this naive code, which obviously suffers from the
     * performance anomalies, since current HotSpot is resistant to make
     * the tail-call optimizations.
     */

    private int v;

    @GenerateMicroBenchmark
    public int test() {
        return veryImportantCode(1000, v);
    }

    public int veryImportantCode(int d, int v) {
        if (d == 0) {
            return v;
        } else {
            return veryImportantCode(d - 1, v);
        }
    }

    /*
     * We could probably make up for the absence of TCO with better inlining
     * policy. But hand-tuning the policy requires knowing a lot about VM
     * internals. Let's instead construct the layman's genetic algorithm
     * which sifts through inlining settings trying to find the better policy.
     *
     * If you are not familiar with the concept of Genetic Algorithms,
     * read the Wikipedia article first:
     *    http://en.wikipedia.org/wiki/Genetic_algorithm
     *
     * VM experts can guess which option should be tuned to get the max
     * performance. Try to run the sample and see if it improves performance.
     */

    public static void main(String[] args) throws RunnerException {
        // These are our base options. We will mix these options into the
        // measurement runs. That is, all measurement runs will inherit these,
        // see how it's done below.
        Options baseOpts = new OptionsBuilder()
                .include(".*" + JMHSample_25_API_GA.class.getName() + ".*")
                .warmupTime(TimeValue.milliseconds(200))
                .measurementTime(TimeValue.milliseconds(200))
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .verbosity(VerboseMode.SILENT)
                .build();

        // Initial population
        Population pop = new Population();
        final int POPULATION = 10;
        for (int c = 0; c < POPULATION; c++) {
            pop.addChromosome(new Chromosome(baseOpts));
        }

        // Make a few rounds of optimization:
        final int GENERATIONS = 100;
        for (int g = 0; g < GENERATIONS; g++) {
            System.out.println("Entering generation " + g);

            // Get the baseline score.
            // We opt to remeasure it in order to get reliable current estimate.
            RunResult runner = new Runner(baseOpts).runSingle();
            Result baseResult = runner.getPrimaryResult();

            // Printing a nice table...
            System.out.println("---------------------------------------");
            System.out.printf("Baseline score: %10.2f %s%n",
                    baseResult.getScore(),
                    baseResult.getScoreUnit()
            );

            for (Chromosome c : pop.getAll()) {
                System.out.printf("%10.2f %s (%+10.2f%%) %s%n",
                        c.getScore(),
                        baseResult.getScoreUnit(),
                        (c.getScore() / baseResult.getScore() - 1) * 100,
                        c.toString()
                );
            }
            System.out.println();

            Population newPop = new Population();

            // Copy out elite solutions
            final int ELITE = 2;
            for (Chromosome c : pop.getAll().subList(0, ELITE)) {
                newPop.addChromosome(c);
            }

            // Cross-breed the rest of new population
            while (newPop.size() < pop.size()) {
                Chromosome p1 = pop.selectToBreed();
                Chromosome p2 = pop.selectToBreed();

                newPop.addChromosome(p1.crossover(p2).mutate());
                newPop.addChromosome(p2.crossover(p1).mutate());
            }

            pop = newPop;
        }

    }

    /**
     * Population.
     */
    public static class Population {
        private final List<Chromosome> list = new ArrayList<Chromosome>();

        public void addChromosome(Chromosome c) {
            list.add(c);
            Collections.sort(list);
        }

        /**
         * Select the breeding material.
         * Solutions with better score have better chance to be selected.
         * @return breed
         */
        public Chromosome selectToBreed() {
            double totalScore = 0D;
            for (Chromosome c : list) {
                totalScore += c.score();
            }

            double thresh = Math.random() * totalScore;
            for (Chromosome c : list) {
                if (thresh < 0) return c;
                thresh =- c.score();
            }

            throw new IllegalStateException("Can not choose");
        }

        public int size() {
            return list.size();
        }

        public List<Chromosome> getAll() {
            return list;
        }
    }

    /**
     * Chromosome: encodes solution.
     */
    public static class Chromosome implements Comparable<Chromosome> {

        // Current score is not yet computed.
        double score = Double.NEGATIVE_INFINITY;

        // Base options to mix in
        final Options baseOpts;

        // These are current HotSpot defaults.
        int freqInlineSize = 325;
        int inlineSmallCode = 1000;
        int maxInlineLevel = 9;
        int maxInlineSize = 35;
        int maxRecursiveInlineLevel = 1;
        int minInliningThreshold = 250;

        public Chromosome(Options baseOpts) {
            this.baseOpts = baseOpts;
        }

        public double score() {
            if (score != Double.NEGATIVE_INFINITY) {
                // Already got the score, shortcutting
                return score;
            }

            try {
                // Add the options encoded by this solution:
                //  a) Mix in base options.
                //  b) Add JVM arguments: we opt to parse the
                //     stringly representation to make the example
                //     shorter. There are, of course, cleaner ways
                //     to do this.
                Options theseOpts = new OptionsBuilder()
                        .parent(baseOpts)
                        .jvmArgs(toString().split("[ ]"))
                        .build();

                // Run through JMH and get the result back.
                RunResult runResult = new Runner(theseOpts).runSingle();
                score = runResult.getPrimaryResult().getScore();
            } catch (RunnerException e) {
                // Something went wrong, the solution is defective
                score = Double.MIN_VALUE;
            }

            return score;
        }

        @Override
        public int compareTo(Chromosome o) {
            // Order by score, descending.
            return -Double.valueOf(score()).compareTo(o.score());
        }

        @Override
        public String toString() {
            return "-XX:FreqInlineSize=" + freqInlineSize +
                    " -XX:InlineSmallCode=" + inlineSmallCode +
                    " -XX:MaxInlineLevel=" + maxInlineLevel +
                    " -XX:MaxInlineSize=" + maxInlineSize +
                    " -XX:MaxRecursiveInlineLevel=" + maxRecursiveInlineLevel +
                    " -XX:MinInliningThreshold=" + minInliningThreshold;
        }

        public Chromosome crossover(Chromosome other) {
            // Perform crossover:
            // While this is a very naive way to perform crossover, it still works.

            final double CROSSOVER_PROB = 0.1;

            Chromosome result = new Chromosome(baseOpts);

            result.freqInlineSize = (Math.random() < CROSSOVER_PROB) ?
                    this.freqInlineSize : other.freqInlineSize;

            result.inlineSmallCode = (Math.random() < CROSSOVER_PROB) ?
                    this.inlineSmallCode : other.inlineSmallCode;

            result.maxInlineLevel = (Math.random() < CROSSOVER_PROB) ?
                    this.maxInlineLevel : other.maxInlineLevel;

            result.maxInlineSize = (Math.random() < CROSSOVER_PROB) ?
                    this.maxInlineSize : other.maxInlineSize;

            result.maxRecursiveInlineLevel = (Math.random() < CROSSOVER_PROB) ?
                    this.maxRecursiveInlineLevel : other.maxRecursiveInlineLevel;

            result.minInliningThreshold = (Math.random() < CROSSOVER_PROB) ?
                    this.minInliningThreshold : other.minInliningThreshold;

            return result;
        }

        public Chromosome mutate() {
            // Perform mutation:
            //  Again, this is a naive way to do mutation, but it still works.

            Chromosome result = new Chromosome(baseOpts);

            result.freqInlineSize = (int) randomChange(freqInlineSize);
            result.inlineSmallCode = (int) randomChange(inlineSmallCode);
            result.maxInlineLevel = (int) randomChange(maxInlineLevel);
            result.maxInlineSize = (int) randomChange(maxInlineSize);
            result.maxRecursiveInlineLevel = (int) randomChange(maxRecursiveInlineLevel);
            result.minInliningThreshold = (int) randomChange(minInliningThreshold);

            return result;
        }

        private double randomChange(double v) {
            final double MUTATE_PROB = 0.5;
            if (Math.random() < MUTATE_PROB) {
                if (Math.random() < 0.5) {
                    return v / (Math.random() * 2);
                } else {
                    return v * (Math.random() * 2);
                }
            } else {
                return v;
            }
        }

        public double getScore() {
            return score;
        }
    }

}
