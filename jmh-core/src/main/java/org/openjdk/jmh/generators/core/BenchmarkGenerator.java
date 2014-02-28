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
package org.openjdk.jmh.generators.core;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.generators.source.BenchmarkInfo;
import org.openjdk.jmh.generators.source.ClassInfo;
import org.openjdk.jmh.generators.source.FieldInfo;
import org.openjdk.jmh.generators.source.GenerationException;
import org.openjdk.jmh.generators.source.GeneratorSource;
import org.openjdk.jmh.generators.source.MethodGroup;
import org.openjdk.jmh.generators.source.MethodInfo;
import org.openjdk.jmh.generators.source.ParameterInfo;
import org.openjdk.jmh.generators.source.StateObject;
import org.openjdk.jmh.logic.BlackHole;
import org.openjdk.jmh.logic.InfraControl;
import org.openjdk.jmh.logic.ThreadControl;
import org.openjdk.jmh.logic.results.AverageTimeResult;
import org.openjdk.jmh.logic.results.RawResults;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.ResultRole;
import org.openjdk.jmh.logic.results.SampleTimeResult;
import org.openjdk.jmh.logic.results.SingleShotResult;
import org.openjdk.jmh.logic.results.ThroughputResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.MicroBenchmarkList;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;
import org.openjdk.jmh.util.internal.SampleBuffer;

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
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class BenchmarkGenerator {

    private final Set<BenchmarkInfo> benchmarkInfos;
    private final Collection<Plugin> plugins;
    private final CompilerControlPlugin compilerControl;
    private final Set<String> processedBenchmarks;

    public BenchmarkGenerator() {
        benchmarkInfos = new HashSet<BenchmarkInfo>();
        processedBenchmarks = new HashSet<String>();
        compilerControl = new CompilerControlPlugin();

        plugins = new ArrayList<Plugin>();
        plugins.add(new HelperMethodValidationPlugin());
        plugins.add(new GroupValidationPlugin());
        plugins.add(new ParamValidationPlugin());
        plugins.add(compilerControl);
    }

    public void generate(GeneratorSource source) {
        try {
            for (Plugin sub : plugins) {
                sub.process(source);
            }

            // Build a Set of classes with a list of annotated methods
            Multimap<ClassInfo, MethodInfo> clazzes = buildAnnotatedSet(source);

            // Generate code for all found Classes and Methods
            for (ClassInfo clazz : clazzes.keys()) {
                if (!processedBenchmarks.add(clazz.getQualifiedName())) continue;
                try {
                    validateBenchmark(clazz, clazzes.get(clazz));
                    BenchmarkInfo info = makeBenchmarkInfo(clazz, clazzes.get(clazz));
                    generateClass(source, clazz, info);
                    benchmarkInfos.add(info);
                } catch (GenerationException ge) {
                    source.printError(ge.getMessage(), ge.getElement());
                }
            }
        } catch (Throwable t) {
            source.printError("Annotation generators had thrown the exception: " + t);
            t.printStackTrace(System.err);
        }
    }

    public void complete(GeneratorSource source) {
        for (Plugin sub : plugins) {
            sub.finish(source);
        }

        // Processing completed, final round. Print all added methods to file
        try {
            PrintWriter writer = new PrintWriter(source.newResource(MicroBenchmarkList.MICROBENCHMARK_LIST.substring(1)));
            for (BenchmarkInfo info : benchmarkInfos) {
                for (String method : info.methodGroups.keySet()) {
                    MethodGroup group = info.methodGroups.get(method);
                    for (Mode m : group.getModes()) {
                        BenchmarkRecord br = new BenchmarkRecord(
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
                                group.getJVMArgs(),
                                group.getJVMArgsPrepend(),
                                group.getJVMArgsAppend(),
                                group.getParams()
                        );
                        writer.println(br.toLine());
                    }
                }
            }

            writer.close();
        } catch (IOException ex) {
            source.printError("Error writing MicroBenchmark list " + ex);
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
                    GenerateMicroBenchmark ann = mi.getAnnotation(GenerateMicroBenchmark.class);
                    if (ann != null) {
                        result.put(currentClass, mi);
                    }
                }
            } while ((walk = walk.getSuperclass()) != null);
        }
        return result;
    }


    /**
     * Do basic benchmark validation.
     */
    private void validateBenchmark(ClassInfo clazz, Collection<MethodInfo> methods) {
        if (clazz.getQualifiedName().isEmpty()) {
            throw new GenerationException("Microbenchmark should have package other than default.", clazz);
        }

        Collection<ClassInfo> states = new ArrayList<ClassInfo>();

        // validate all arguments are @State-s
        for (MethodInfo e : methods) {
            for (ParameterInfo var : e.getParameters()) {
                states.add(var.getType());
            }
        }

        // validate if enclosing class is implicit @State
        if (clazz.getAnnotation(State.class) != null) {
            states.add(clazz);
        }

        // validate @State classes
        for (ClassInfo state : states) {
            // Because of https://bugs.openjdk.java.net/browse/JDK-8031122,
            // we need to preemptively check the annotation value, and
            // the API can only allow that by catching the exception, argh.
            try {
                state.getAnnotation(State.class).value();
            } catch (IncompleteAnnotationException iae) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation should have the explicit " + Scope.class.getSimpleName() + " argument",
                        state);
            }

            if (!state.isPublic()) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation only supports public classes.", state);
            }

            boolean hasDefaultConstructor = false;
            for (MethodInfo constructor : state.getConstructors()) {
                hasDefaultConstructor |= (constructor.getParameters().isEmpty() && constructor.isPublic());
            }

            if (!hasDefaultConstructor) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation can only be applied to the classes having the default public constructor." +
                        " Make sure the class is nested (static).",
                        state);
            }
        }

        // validate against rogue fields
        if (clazz.getAnnotation(State.class) == null || clazz.isAbstract()) {
            for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(clazz)) {
                // allow static fields
                if (fi.isStatic()) continue;
                throw new GenerationException(
                        "Field \"" + fi.getName() + "\" is declared within " +
                                "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                "This can result in unspecified behavior, and prohibited.", fi);
            }
        }

        // check modifiers
        for (MethodInfo m : methods) {
            if (!m.isPublic()) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName() +
                        " method should be public.", m);
            }

            if (m.isAbstract()) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                        + " method can not be abstract.", m);
            }
            if (m.isSynchronized()) {
                if (clazz.getAnnotation(State.class) == null) {
                    throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                            + " method can only be synchronized if the enclosing class is annotated with "
                            + "@" + State.class.getSimpleName() + ".", m);
                    }
            }
        }

        // check annotations
        for (MethodInfo m : methods) {
            OperationsPerInvocation opi = m.getAnnotationRecursive(OperationsPerInvocation.class);
            if (opi != null && opi.value() < 1) {
                throw new GenerationException("The " + OperationsPerInvocation.class.getSimpleName() +
                        " needs to be greater than 0.", m);
            }
        }
    }

    /**
     * validate benchmark info
     * @param info benchmark info to validate
     */
    private void validateBenchmarkInfo(BenchmarkInfo info) {
        // check the @Group preconditions,
        // ban some of the surprising configurations
        //
        for (MethodGroup group : info.methodGroups.values()) {
            if (group.methods().size() == 1) {
                MethodInfo meth = group.methods().iterator().next();
                if (meth.getAnnotation(Group.class) == null) {
                    for (ParameterInfo param : meth.getParameters()) {
                        State stateAnn = param.getType().getAnnotation(State.class);
                        if (stateAnn != null && stateAnn.value() == Scope.Group) {
                            throw new GenerationException(
                                    "Only @" + Group.class.getSimpleName() + " methods can reference @" + State.class.getSimpleName()
                                            + "(" + Scope.class.getSimpleName() + "." + Scope.Group + ") states.",
                                    meth);
                        }
                    }

                    State stateAnn = meth.getOwner().getAnnotation(State.class);
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
    }

    /**
     * Generate BenchmarkInfo for given class.
     * We will figure out method groups at this point.
     *
     * @param clazz holder class
     * @param methods annotated methods
     * @return BenchmarkInfo
     */
    private BenchmarkInfo makeBenchmarkInfo(ClassInfo clazz, Collection<MethodInfo> methods) {
        Map<String, MethodGroup> result = new TreeMap<String, MethodGroup>();

        for (MethodInfo method : methods) {
            Group groupAnn = method.getAnnotation(Group.class);
            String groupName = (groupAnn != null) ? groupAnn.value() : method.getName();

            if (!checkJavaIdentifier(groupName)) {
                throw new GenerationException("Group name should be the legal Java identifier.", method);
            }

            MethodGroup group = result.get(groupName);
            if (group == null) {
                group = new MethodGroup(groupName);
                result.put(groupName, group);
            }

            BenchmarkMode mbAn = method.getAnnotationRecursive(BenchmarkMode.class);
            if (mbAn != null) {
                group.addModes(mbAn.value());
            }

            group.addStrictFP(clazz.isStrictFP());
            group.addStrictFP(method.isStrictFP());
            group.addMethod(method, (method.getAnnotation(GroupThreads.class) != null) ? method.getAnnotation(GroupThreads.class).value() : 1);

            // Discovering @Params, part 1:
            //   For each parameter, walk the type hierarchy up to discover inherited @Param fields in @State objects.
            for (ParameterInfo pi : method.getParameters()) {
                for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(pi.getType())) {
                    if (fi.getAnnotation(Param.class) != null) {
                        group.addParam(fi.getName(), fi.getAnnotation(Param.class).value());
                    }
                }
            }

            // Discovering @Params, part 2:
            //  Walk the type hierarchy up to discover inherited @Param fields for class.
            for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(clazz)) {
                if (fi.getAnnotation(Param.class) != null) {
                    group.addParam(fi.getName(), fi.getAnnotation(Param.class).value());
                }
            }
        }

        // enforce the default value
        for (MethodGroup group : result.values()) {
            if (group.getModes().isEmpty()) {
                group.addModes(Mode.Throughput);
            }
        }

        String sourcePackage = clazz.getPackageName();
        String generatedPackageName = sourcePackage + ".generated";
        String generatedClassName = clazz.getNestedName();

        BenchmarkInfo info = new BenchmarkInfo(clazz.getQualifiedName(), generatedPackageName, generatedClassName, result);
        validateBenchmarkInfo(info);
        return info;
    }

    public static boolean checkJavaIdentifier(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create and generate Java code for a class and it's methods
     */
    private void generateClass(GeneratorSource source, ClassInfo classInfo, BenchmarkInfo info) {
        try {
            // Create file and open an outputstream
            PrintWriter writer = new PrintWriter(source.newFile(info.generatedName), false);

            // Write package and imports
            writer.println("package " + info.generatedPackageName + ';');
            writer.println();

            generateImport(writer);

            // Write class header
            writer.println("@" + CompilerControl.class.getSimpleName() +
                    "(" + CompilerControl.class.getSimpleName() + "." + CompilerControl.Mode.class.getSimpleName() +
                    "." + CompilerControl.Mode.DONT_INLINE + ")");
            writer.println("@Generated(\"" + BenchmarkGenerator.class.getCanonicalName() + "\")");
            writer.println("public final class " + info.generatedClassName + " {");
            writer.println();
            generatePadding(writer);

            StateObjectHandler states = new StateObjectHandler();

            // benchmark instance is implicit
            states.bindImplicit(classInfo, "bench", Scope.Thread);

            // default blackhole is implicit
            states.bindImplicit(source.resolveClass(BlackHole.class.getCanonicalName()), "blackhole", Scope.Thread);

            // Write all methods
            for (String groupName : info.methodGroups.keySet()) {
                for (MethodInfo method : info.methodGroups.get(groupName).methods()) {
                    for (ParameterInfo p : method.getParameters()) {
                        states.bindArg(method, p);
                    }
                }

                for (Mode benchmarkKind : Mode.values()) {
                    if (benchmarkKind == Mode.All) continue;
                    generateMethod(benchmarkKind, writer, info.methodGroups.get(groupName), states);
                }
                states.clearArgs();
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
        } catch (IOException ex) {
            throw new GenerationException("IOException", classInfo);
        }
    }

    private void generatePadding(PrintWriter writer) {
        for (int p = 0; p < 16; p++) {
            StringBuilder sb = new StringBuilder();
            sb.append(ident(1));
            sb.append("private boolean p").append(p);
            for (int q = 1; q < 16; q++) {
                sb.append(", p").append(p).append("_").append(q);
            }
            sb.append(";");
            writer.println(sb.toString());
        }
    }

    private void generateImport(PrintWriter writer) {
        Class<?>[] imports = new Class<?>[] {
                List.class, AtomicInteger.class, AtomicIntegerFieldUpdater.class,
                Collection.class, Collections.class, ArrayList.class, Arrays.class,
                TimeUnit.class, Generated.class, CompilerControl.class,
                InfraControl.class, ThreadControl.class, BlackHole.class,
                Result.class, ThroughputResult.class, AverageTimeResult.class,
                SampleTimeResult.class, SingleShotResult.class, SampleBuffer.class,
                Mode.class, Fork.class, Measurement.class, Threads.class, Warmup.class,
                BenchmarkMode.class, RawResults.class, ResultRole.class,
                Field.class
        };

        for (Class<?> c : imports) {
            writer.println("import " + c.getName() + ';');
        }
        writer.println();
    }

    /**
     * Generate the method for a specific benchmark method
     */
    private void generateMethod(Mode benchmarkKind, PrintWriter writer, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println();
        switch (benchmarkKind) {
            case Throughput:
                generateThroughput(writer, benchmarkKind, methodGroup, states);
                break;
            case AverageTime:
                generateAverageTime(writer, benchmarkKind, methodGroup, states);
                break;
            case SampleTime:
                generateSampleTime(writer, benchmarkKind, methodGroup, states);
                break;
            case SingleShotTime:
                generateSingleShotTime(writer, benchmarkKind, methodGroup, states);
                break;
            default:
                throw new AssertionError("Shouldn't be here");
        }
    }

    private void generateThroughput(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadControl.subgroup == " + subGroup + ") {");

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
                writer.println(ident(3) + so.localIdentifier + ".iterationTime = control.getDuration();");
            }

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults(" + methodGroup.getOperationsPerInvocation() + "L);");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind + "_measurementLoop" +
                    "(control, res, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");

            // iteration prolog
            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + methodGroup.getOutputTimeUnit() + ";");
            writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), tu));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            String methodName = method.getName() + "_" + benchmarkKind + "_measurementLoop";
            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + methodName + "(InfraControl control, RawResults result, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
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

    private void generateAverageTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadControl.subgroup == " + subGroup + ") {");

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
                writer.println(ident(3) + so.localIdentifier + ".iterationTime = control.getDuration();");
            }

            // measurement loop call
            writer.println(ident(3) + "RawResults res = new RawResults(" + methodGroup.getOperationsPerInvocation() + "L);");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind + "_measurementLoop(control, res, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + methodGroup.getOutputTimeUnit() + ";");
            writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", res.getOperations(), res.getTime(), tu));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") +  " void " + method.getName() + "_" + benchmarkKind + "_measurementLoop" +
                    "(InfraControl control, RawResults result, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() +
                    prefix(states.getTypeArgList(method)) + ") throws Throwable {");
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

    private void generateSampleTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind +
                "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadControl.subgroup == " + subGroup + ") {");

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
                writer.println(ident(3) + so.localIdentifier + ".iterationTime = control.getDuration();");
            }

            // measurement loop call
            writer.println(ident(3) + "SampleBuffer buffer = new SampleBuffer();");
            writer.println(ident(3) + method.getName() + "_" + benchmarkKind + "_measurementLoop(control, buffer, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "control.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (control.warmdownShouldWait) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");
            writer.println();

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + methodGroup.getOutputTimeUnit() + ";");
            writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.PRIMARY, \"" + method.getName() + "\", buffer, tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.SECONDARY, \"" + method.getName() + "\", buffer, tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");

        writer.println();

        // measurement loop bodies
        for (MethodInfo method : methodGroup.methods()) {
            writer.println(ident(1) + "public" + (methodGroup.isStrictFP() ? " strictfp" : "") + " void " + method.getName() + "_" + benchmarkKind + "_measurementLoop(InfraControl control, SampleBuffer buffer, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println(ident(2) + "long realTime = 0;");
            writer.println(ident(2) + "int rnd = (int)System.nanoTime();");
            writer.println(ident(2) + "int rndMask = 0;");
            writer.println(ident(2) + "long time = 0;");
            writer.println(ident(2) + "int currentStride = 0;");
            writer.println(ident(2) + "do {");

            invocationProlog(writer, 3, method, states, true);

            writer.println(ident(3) + "rnd = (rnd * 1664525 + 1013904223);");
            writer.println(ident(3) + "boolean sample = (rnd & rndMask) == 0;");
            writer.println(ident(3) + "if (sample) {");
            writer.println(ident(4) + "time = System.nanoTime();");
            writer.println(ident(3) + "}");
            writer.println(ident(3) + "" + emitCall(method, states) + ';');
            writer.println(ident(3) + "if (sample) {");
            writer.println(ident(4) + "buffer.add(System.nanoTime() - time);");
            writer.println(ident(4) + "if (currentStride++ > 1000000) {");
            writer.println(ident(5) + "buffer.half();");
            writer.println(ident(5) + "currentStride = 0;");
            writer.println(ident(5) + "rndMask = (rndMask << 1) + 1;");
            writer.println(ident(4) + "}");
            writer.println(ident(3) + "}");

            invocationEpilog(writer, 3, method, states, true);

            writer.println(ident(2) + "} while(!control.isDone);");

            writer.println(ident(1) + "}");
            writer.println();
        }
    }

    private void generateSingleShotTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        writer.println(ident(2) + "long realTime = 0;");

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (MethodInfo method : methodGroup.methods()) {
            compilerControl.defaultForceInline(method);

            subGroup++;

            writer.println(ident(2) + "if (threadControl.subgroup == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            invocationProlog(writer, 3, method, states, false);

            writer.println(ident(3) + "long time1 = System.nanoTime();");
            writer.println(ident(3) + "int batchSize = control.batchSize;");
            writer.println(ident(3) + "for (int b = 0; b < batchSize; b++) {");
            writer.println(ident(4) + emitCall(method, states) + ';');
            writer.println(ident(3) + "}");
            writer.println(ident(3) + "long time2 = System.nanoTime();");

            invocationEpilog(writer, 3, method, states, false);

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + methodGroup.getOutputTimeUnit() + ";");
            writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.PRIMARY, \"" + method.getName() + "\", (realTime > 0) ? realTime : (time2 - time1), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.SECONDARY, \"" + method.getName() + "\", (realTime > 0) ? realTime : (time2 - time1), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println(ident(1) + "}");
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
        writer.println(ident(prefix) + "control.preTearDown();");

        for (String s : states.getIterationTearDowns(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "if (control.isLastIteration()) {");
        for (String s : states.getRunTearDowns(method)) writer.println(ident(prefix + 1) + s);
        for (String s : states.getStateDestructors(method)) writer.println(ident(prefix + 1) + s);
        writer.println(ident(prefix) + "}");
    }

    private String emitCall(MethodInfo method, StateObjectHandler states) {
        if ("void".equalsIgnoreCase(method.getReturnType())) {
            return states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getArgList(method) + ")";
        } else {
            return states.getImplicit("blackhole").localIdentifier + ".consume(" + states.getImplicit("bench").localIdentifier + "." + method.getName() + "(" + states.getArgList(method) + "))";
        }
    }

    public static String ident(int prefix) {
        char[] chars = new char[prefix*4];
        for (int i = 0; i < prefix*4; i++) {
            chars[i] = ' ';
        }
        return new String(chars);
    }

}
