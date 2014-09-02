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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.AverageTimeResult;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.SampleTimeResult;
import org.openjdk.jmh.results.SingleShotResult;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.InfraControl;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.SampleBuffer;

import javax.annotation.Generated;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Benchmark generator.
 *
 * <p>Benchmark generator is the agnostic piece of code which generates
 * synthetic Java code for JMH benchmarks. {@link GeneratorSource} is
 * used to feed the generator with the required metadata.</p>
 */
public class BenchmarkGenerator {

    private final Set<BenchmarkInfo> benchmarkInfos;
    private final CompilerControlPlugin compilerControl;
    private final Set<String> processedBenchmarks;

    public BenchmarkGenerator() {
        benchmarkInfos = new HashSet<BenchmarkInfo>();
        processedBenchmarks = new HashSet<String>();
        compilerControl = new CompilerControlPlugin();
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

        // Processing completed, final round. Print all added methods to file
        try {
            PrintWriter writer = new PrintWriter(destination.newResource(BenchmarkList.BENCHMARK_LIST.substring(1)));
            for (BenchmarkInfo info : benchmarkInfos) {
                MethodGroup group = info.methodGroup;
                String method = group.getName();
                for (Mode m : group.getModes()) {
                    BenchmarkListEntry br = new BenchmarkListEntry(
                            info.userName + "." + method,
                            info.generatedName + "." + method,
                            m,
                            group.getThreads(),
                            group.getTotalThreadCount(),
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
                            group.getOperationsPerInvocation()
                    );
                    writer.println(br.toLine());
                }
            }

            writer.close();
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
        //   If superclass has a @GMB method, then all subclasses also have it.
        //   We skip the generated classes, which we had probably generated during the previous rounds
        //   of processing. Abstract classes are of no interest for us either.

        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<ClassInfo, MethodInfo>();
        for (ClassInfo currentClass : source.getClasses()) {
            ClassInfo walk = currentClass;
            do {
                if (currentClass.getQualifiedName().contains("generated")) continue;
                if (currentClass.isAbstract()) continue;
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

        Collection<ClassInfo> states = new ArrayList<ClassInfo>();

        // validate all arguments are @State-s
        for (MethodInfo e : methods) {
            for (ParameterInfo var : e.getParameters()) {
                if (BenchmarkGeneratorUtils.getAnnSuper(var.getType(), State.class) == null) {
                    throw new GenerationException(
                            "Method parameters should be @" + State.class.getSimpleName() + " classes.",
                            e);
                }
                states.add(var.getType());
            }
        }

        // validate if enclosing class is implicit @State
        if (BenchmarkGeneratorUtils.getAnnSuper(clazz, State.class) != null) {
            states.add(clazz);
        }

        // validate @State classes
        for (ClassInfo state : states) {
            // Because of https://bugs.openjdk.java.net/browse/JDK-8031122,
            // we need to preemptively check the annotation value, and
            // the API can only allow that by catching the exception, argh.
            try {
                BenchmarkGeneratorUtils.getAnnSuper(state, State.class).value();
            } catch (IncompleteAnnotationException iae) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation should have the explicit " + Scope.class.getSimpleName() + " argument",
                        state);
            }

            if (!state.isPublic()) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation only supports public classes.", state);
            }

            if (state.isFinal()) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation does not support final classes.", state);
            }

            if (state.isInner()) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation does not support inner classes, make sure your class is static.", state);
            }

            if (state.isAbstract()) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation does not support abstract classes.", state);
            }

            boolean hasDefaultConstructor = false;
            for (MethodInfo constructor : state.getConstructors()) {
                hasDefaultConstructor |= (constructor.getParameters().isEmpty() && constructor.isPublic());
            }

            // These classes use the special init sequence:
            hasDefaultConstructor |= state.getQualifiedName().equals(BenchmarkParams.class.getCanonicalName());
            hasDefaultConstructor |= state.getQualifiedName().equals(IterationParams.class.getCanonicalName());
            hasDefaultConstructor |= state.getQualifiedName().equals(ThreadParams.class.getCanonicalName());

            if (!hasDefaultConstructor) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation can only be applied to the classes having the default public constructor.",
                        state);
            }
        }

        // validate against rogue fields
        if (BenchmarkGeneratorUtils.getAnnSuper(clazz, State.class) == null || clazz.isAbstract()) {
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
        for (ClassInfo state : states) {
            BenchmarkGeneratorUtils.checkAnnotations(state);
        }

        // validate rogue annotations on fields
        for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(clazz)) {
            BenchmarkGeneratorUtils.checkAnnotations(fi);
        }
        for (ClassInfo state : states) {
            for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(state)) {
                BenchmarkGeneratorUtils.checkAnnotations(fi);
            }
        }

        // validate rogue annotations on methods
        for (MethodInfo mi : methods) {
            BenchmarkGeneratorUtils.checkAnnotations(mi);
        }
        for (ClassInfo state : states) {
            for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(state)) {
                BenchmarkGeneratorUtils.checkAnnotations(mi);
            }
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
        Map<String, MethodGroup> result = new TreeMap<String, MethodGroup>();

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

        Collection<BenchmarkInfo> benchmarks = new ArrayList<BenchmarkInfo>();
        for (MethodGroup group : result.values()) {
            String sourcePackage = clazz.getPackageName();
            String generatedPackageName = sourcePackage + ".generated";
            String generatedClassName = BenchmarkGeneratorUtils.getGeneratedName(clazz) + "_" + group.getName();

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
        // Create file and open an outputstream
        PrintWriter writer = new PrintWriter(destination.newClass(info.generatedName), false);

        // Write package and imports
        writer.println("package " + info.generatedPackageName + ';');
        writer.println();

        generateImport(writer);

        // Write class header
        writer.println("@Generated(\"" + BenchmarkGenerator.class.getCanonicalName() + "\")");
        writer.println("public final class " + info.generatedClassName + " {");
        writer.println();

        // generate padding
        Paddings.padding(writer, "p");

        writer.println(ident(1) + "int startRndMask;");

        StateObjectHandler states = new StateObjectHandler(compilerControl);

        // benchmark instance is implicit
        states.bindImplicit(classInfo, "bench", Scope.Thread);

        // default blackhole is implicit
        states.bindImplicit(source.resolveClass(Blackhole.class.getCanonicalName()), "blackhole", Scope.Thread);

        // bind all methods
        states.bindMethodGroup(info.methodGroup);

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
        for (String s : states.getStateOverrides()) {
            writer.println(ident(1) + s);
        }
        writer.println();

        // Finish class
        writer.println("}");
        writer.println();

        writer.close();
    }

    private void generateImport(PrintWriter writer) {
        Class<?>[] imports = new Class<?>[]{
                List.class, AtomicInteger.class, AtomicIntegerFieldUpdater.class,
                Collection.class, Collections.class, ArrayList.class, Arrays.class,
                TimeUnit.class, Generated.class, CompilerControl.class,
                InfraControl.class, ThreadParams.class, Blackhole.class,
                Result.class, ThroughputResult.class, AverageTimeResult.class,
                SampleTimeResult.class, SingleShotResult.class, SampleBuffer.class,
                Mode.class, Fork.class, Measurement.class, Threads.class, Warmup.class,
                BenchmarkMode.class, RawResults.class, ResultRole.class,
                Field.class, BenchmarkParams.class, IterationParams.class
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
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".startMeasurement = true;");
            }

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults(control.benchmarkParams.getOpsPerInvocation());");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop" +
                    "(control, res" + prefix(states.getArgList(method)) + ");");

            // pretend we did the batched run; there is no reason to have an additional loop,
            // when _jmhLoop* already is optimized.
            writer.println(ident(3) + "res.operations /= control.iterationParams.getBatchSize();");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            // iteration prolog
            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), control.benchmarkParams.getTimeUnit()));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), control.benchmarkParams.getTimeUnit()));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), control.benchmarkParams.getTimeUnit()));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop";

            compilerControl.defaultForceInline(method);
            compilerControl.alwaysDontInline(classInfo.getQualifiedName(), methodName);

            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName + "(InfraControl control, RawResults result" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
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
            writer.println(ident(2) + "result.operations = operations;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void generateAverageTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".startMeasurement = true;");
            }

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults(control.benchmarkParams.getOpsPerInvocation());");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop(control, res" + prefix(states.getArgList(method)) + ");");

            // pretend we did the batched run; there is no reason to have an additional loop,
            // when _jmhLoop* already is optimized.
            writer.println(ident(3) + "res.operations /= control.iterationParams.getBatchSize();");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), control.benchmarkParams.getTimeUnit()));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), control.benchmarkParams.getTimeUnit()));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), control.benchmarkParams.getTimeUnit()));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop";
            compilerControl.defaultForceInline(method);
            compilerControl.alwaysDontInline(classInfo.getQualifiedName(), methodName);

            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName +
                    "(InfraControl control, RawResults result" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
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
            writer.println(ident(2) + "result.operations = operations;");
            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void methodProlog(PrintWriter writer, MethodGroup methodGroup) {
        // do nothing
    }

    private String prefix(String argList) {
        if (argList.trim().isEmpty()) {
            return "";
        } else {
            return ", " + argList;
        }
    }

    private void generateSampleTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "control.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (control.warmupShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");
            writer.println();

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".startMeasurement = true;");
            }

            // measurement loop call
            writer.println(ident(3) + "int targetSamples = (int) (control.getDuration(TimeUnit.MILLISECONDS) * 20); // at max, 20 timestamps per millisecond");
            writer.println(ident(3) + "int batchSize = control.iterationParams.getBatchSize();");
            writer.println(ident(3) + "SampleBuffer buffer = new SampleBuffer();");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop(control, buffer, targetSamples, control.benchmarkParams.getOpsPerInvocation(), batchSize" + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "try {");
            writer.println(ident(4) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 5, method, states, false);
            writer.println(ident(5) + emitCall(method, states) + ';');
            invocationEpilog(writer, 5, method, states, false);

            writer.println(ident(4) + "}");
            writer.println(ident(4) + "control.preTearDown();");
            writer.println(ident(3) + "} catch (InterruptedException ie) {");
            writer.println(ident(4) + "control.preTearDownForce();");
            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", buffer, control.benchmarkParams.getTimeUnit()));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", buffer, control.benchmarkParams.getTimeUnit()));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhLoop";
            compilerControl.defaultForceInline(method);
            compilerControl.alwaysDontInline(classInfo.getQualifiedName(), methodName);

            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName + "(InfraControl control, SampleBuffer buffer, int targetSamples, long opsPerInv, int batchSize" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println(ident(2) + "long realTime = 0;");
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

            writer.println(ident(2) + "} while(!control.isDone);");
            writer.println(ident(2) + "startRndMask = Math.max(startRndMask, rndMask);");

            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void generateSingleShotTime(ClassInfo classInfo, PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadParams threadParams) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadParams.getSubgroupIndex() == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            invocationProlog(writer, 3, method, states, false);

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults(control.benchmarkParams.getOpsPerInvocation());");
            writer.println(ident(3) + "int batchSize = control.iterationParams.getBatchSize();");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhStub(control, batchSize, res" + prefix(states.getArgList(method)) + ");");

            invocationEpilog(writer, 3, method, states, false);

            writer.println(ident(3) + "control.preTearDown();");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getTime(), control.benchmarkParams.getTimeUnit()));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getTime(), control.benchmarkParams.getTimeUnit()));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement stub bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind.shortLabel() + "_jmhStub";
            compilerControl.defaultForceInline(method);
            compilerControl.alwaysDontInline(classInfo.getQualifiedName(), methodName);

            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName +
                    "(InfraControl control, int batchSize, RawResults result" + prefix(states.getTypeArgList(method)) + ") throws Throwable {");

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
        if (!states.getInvocationSetups(method).isEmpty()) {
            for (String s : states.getInvocationSetups(method))
                writer.println(ident(prefix) + s);
            if (pauseMeasurement)
                writer.println(ident(prefix) + "long rt = System.nanoTime();");
            writer.println();
        }
    }

    private void invocationEpilog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states, boolean pauseMeasurement) {
        if (!states.getInvocationTearDowns(method).isEmpty()) {
            writer.println();
            if (pauseMeasurement)
                writer.println(ident(prefix) + "realTime += (System.nanoTime() - rt);");
            for (String s : states.getInvocationTearDowns(method))
                writer.println(ident(prefix) + s);
            writer.println();
        }
    }

    private void iterationProlog(PrintWriter writer, int prefix, MethodInfo method, StateObjectHandler states) {
        for (String s : states.getStateGetters(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "control.preSetup();");

        for (String s : states.getIterationSetups(method)) writer.println(ident(prefix) + s);
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
            return states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getGMBArgList(method) + ")";
        } else {
            return states.getImplicit("blackhole").localIdentifier + ".consume(" + states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getGMBArgList(method) + "))";
        }
    }

    static String ident(int prefix) {
        char[] chars = new char[prefix * 4];
        for (int i = 0; i < prefix * 4; i++) {
            chars[i] = ' ';
        }
        return new String(chars);
    }

}
