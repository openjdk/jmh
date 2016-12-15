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
package org.openjdk.jmh.generators.core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.SampleBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark generator.
 *
 * <p>Benchmark generator is the agnostic piece of code which generates
 * synthetic Java code for JMH benchmarks. {@link GeneratorSource} is
 * used to feed the generator with the required metadata.</p>
 */
public class BenchmarkGenerator {

    private static final String JMH_STUB_SUFFIX = "_jmhStub";

    private final Set<BenchmarkInfo> benchmarkInfos;
    private final CompilerControlPlugin compilerControl;
    private final Set<String> processedBenchmarks;
    private final BenchmarkGeneratorSession session;

    public BenchmarkGenerator() {
        benchmarkInfos = new HashSet<>();
        processedBenchmarks = new HashSet<>();
        compilerControl = new CompilerControlPlugin();
        session = new BenchmarkGeneratorSession();
    }

    /**
     * Execute the next phase of benchmark generation.
     * Multiple calls to this method are acceptable, even with the difference sources
     *
     * @param source      generator source to get the metadata from
     * @param destination generator destination to write the results to
     */
    public void generate(GeneratorSource source, GeneratorDestination destination) {
        try {
            // Build a Set of classes with a list of annotated methods
            Multimap<ClassInfo, MethodInfo> clazzes = buildAnnotatedSet(source);

            // Generate code for all found Classes and Methods
            for (ClassInfo clazz : clazzes.keys()) {
                if (!processedBenchmarks.add(clazz.getQualifiedName())) continue;
                try {
                    validateBenchmark(clazz, clazzes.get(clazz));
                    Collection<BenchmarkInfo> infos = makeBenchmarkInfo(clazz, clazzes.get(clazz));
                    for (BenchmarkInfo info : infos) {
                        generateClass(source, destination, clazz, info);
                    }
                    benchmarkInfos.addAll(infos);
                } catch (GenerationException ge) {
                    destination.printError(ge.getMessage(), ge.getElement());
                }
            }

            /*
             * JMH stubs should not be inlined to start the inlining budget from the hottest loop.
             * We would like to accurately track the things we do not want to inline, but
             * unfortunately the Hotspot's CompilerOracle is not scaling well with the number of compiler
             * commands. Therefore, in order to cut down the number of compiler commands, we opt to
             * blankly forbid the inlining all methods that look like JMH stubs.
             *
             * See: https://bugs.openjdk.java.net/browse/JDK-8057169
             */
            for (Mode mode : Mode.values()) {
                compilerControl.alwaysDontInline("*", "*_" + mode.shortLabel() + JMH_STUB_SUFFIX);
            }

            compilerControl.process(source, destination);
        } catch (Throwable t) {
            destination.printError("Annotation generator had thrown the exception.", t);
        }
    }

    /**
     * Finish generating the benchmarks.
     * Must be called at the end of generation.
     *
     * @param source source generator to use
     * @param destination generator destination to write the results to
     */
    public void complete(GeneratorSource source, GeneratorDestination destination) {
        compilerControl.finish(source, destination);

        // Processing completed, final round.
        // Collect all benchmark entries here
        Set<BenchmarkListEntry> entries = new HashSet<>();

        // Try to read the benchmark entries from the previous generator sessions.
        // Incremental compilation may add or remove @Benchmark entries. New entries
        // are discovered and added from the current compilation session. It is harder
        // to detect removed @Benchmark entries. To do so, we are overwriting all benchmark
        // records that belong to a current compilation unit.
        Multimap<String, BenchmarkListEntry> entriesByQName = new HashMultimap<>();
        try {
            for (String line : readBenchmarkList(destination)) {
                BenchmarkListEntry br = new BenchmarkListEntry(line);
                entries.add(br);
                entriesByQName.put(br.getUserClassQName(), br);
            }
        } catch (UnsupportedOperationException e) {
            destination.printError("Unable to read the existing benchmark list.", e);
        }

        // Generate new benchmark entries
        for (BenchmarkInfo info : benchmarkInfos) {
            try {
                MethodGroup group = info.methodGroup;
                for (Mode m : group.getModes()) {
                    BenchmarkListEntry br = new BenchmarkListEntry(
                            info.userClassQName,
                            info.generatedClassQName,
                            group.getName(),
                            m,
                            group.getTotalThreadCount(),
                            group.getGroupThreads(),
                            group.getGroupLabels(),
                            group.getWarmupIterations(),
                            group.getWarmupTime(),
                            group.getWarmupBatchSize(),
                            group.getMeasurementIterations(),
                            group.getMeasurementTime(),
                            group.getMeasurementBatchSize(),
                            group.getForks(),
                            group.getWarmupForks(),
                            group.getJvm(),
                            group.getJvmArgs(),
                            group.getJvmArgsPrepend(),
                            group.getJvmArgsAppend(),
                            group.getParams(),
                            group.getOutputTimeUnit(),
                            group.getOperationsPerInvocation(),
                            group.getTimeout()
                    );

                    if (entriesByQName.keys().contains(info.userClassQName)) {
                        destination.printNote("Benchmark entries for " + info.userClassQName + " already exist, overwriting");
                        entries.removeAll(entriesByQName.get(info.userClassQName));
                        entriesByQName.remove(info.userClassQName);
                    }

                    entries.add(br);
                }
            } catch (GenerationException ge) {
                destination.printError(ge.getMessage(), ge.getElement());
            }
        }

        writeBenchmarkList(destination, entries);
    }

    private Collection<String> readBenchmarkList(GeneratorDestination destination) {
        String list = BenchmarkList.BENCHMARK_LIST.substring(1);
        try (Reader reader = destination.getResource(list)) {
            return FileUtils.readAllLines(reader);
        } catch (IOException e) {
            // okay, move on
        }
        return Collections.emptyList();
    }

    private void writeBenchmarkList(GeneratorDestination destination, Collection<BenchmarkListEntry> entries) {
        String list = BenchmarkList.BENCHMARK_LIST.substring(1);
        try (PrintWriter writer = new PrintWriter(destination.newResource(list))) {
            // Write out the complete benchmark list
            for (BenchmarkListEntry entry : entries) {
                writer.println(entry.toLine());
            }
        } catch (IOException ex) {
            destination.printError("Error writing benchmark list", ex);
        }
    }

    /**
     * Build a set of Classes which has annotated methods in them
     *
     * @return for all methods annotated with $annotation, returns Map<holder-class, Set<method>>
     */
    private Multimap<ClassInfo, MethodInfo> buildAnnotatedSet(GeneratorSource source) {
        // Transitively close the hierarchy:
        //   If superclass has a @Benchmark method, then all subclasses also have it.
        //   We skip the generated classes, which we had probably generated during the previous rounds
        //   of processing. Abstract classes are of no interest for us either.

        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<>();
        for (ClassInfo currentClass : source.getClasses()) {
            if (currentClass.getQualifiedName().contains("generated")) continue;
            if (currentClass.isAbstract()) continue;

            ClassInfo walk = currentClass;
            do {
                for (MethodInfo mi : walk.getMethods()) {
                    Benchmark ann = mi.getAnnotation(Benchmark.class);
                    if (ann != null) {
                        result.put(currentClass, mi);
                    }
                }
            } while ((walk = walk.getSuperClass()) != null);
        }
        return result;
    }


    /**
     * Do basic benchmark validation.
     */
    private void validateBenchmark(ClassInfo clazz, Collection<MethodInfo> methods) {
        if (clazz.getPackageName().isEmpty()) {
            throw new GenerationException("Benchmark class should have package other than default.", clazz);
        }

        if (clazz.isFinal()) {
            throw new GenerationException("Benchmark classes should not be final.", clazz);
        }

        // validate all arguments are @State-s
        for (MethodInfo e : methods) {
            StateObjectHandler.validateStateArgs(e);
        }

        boolean explicitState = BenchmarkGeneratorUtils.getAnnSuper(clazz, State.class) != null;

        // validate if enclosing class is implicit @State
        if (explicitState) {
            StateObjectHandler.validateState(clazz);
        }

        // validate no @State cycles
        for (MethodInfo e : methods) {
            StateObjectHandler.validateNoCycles(e);
        }

        // validate against rogue fields
        if (!explicitState || clazz.isAbstract()) {
            for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(clazz)) {
                // allow static fields
                if (fi.isStatic()) continue;
                throw new GenerationException(
                        "Field \"" + fi.getName() + "\" is declared within " +
                                "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                "This can result in unspecified behavior, and prohibited.", fi);
            }
        }

        // validate rogue annotations on classes
        BenchmarkGeneratorUtils.checkAnnotations(clazz);

        // validate rogue annotations on fields
        for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(clazz)) {
            BenchmarkGeneratorUtils.checkAnnotations(fi);
        }

        // validate rogue annotations on methods
        for (MethodInfo mi : methods) {
            BenchmarkGeneratorUtils.checkAnnotations(mi);
        }

        // check modifiers
        for (MethodInfo m : methods) {
            if (!m.isPublic()) {
                throw new GenerationException("@" + Benchmark.class.getSimpleName() +
                        " method should be public.", m);
            }

            if (m.isAbstract()) {
                throw new GenerationException("@" + Benchmark.class.getSimpleName()
                        + " method can not be abstract.", m);
            }

            if (m.isSynchronized()) {
                State annState = BenchmarkGeneratorUtils.getAnnSuper(m, State.class);
                if (annState == null) {
                    throw new GenerationException("@" + Benchmark.class.getSimpleName()
                            + " method can only be synchronized if the enclosing class is annotated with "
                            + "@" + State.class.getSimpleName() + ".", m);
                } else {
                    if (m.isStatic() && annState.value() != Scope.Benchmark) {
                        throw new GenerationException("@" + Benchmark.class.getSimpleName()
                                + " method can only be static and synchronized if the enclosing class is annotated with "
                                + "@" + State.class.getSimpleName() + "(" + Scope.class.getSimpleName() + "." + Scope.Benchmark + ").", m);
                    }
                }
            }
        }

        // check annotations
        for (MethodInfo m : methods) {
            OperationsPerInvocation opi = BenchmarkGeneratorUtils.getAnnSuper(m, clazz, OperationsPerInvocation.class);
            if (opi != null && opi.value() < 1) {
                throw new GenerationException("The " + OperationsPerInvocation.class.getSimpleName() +
                        " needs to be greater than 0.", m);
            }
        }

        // validate @Group-s
        for (MethodInfo m : methods) {
            if (m.getAnnotation(Group.class) != null && m.getAnnotation(Threads.class) != null) {
                throw new GenerationException("@" + Threads.class.getSimpleName() + " annotation is placed within " +
                        "the benchmark method with @" + Group.class.getSimpleName() + " annotation. " +
                        "This has ambiguous behavioral effect, and prohibited. " +
                        "Did you mean @" + GroupThreads.class.getSimpleName() + " instead?",
                        m);
            }
        }
    }

    /**
     * validate benchmark info
     *
     * @param info benchmark info to validate
     */
    private void validateBenchmarkInfo(BenchmarkInfo info) {
        // check the @Group preconditions,
        // ban some of the surprising configurations
        //
        MethodGroup group = info.methodGroup;
        if (group.methods().size() == 1) {
            MethodInfo meth = group.methods().iterator().next();
            if (meth.getAnnotation(Group.class) == null) {
                for (ParameterInfo param : meth.getParameters()) {
                    State stateAnn = BenchmarkGeneratorUtils.getAnnSuper(param.getType(), State.class);
                    if (stateAnn != null && stateAnn.value() == Scope.Group) {
                        throw new GenerationException(
                                "Only @" + Group.class.getSimpleName() + " methods can reference @" + State.class.getSimpleName()
                                        + "(" + Scope.class.getSimpleName() + "." + Scope.Group + ") states.",
                                meth);
                    }
                }

                State stateAnn = BenchmarkGeneratorUtils.getAnnSuper(meth.getDeclaringClass(), State.class);
                if (stateAnn != null && stateAnn.value() == Scope.Group) {
                    throw new GenerationException(
                            "Only @" + Group.class.getSimpleName() + " methods can implicitly reference @" + State.class.getSimpleName()
                                    + "(" + Scope.class.getSimpleName() + "." + Scope.Group + ") states.",
                            meth);
                }
            }
        } else {
            for (MethodInfo m : group.methods()) {
                if (m.getAnnotation(Group.class) == null) {
                    throw new GenerationException(
                            "Internal error: multiple methods per @" + Group.class.getSimpleName()
                                    + ", but not all methods have @" + Group.class.getSimpleName(),
                            m);
                }
            }
        }
    }

    /**
     * Generate BenchmarkInfo for given class.
     * We will figure out method groups at this point.
     *
     *
     * @param clazz   holder class
     * @param methods annotated methods
     * @return BenchmarkInfo
     */
    private Collection<BenchmarkInfo> makeBenchmarkInfo(ClassInfo clazz, Collection<MethodInfo> methods) {
        Map<String, MethodGroup> result = new TreeMap<>();

        for (MethodInfo method : methods) {
            Group groupAnn = method.getAnnotation(Group.class);
            String groupName = (groupAnn != null) ? groupAnn.value() : method.getName();

            if (!BenchmarkGeneratorUtils.checkJavaIdentifier(groupName)) {
                throw new GenerationException("Group name should be the legal Java identifier.", method);
            }

            MethodGroup group = result.get(groupName);
            if (group == null) {
                group = new MethodGroup(clazz, groupName);
                result.put(groupName, group);
            }

            BenchmarkMode mbAn = BenchmarkGeneratorUtils.getAnnSuper(method, clazz, BenchmarkMode.class);
            if (mbAn != null) {
                group.addModes(mbAn.value());
            }

            group.addStrictFP(clazz.isStrictFP());
            group.addStrictFP(method.isStrictFP());
            group.addMethod(method, (method.getAnnotation(GroupThreads.class) != null) ? method.getAnnotation(GroupThreads.class).value() : 1);

            // Discovering @Params, part 1:
            //   For each parameter, walk the type hierarchy up to discover inherited @Param fields in @State objects.
            for (ParameterInfo pi : method.getParameters()) {
                BenchmarkGeneratorUtils.addParameterValuesToGroup(pi.getType(), group);
            }

            // Discovering @Params, part 2:
            //  Walk the type hierarchy up to discover inherited @Param fields for class.
            BenchmarkGeneratorUtils.addParameterValuesToGroup(clazz, group);
        }

        // enforce the default value
        for (MethodGroup group : result.values()) {
            if (group.getModes().isEmpty()) {
                group.addModes(Defaults.BENCHMARK_MODE);
            }
        }

        Collection<BenchmarkInfo> benchmarks = new ArrayList<>();
        for (MethodGroup group : result.values()) {
            String sourcePackage = clazz.getPackageName();
            String generatedPackageName = sourcePackage + ".generated";
            String generatedClassName = BenchmarkGeneratorUtils.getGeneratedName(clazz) + "_" + group.getName() + "_jmhTest";

            BenchmarkInfo info = new BenchmarkInfo(clazz.getQualifiedName(), generatedPackageName, generatedClassName, group);
            validateBenchmarkInfo(info);
            benchmarks.add(info);
        }

        return benchmarks;
    }

    /**
     * Create and generate Java code for a class and it's methods
     */
    private void generateClass(GeneratorSource source, GeneratorDestination destination, ClassInfo classInfo, BenchmarkInfo info) throws IOException {
        StateObjectHandler states = new StateObjectHandler(compilerControl);

        // bind all methods
        states.bindMethods(classInfo, info.methodGroup);

        // Create file and open an outputstream
        PrintWriter writer = new PrintWriter(destination.newClass(info.generatedClassQName), false);

        // Write package and imports
        writer.println("package " + info.generatedPackageName + ';');
        writer.println();

        generateImport(writer);
        states.addImports(writer);

        // Write class header
        writer.println("public final class " + info.generatedClassName + " {");
        writer.println();

        // generate padding
        Paddings.padding(writer);

        writer.println(ident(1) + "int startRndMask;");
        writer.println(ident(1) + "BenchmarkParams benchmarkParams;");
        writer.println(ident(1) + "IterationParams iterationParams;");
        writer.println(ident(1) + "ThreadParams threadParams;");
        writer.println(ident(1) + "Blackhole blackhole;");
        writer.println(ident(1) + "Control notifyControl;");

        // write all methods
        for (Mode benchmarkKind : Mode.values()) {
            if (benchmarkKind == Mode.All) continue;
            generateMethod(classInfo, benchmarkKind, writer, info.methodGroup, states);
        }

        // Write out state initializers
        for (String s : states.getStateInitializers()) {
            writer.println(ident(1) + s);
        }
        writer.println();

        // Write out the required fields
        for (String s : states.getFields()) {
            writer.println(ident(1) + s);
        }
        writer.println();

        // Write out the required objects
        states.writeStateOverrides(session, destination);

        // Finish class
        writer.println("}");
        writer.println();

        writer.close();
    }

    private void generateImport(PrintWriter writer) {
        Class<?>[] imports = new Class<?>[]{
                List.class, AtomicInteger.class,
                Collection.class, ArrayList.class,
                TimeUnit.class, CompilerControl.class,
                InfraControl.class, ThreadParams.class,
                BenchmarkTaskResult.class,
                Result.class, ThroughputResult.class, AverageTimeResult.class,
                SampleTimeResult.class, SingleShotResult.class, SampleBuffer.class,
                Mode.class, Fork.class, Measurement.class, Threads.class, Warmup.class,
                BenchmarkMode.class, RawResults.class, ResultRole.class,
                Field.class, BenchmarkParams.class, IterationParams.class,
                Blackhole.class, Control.class,
                ScalarResult.class, AggregationPolicy.class,
                FailureAssistException.class
        };

        for (Class<?> c : imports) {
            writer.println("import " + c.getName() + ';');
        }
        writer.println();
    }

    /**
     * Generate the method for a specific benchmark method
     */
    private void generateMethod(ClassInfo classInfo, Mode benchmarkKind, PrintWriter writer, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println();
        switch (benchmarkKind) {
            case Throughput:
                generateThroughput(classInfo, writer, benchmarkKind, methodGroup, states);
                break;
            case AverageTime:
                generateAverageTime(classInfo, writer, benchmarkKind, methodGroup, states);
                break;
            case SampleTime:
                generateSampleTime(classInfo, writer, benchmarkKind, methodGroup, states);
                break;
            case SingleShotTime:
                generateSingleShotTime(classInfo, writer, benchmarkKind, methodGroup, states);
                break;
            default:
                throw new AssertionError("Shouldn't be here");
        }
    }

    private void generateThroughput(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public BenchmarkTaskResult " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");
            writer.println(ident(3) + "RawResults res = new RawResults();");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(4) + "res.allOps++;");
            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.startMeasurement = true;");

            // measurement loop call
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX +
                    "(" + getStubArgs() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.stopMeasurement = true;");

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(5) + "res.allOps++;");
            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            // iteration prolog
            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "res.allOps += res.measuredOps;");

            /*
               Adjust the operation counts:
                  1) res.measuredOps counted the individual @Benchmark invocations. Therefore, we need
                     to adjust for opsPerInv (pretending each @Benchmark invocation counts as $opsPerInv ops);
                     and we need to adjust down for $batchSize (pretending we had the batched run, and $batchSize
                     @Benchmark invocations counted as single op);
                  2) res.allOps counted the individual @Benchmark invocations as well; the same reasoning applies.

               It's prudent to make the multiplication first to get more accuracy.
             */

            writer.println(ident(3) + "int batchSize = iterationParams.getBatchSize();");
            writer.println(ident(3) + "int opsPerInv = benchmarkParams.getOpsPerInvocation();");

            writer.println(ident(3) + "res.allOps *= opsPerInv;");
            writer.println(ident(3) + "res.allOps /= batchSize;");
            writer.println(ident(3) + "res.measuredOps *= opsPerInv;");
            writer.println(ident(3) + "res.measuredOps /= batchSize;");

            writer.println(ident(3) + "BenchmarkTaskResult results = new BenchmarkTaskResult(res.allOps, res.measuredOps);");
            if (isSingleMethod) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
            } else {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.PRIMARY, \"" + methodGroup.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
            }
            for (String res : states.getAuxResults(method, "ThroughputResult")) {
                writer.println(ident(3) + "results.add(" + res + ");");
            }

            methodEpilog(writer, methodGroup);

            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;

            compilerControl.defaultForceInline(method);

            writer.println(ident(1) + "public static" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName + "(" +
                    getStubTypeArgs() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println(ident(2) + "long operations = 0;");
            writer.println(ident(2) + "long realTime = 0;");
            writer.println(ident(2) + "result.startTime = System.nanoTime();");
            writer.println(ident(2) + "do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println(ident(3) + "operations++;");
            writer.println(ident(2) + "} while(!control.isDone);");
            writer.println(ident(2) + "result.stopTime = System.nanoTime();");
            writer.println(ident(2) + "result.realTime = realTime;");
            writer.println(ident(2) + "result.measuredOps = operations;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void addAuxCounters(PrintWriter writer, String resName, StateObjectHandler states, MethodInfo method) {
        for (String res : states.getAuxResults(method, resName)) {
            writer.println(ident(3) + "results.add(" + res + ");");
        }
    }

    private void generateAverageTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public BenchmarkTaskResult " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");
            writer.println(ident(3) + "RawResults res = new RawResults();");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(4) + "res.allOps++;");
            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.startMeasurement = true;");

            // measurement loop call
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX + "(" + getStubArgs() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.stopMeasurement = true;");

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(5) + "res.allOps++;");
            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "res.allOps += res.measuredOps;");

            /*
               Adjust the operation counts:
                  1) res.measuredOps counted the individual @Benchmark invocations. Therefore, we need
                     to adjust for opsPerInv (pretending each @Benchmark invocation counts as $opsPerInv ops);
                     and we need to adjust down for $batchSize (pretending we had the batched run, and $batchSize
                     @Benchmark invocations counted as single op)
                  2) res.measuredOps counted the individual @Benchmark invocations as well; the same reasoning applies.

               It's prudent to make the multiplication first to get more accuracy.
             */

            writer.println(ident(3) + "int batchSize = iterationParams.getBatchSize();");
            writer.println(ident(3) + "int opsPerInv = benchmarkParams.getOpsPerInvocation();");

            writer.println(ident(3) + "res.allOps *= opsPerInv;");
            writer.println(ident(3) + "res.allOps /= batchSize;");
            writer.println(ident(3) + "res.measuredOps *= opsPerInv;");
            writer.println(ident(3) + "res.measuredOps /= batchSize;");

            writer.println(ident(3) + "BenchmarkTaskResult results = new BenchmarkTaskResult(res.allOps, res.measuredOps);");
            if (isSingleMethod) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
            } else {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.PRIMARY, \"" + methodGroup.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit()));");
            }
            addAuxCounters(writer, "AverageTimeResult", states, method);

            methodEpilog(writer, methodGroup);

            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;
            compilerControl.defaultForceInline(method);

            writer.println(ident(1) + "public static" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName +
                    "(" + getStubTypeArgs() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println(ident(2) + "long operations = 0;");
            writer.println(ident(2) + "long realTime = 0;");
            writer.println(ident(2) + "result.startTime = System.nanoTime();");
            writer.println(ident(2) + "do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println(ident(3) + "operations++;");
            writer.println(ident(2) + "} while(!control.isDone);");
            writer.println(ident(2) + "result.stopTime = System.nanoTime();");
            writer.println(ident(2) + "result.realTime = realTime;");
            writer.println(ident(2) + "result.measuredOps = operations;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private String getStubArgs() {
        return "control, res, benchmarkParams, iterationParams, threadParams, blackhole, notifyControl, startRndMask";
    }

    private String getStubTypeArgs() {
        return "InfraControl control, RawResults result, " +
                "BenchmarkParams benchmarkParams, IterationParams iterationParams, ThreadParams threadParams, " +
                "Blackhole blackhole, Control notifyControl, int startRndMask";
    }

    private void methodProlog(PrintWriter writer, MethodGroup methodGroup) {
        // do nothing
        writer.println(ident(2) + "this.benchmarkParams = control.benchmarkParams;");
        writer.println(ident(2) + "this.iterationParams = control.iterationParams;");
        writer.println(ident(2) + "this.threadParams    = threadParams;");
        writer.println(ident(2) + "this.notifyControl   = control.notifyControl;");
        writer.println(ident(2) + "if (this.blackhole == null) {");
        writer.println(ident(3) + "this.blackhole = new Blackhole(\"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.\");");
        writer.println(ident(2) + "}");
    }

    private void methodEpilog(PrintWriter writer, MethodGroup methodGroup) {
        writer.println(ident(3) + "this.blackhole.evaporate(\"Yes, I am Stephen Hawking, and know a thing or two about black holes.\");");
    }

    private String prefix(String argList) {
        if (argList.trim().isEmpty()) {
            return "";
        } else {
            return ", " + argList;
        }
    }

    private void generateSampleTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public BenchmarkTaskResult " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");
            writer.println(ident(3) + "RawResults res = new RawResults();");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(4) + "res.allOps++;");
            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.startMeasurement = true;");

            // measurement loop call
            writer.println(ident(3) + "int targetSamples = (int) (control.getDuration(TimeUnit.MILLISECONDS) * 20); // at max, 20 timestamps per millisecond");
            writer.println(ident(3) + "int batchSize = iterationParams.getBatchSize();");
            writer.println(ident(3) + "int opsPerInv = benchmarkParams.getOpsPerInvocation();");
            writer.println(ident(3) + "SampleBuffer buffer = new SampleBuffer();");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX + "(" +
                    getStubArgs() + ", buffer, targetSamples, opsPerInv, batchSize" + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.stopMeasurement = true;");

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(5) + "res.allOps++;");
            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            /*
               Adjust the operation counts:
                  1) res.measuredOps counted the batched @Benchmark invocations. Therefore, we need only
                     to adjust for opsPerInv (pretending each @Benchmark invocation counts as $opsPerInv ops);
                  2) res.allOps counted the individual @Benchmark invocations; to it needs the adjustment for $batchSize.

               It's prudent to make the multiplication first to get more accuracy.
             */

            writer.println(ident(3) + "res.allOps += res.measuredOps * batchSize;");

            writer.println(ident(3) + "res.allOps *= opsPerInv;");
            writer.println(ident(3) + "res.allOps /= batchSize;");
            writer.println(ident(3) + "res.measuredOps *= opsPerInv;");

            writer.println(ident(3) + "BenchmarkTaskResult results = new BenchmarkTaskResult(res.allOps, res.measuredOps);");
            if (isSingleMethod) {
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", buffer, benchmarkParams.getTimeUnit()));");
            } else {
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.PRIMARY, \"" + methodGroup.getName() + "\", buffer, benchmarkParams.getTimeUnit()));");
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", buffer, benchmarkParams.getTimeUnit()));");
            }
            methodEpilog(writer, methodGroup);

            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;
            compilerControl.defaultForceInline(method);

            writer.println(ident(1) + "public static" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName + "(" +
                    getStubTypeArgs() + ", SampleBuffer buffer, int targetSamples, long opsPerInv, int batchSize" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");

            writer.println(ident(2) + "long realTime = 0;");
            writer.println(ident(2) + "long operations = 0;");
            writer.println(ident(2) + "int rnd = (int)System.nanoTime();");
            writer.println(ident(2) + "int rndMask = startRndMask;");
            writer.println(ident(2) + "long time = 0;");
            writer.println(ident(2) + "int currentStride = 0;");
            writer.println(ident(2) + "do {");

            invocationProlog(writer, 3, method, states, true);

            writer.println(ident(3) + "rnd = (rnd * 1664525 + 1013904223);");
            writer.println(ident(3) + "boolean sample = (rnd & rndMask) == 0;");
            writer.println(ident(3) + "if (sample) {");
            writer.println(ident(4) + "time = System.nanoTime();");
            writer.println(ident(3) + "}");

            writer.println(ident(3) + "for (int b = 0; b < batchSize; b++) {");
            writer.println(ident(4) + "if (control.volatileSpoiler) return;");
            writer.println(ident(4) + "" + emitCall(method, states) + ';');
            writer.println(ident(3) + "}");

            writer.println(ident(3) + "if (sample) {");
            writer.println(ident(4) + "buffer.add((System.nanoTime() - time) / opsPerInv);");
            writer.println(ident(4) + "if (currentStride++ > targetSamples) {");
            writer.println(ident(5) + "buffer.half();");
            writer.println(ident(5) + "currentStride = 0;");
            writer.println(ident(5) + "rndMask = (rndMask << 1) + 1;");
            writer.println(ident(4) + "}");
            writer.println(ident(3) + "}");

            invocationEpilog(writer, 3, method, states, true);

            writer.println(ident(3) + "operations++;");
            writer.println(ident(2) + "} while(!control.isDone);");
            writer.println(ident(2) + "startRndMask = Math.max(startRndMask, rndMask);");

            writer.println(ident(2) + "result.realTime = realTime;");
            writer.println(ident(2) + "result.measuredOps = operations;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void generateSingleShotTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public BenchmarkTaskResult " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            // control objects get a special treatment
            writer.println(ident(3) + "notifyControl.startMeasurement = true;");

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults();");
            writer.println(ident(3) + "int batchSize = iterationParams.getBatchSize();");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX + "(" +
                    getStubArgs() + ", batchSize" + prefix(states.getArgList(method)) + ");");

            writer.println(ident(3) + "control.preTearDown();");

            iterationEpilog(writer, 3, method, states);

            /*
             * Adjust total ops:
             *   Single shot always does single op.  Therefore, we need to adjust for $opsPerInv (pretending each @Benchmark
             *   invocation counts as $opsPerInv ops). We *don't need* to adjust down for $batchSize, because we always have
             *   one "op".
             */

            writer.println(ident(3) + "int opsPerInv = control.benchmarkParams.getOpsPerInvocation();");
            writer.println(ident(3) + "long totalOps = opsPerInv;");

            writer.println(ident(3) + "BenchmarkTaskResult results = new BenchmarkTaskResult(totalOps, totalOps);");
            if (isSingleMethod) {
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getTime(), benchmarkParams.getTimeUnit()));");
            } else {
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.PRIMARY, \"" + methodGroup.getName() + "\", res.getTime(), benchmarkParams.getTimeUnit()));");
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getTime(), benchmarkParams.getTimeUnit()));");
            }
            methodEpilog(writer, methodGroup);

            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement stub bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;
            compilerControl.defaultForceInline(method);

            writer.println(ident(1) + "public static" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName +
                    "(" + getStubTypeArgs() + ", int batchSize" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");

            writer.println(ident(2) + "long realTime = 0;");
            writer.println(ident(2) + "result.startTime = System.nanoTime();");
            writer.println(ident(2) + "for (int b = 0; b < batchSize; b++) {");
            writer.println(ident(3) + "if (control.volatileSpoiler) return;");

            invocationProlog(writer, 3, method, states, true);

            writer.println(ident(3) + emitCall(method, states) + ';');

            invocationEpilog(writer, 3, method, states, true);

            writer.println(ident(2) + "}");
            writer.println(ident(2) + "result.stopTime = System.nanoTime();");
            writer.println(ident(2) + "result.realTime = realTime;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void invocationProlog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states, boolean pauseMeasurement) {
        if (states.hasInvocationStubs(method)) {
            for (String s : states.getInvocationSetups(method))
                writer.println(ident(prefix) + s);
            if (pauseMeasurement)
                writer.println(ident(prefix) + "long rt = System.nanoTime();");
        }
    }

    private void invocationEpilog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states, boolean pauseMeasurement) {
        if (states.hasInvocationStubs(method)) {
            if (pauseMeasurement)
                writer.println(ident(prefix) + "realTime += (System.nanoTime() - rt);");
            for (String s : states.getInvocationTearDowns(method))
                writer.println(ident(prefix) + s);
        }
    }

    private void iterationProlog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states) {
        for (String s : states.getStateGetters(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "control.preSetup();");

        for (String s : states.getIterationSetups(method)) writer.println(ident(prefix) + s);
        writer.println();

        // reset @AuxCounters
        for (String s : states.getAuxResets(method)) writer.println(ident(prefix) + s);
        writer.println();

    }

    private void iterationEpilog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states) {
        for (String s : states.getIterationTearDowns(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "if (control.isLastIteration()) {");
        for (String s : states.getRunTearDowns(method)) writer.println(ident(prefix + 1) + s);
        for (String s : states.getStateDestructors(method)) writer.println(ident(prefix + 1) + s);
        writer.println(ident(prefix) + "}");
    }

    private String emitCall(MethodInfo method, StateObjectHandler states) {
        if ("void".equalsIgnoreCase(method.getReturnType())) {
            return states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getBenchmarkArgList(method) + ")";
        } else {
            return "blackhole.consume(" + states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getBenchmarkArgList(method) + "))";
        }
    }

    static String[] INDENTS = new String[0];

    static String ident(int tabs) {
        final int TAB_SIZE = 4;
        if (tabs >= INDENTS.length) {
            INDENTS = new String[tabs + 1];
            for (int p = 0; p <= tabs; p++) {
                char[] chars = new char[p * TAB_SIZE];
                Arrays.fill(chars, ' ');
                INDENTS[p] = new String(chars);
            }
        }
        return INDENTS[tabs];
    }

}
