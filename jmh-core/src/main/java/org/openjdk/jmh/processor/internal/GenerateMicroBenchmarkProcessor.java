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
package org.openjdk.jmh.processor.internal;

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
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author staffan.friberg@oracle.com
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GenerateMicroBenchmarkProcessor extends AbstractProcessor {

    private final Set<BenchmarkInfo> benchmarkInfos = new HashSet<BenchmarkInfo>();

    private final Collection<SubProcessor> subProcessors = new ArrayList<SubProcessor>();
    private CompilerControlProcessor compilerControl;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        compilerControl = new CompilerControlProcessor();

        subProcessors.add(new HelperMethodValidationProcessor());
        subProcessors.add(new GroupValidationProcessor());
        subProcessors.add(new ParamValidationProcessor());
        subProcessors.add(compilerControl);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (SubProcessor sub : subProcessors) {
                sub.process(roundEnv, processingEnv);
            }

            if (!roundEnv.processingOver()) {
                TypeElement gmb = processingEnv.getElementUtils().getTypeElement(GenerateMicroBenchmark.class.getCanonicalName());
                // Build a Set of classes with a list of annotated methods
                Multimap<TypeElement, Element> clazzes = buildAnnotatedSet(gmb, roundEnv);

                // Generate code for all found Classes and Methods
                for (TypeElement clazz : clazzes.keys()) {
                    try {
                        validateBenchmark(clazz, clazzes.get(clazz));
                        BenchmarkInfo info = makeBenchmarkInfo(clazz, clazzes.get(clazz));
                        generateClass(clazz, info);
                        benchmarkInfos.add(info);
                    } catch (GenerationException ge) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, ge.getMessage(), ge.getElement());
                    }
                }
            } else {
                for (SubProcessor sub : subProcessors) {
                    sub.finish(roundEnv, processingEnv);
                }

                // Processing completed, final round. Print all added methods to file
                try {
                    FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                            MicroBenchmarkList.MICROBENCHMARK_LIST.substring(1));
                    PrintWriter writer = new PrintWriter(file.openWriter());
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
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Error writing MicroBenchmark list " + ex);
                }
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Annotation processor had throw exception: " + t);
            t.printStackTrace(System.err);
        }

        return true;
    }

    private List<TypeElement> getHierarchy(TypeElement subclass) {
        List<TypeElement> result = new ArrayList<TypeElement>();
        TypeElement walk = subclass;
        do {
            result.add(walk);
        } while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
        return result;
    }

    /**
     * Build a set of Classes which has annotated methods in them
     *
     * @return for all methods annotated with $annotation, returns Map<holder-class, Set<method>>
     */
    private Multimap<TypeElement, Element> buildAnnotatedSet(TypeElement te, RoundEnvironment roundEnv) {

        // Need to do a few rollovers to find all classes that have @GMB-annotated methods in their
        // subclasses. This is mostly due to some of the nested classes not discoverable at once,
        // when we need to discover the enclosing class first. With the potentially non-zero nesting
        // depth, we need to do a few rounds. Hopefully we will just do a single stride in most
        // cases.

        Collection<TypeElement> discoveredClasses = new TreeSet<TypeElement>(new Comparator<TypeElement>() {
            @Override
            public int compare(TypeElement o1, TypeElement o2) {
                return o1.getQualifiedName().toString().compareTo(o2.getQualifiedName().toString());
            }
        });

        // Walk around until convergence...

        int lastSize = -1;
        while (discoveredClasses.size() > lastSize) {
            lastSize = discoveredClasses.size();
            for (Element e : roundEnv.getRootElements()) {
                if (e.getKind() != ElementKind.CLASS) continue;
                TypeElement walk = (TypeElement) e;
                do {
                    discoveredClasses.add(walk);
                    for (TypeElement nested : ElementFilter.typesIn(walk.getEnclosedElements())) {
                        discoveredClasses.add(nested);
                    }
                } while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
            }
        }

        // Transitively close the hierarchy:
        //   If superclass has a @GMB method, then all subclasses also have it.
        //   We skip the generated classes, which we had probably generated during the previous rounds
        //   of processing. Abstract classes are of no interest for us either.

        Multimap<TypeElement, Element> result = new HashMultimap<TypeElement, Element>();
        for (TypeElement currentClass : discoveredClasses) {
            if (AnnUtils.getPackageName(currentClass).contains("generated")) continue;
            if (currentClass.getModifiers().contains(Modifier.ABSTRACT)) continue;

            for (TypeElement upperClass : getHierarchy(currentClass)) {
                if (AnnUtils.getPackageName(upperClass).contains("generated")) continue;
                for (ExecutableElement method : ElementFilter.methodsIn(upperClass.getEnclosedElements())) {
                    GenerateMicroBenchmark ann = method.getAnnotation(GenerateMicroBenchmark.class);
                    if (ann != null) {
                        result.put(currentClass, method);
                    }
                }
            }
        }
        return result;
    }


    /**
     * Do basic benchmark validation.
     */
    private void validateBenchmark(TypeElement clazz, Collection<? extends Element> methods) {
        if (AnnUtils.getPackageName(clazz).isEmpty()) {
            throw new GenerationException("Microbenchmark should have package other than default.", clazz);
        }

        Collection<TypeElement> states = new ArrayList<TypeElement>();

        // validate all arguments are @State-s
        for (Element e : methods) {
            ExecutableElement method = (ExecutableElement) e;
            for (VariableElement var : method.getParameters()) {
                TypeElement argClass = (TypeElement) processingEnv.getTypeUtils().asElement(var.asType());
                if (argClass.getAnnotation(State.class) == null) {
                    throw new GenerationException(
                            "The " + GenerateMicroBenchmark.class.getSimpleName() +
                            " annotation only supports methods with @State-bearing typed parameters.",
                            var);
                }
                states.add(argClass);
            }
        }

        // validate if enclosing class is implicit @State
        if (clazz.getAnnotation(State.class) != null) {
            states.add(clazz);
        }

        // validate @State classes
        for (TypeElement state : states) {
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

            if (!state.getModifiers().contains(Modifier.PUBLIC)) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation only supports public classes.", state);
            }
            if (state.getNestingKind().isNested() && !state.getModifiers().contains(Modifier.STATIC)) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation does not support inner classes, make sure the class is nested (static).",
                        state);
            }

            boolean hasDefaultConstructor = false;
            for (ExecutableElement constructor : ElementFilter.constructorsIn(state.getEnclosedElements())) {
                hasDefaultConstructor |= (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC));
            }

            if (!hasDefaultConstructor) {
                throw new GenerationException("The " + State.class.getSimpleName() +
                        " annotation can only be applied to the classes having the default public constructor.",
                        state);
            }
        }

        // validate against rogue fields
        if (clazz.getAnnotation(State.class) == null || clazz.getModifiers().contains(Modifier.ABSTRACT)) {
            for (VariableElement field : ElementFilter.fieldsIn(clazz.getEnclosedElements())) {
                // allow static fields
                if (!field.getModifiers().contains(Modifier.STATIC)) {
                    throw new GenerationException(
                        "Field \"" + field + "\" is declared within " +
                                "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                "This can result in unspecified behavior, and prohibited.", field);
                }
            }
        }

        // check modifiers
        for (Element m : methods) {
            if (!m.getModifiers().contains(Modifier.PUBLIC)) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName() +
                        " method should be public.", m);
            }

            if (m.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                        + " method can not be abstract.", m);
            }
            if (m.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                if (clazz.getAnnotation(State.class) == null) {
                    throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                            + " method can only be synchronized if the enclosing class is annotated with "
                            + "@" + State.class.getSimpleName() + ".", m);
                    }
            }
        }

        // check annotations
        for (Element m : methods) {
            OperationsPerInvocation opi = AnnUtils.getAnnotationRecursive(m, OperationsPerInvocation.class);
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
                ExecutableElement meth = (ExecutableElement) group.methods().iterator().next();
                if (meth.getAnnotation(Group.class) == null) {
                    for (VariableElement param : meth.getParameters()) {
                        TypeElement stateType = (TypeElement) processingEnv.getTypeUtils().asElement(param.asType());
                        State stateAnn = stateType.getAnnotation(State.class);
                        if (stateAnn != null && stateAnn.value() == Scope.Group) {
                            throw new GenerationException(
                                    "Only @" + Group.class.getSimpleName() + " methods can reference @" + State.class.getSimpleName()
                                            + "(" + Scope.class.getSimpleName() + "." + Scope.Group + ") states.",
                                    meth);
                        }
                    }

                    State stateAnn = meth.getEnclosingElement().getAnnotation(State.class);
                    if (stateAnn != null && stateAnn.value() == Scope.Group) {
                        throw new GenerationException(
                                "Only @" + Group.class.getSimpleName() + " methods can implicitly reference @" + State.class.getSimpleName()
                                        + "(" + Scope.class.getSimpleName() + "." + Scope.Group + ") states.",
                                meth);
                    }
                }
            } else {
                for (Element m : group.methods()) {
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
    private BenchmarkInfo makeBenchmarkInfo(TypeElement clazz, Collection<? extends Element> methods) {
        Map<String, MethodGroup> result = new TreeMap<String, MethodGroup>();

        boolean classStrictFP = clazz.getModifiers().contains(Modifier.STRICTFP);

        for (Element method : methods) {

            boolean methodStrictFP = method.getModifiers().contains(Modifier.STRICTFP);

            Group groupAnn = method.getAnnotation(Group.class);
            String groupName = (groupAnn != null) ? groupAnn.value() : method.getSimpleName().toString();

            if (!checkJavaIdentifier(groupName)) {
                throw new GenerationException("Group name should be the legal Java identifier.", method);
            }

            MethodGroup group = result.get(groupName);
            if (group == null) {
                group = new MethodGroup(groupName);
                result.put(groupName, group);
            }

            BenchmarkMode mbAn = AnnUtils.getAnnotationRecursive(method, BenchmarkMode.class);
            if (mbAn != null) {
                group.addModes(mbAn.value());
            }

            group.addStrictFP(classStrictFP);
            group.addStrictFP(methodStrictFP);
            group.addMethod(method, (method.getAnnotation(GroupThreads.class) != null) ? method.getAnnotation(GroupThreads.class).value() : 1);

            // Discovering @Params, part 1:
            //   For each parameter, walk the type hierarchy up to discover inherited @Param fields in @State objects.
            ExecutableElement execMethod = (ExecutableElement) method;
            for (VariableElement element : execMethod.getParameters()) {
                TypeElement walk = (TypeElement) processingEnv.getTypeUtils().asElement(element.asType());
                do {
                    for (VariableElement ve : ElementFilter.fieldsIn(walk.getEnclosedElements())) {
                        if (ve.getAnnotation(Param.class) != null) {
                            group.addParam(ve.getSimpleName().toString(), ve.getAnnotation(Param.class).value());
                        }
                    }
                } while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
            }

            // Discovering @Params, part 2:
            //  Walk the type hierarchy up to discover inherited @Param fields for class.
            TypeElement walk = clazz;
            do {
                for (VariableElement ve : ElementFilter.fieldsIn(walk.getEnclosedElements())) {
                    if (ve.getAnnotation(Param.class) != null) {
                        group.addParam(ve.getSimpleName().toString(), ve.getAnnotation(Param.class).value());
                    }
                }
            } while ((walk = (TypeElement) processingEnv.getTypeUtils().asElement(walk.getSuperclass())) != null);
        }

        // enforce the default value
        for (MethodGroup group : result.values()) {
            if (group.getModes().isEmpty()) {
                group.addModes(Mode.Throughput);
            }
        }

        String sourcePackage = AnnUtils.getPackageName(clazz);
        String generatedPackageName = sourcePackage + ".generated";
        String generatedClassName = AnnUtils.getNestedName(clazz);

        BenchmarkInfo info = new BenchmarkInfo(clazz.getQualifiedName().toString(), generatedPackageName, generatedClassName, result);
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
     *
     * @param clazz
     */
    private void generateClass(TypeElement clazz, BenchmarkInfo info) {
        try {
            // Create file and open an outputstream
            JavaFileObject jof = processingEnv.getFiler().createSourceFile(info.generatedName, clazz);
            PrintWriter writer = new PrintWriter(jof.openWriter(), false);

            // Write package and imports
            writer.println("package " + info.generatedPackageName + ';');
            writer.println();

            generateImport(writer);

            // Write class header
            writer.println("@" + CompilerControl.class.getSimpleName() +
                    "(" + CompilerControl.class.getSimpleName() + "." + CompilerControl.Mode.class.getSimpleName() +
                    "." + CompilerControl.Mode.DONT_INLINE + ")");
            writer.println("@Generated(\"" + GenerateMicroBenchmarkProcessor.class.getCanonicalName() + "\")");
            writer.println("public final class " + info.generatedClassName + " {");
            writer.println();
            generatePadding(writer);

            generateFields(writer);

            StateObjectHandler states = new StateObjectHandler(processingEnv);

            // benchmark instance is implicit
            states.bindImplicit(clazz, "bench", Scope.Thread);

            // default blackhole is implicit
            states.bindImplicit(processingEnv.getElementUtils().getTypeElement(BlackHole.class.getCanonicalName()), "blackhole", Scope.Thread);

            // Write all methods
            for (String groupName : info.methodGroups.keySet()) {
                for (Element method : info.methodGroups.get(groupName).methods()) {
                    // Look for method signature and figure out state bindings
                    ExecutableElement execMethod = (ExecutableElement) method;
                    for (VariableElement element : execMethod.getParameters()) {
                        TypeElement stateType = (TypeElement) processingEnv.getTypeUtils().asElement(element.asType());
                        states.bindArg(execMethod, stateType);
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
                writer.println("    " + s);
            }
            writer.println();

            // Write out the required fields
            for (String s : states.getFields()) {
                writer.println("    " + s);
            }
            writer.println();

            // Write out the required objects
            for (String s : states.getStateOverrides()) {
                writer.println("    " + s);
            }
            writer.println();

            // Finish class
            writer.println("}");
            writer.println();

            writer.close();
        } catch (IOException ex) {
            throw new GenerationException("IOException", ex, clazz);
        }
    }

    private void generateFields(PrintWriter writer) {
        // nothing here
    }

    private void generatePadding(PrintWriter writer) {
        for (int p = 0; p < 16; p++) {
            StringBuilder sb = new StringBuilder();
            sb.append("    boolean jmh_bench_pad_").append(p);
            for (int q = 1; q < 16; q++) {
                sb.append(", jmh_bench_pad_").append(p).append("_").append(q);
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
                BenchmarkMode.class, RawResults.class, ResultRole.class
        };

        for (Class<?> c : imports) {
            writer.println("import " + c.getName() + ';');
        }
        writer.println();
    }

     /**
     * Generate the method for a specific benchmark method
     *
     * @param benchmarkKind
     * @param writer
     * @param methodGroup
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
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");
        writer.println();

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop(control, res, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

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
            writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.PRIMARY, \"" + method.getSimpleName() + "\", res.getOperations(), res.getTime(), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + method.getSimpleName() + "\", res.getOperations(), res.getTime(), tu));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new ThroughputResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            String methodName = method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop";
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") + " void " + methodName + "(InfraControl control, RawResults result, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        long operations = 0;");
            writer.println("        long realTime = 0;");
            writer.println("        result.startTime = System.nanoTime();");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println("            operations++;");
            writer.println("        } while(!control.isDone);");
            writer.println("        result.stopTime = System.nanoTime();");
            writer.println("        result.realTime = realTime;");
            writer.println("        result.operations = operations;");
            writer.println("    }");
            writer.println();
        }
    }

    private void generateAverageTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop(control, res, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

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
            writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.PRIMARY, \"" + method.getSimpleName() + "\", res.getOperations(), res.getTime(), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + method.getSimpleName() + "\", res.getOperations(), res.getTime(), tu));");
            }
            for (String ops : states.getAuxResultNames(method)) {
                writer.println(ident(3) + "results.add(new AverageTimeResult(ResultRole.SECONDARY, \"" + ops + "\", " + states.getAuxResultAccessor(method, ops) + ", res.getTime(), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") +  " void " + method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop(InfraControl control, RawResults result, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        long operations = 0;");
            writer.println("        long realTime = 0;");
            writer.println("        result.startTime = System.nanoTime();");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println("            operations++;");
            writer.println("        } while(!control.isDone);");
            writer.println("        result.stopTime = System.nanoTime();");
            writer.println("        result.realTime = realTime;");
            writer.println("        result.operations = operations;");
            writer.println("    }");
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
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");
        writer.println();

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop(control, buffer, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

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
            writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.PRIMARY, \"" + method.getSimpleName() + "\", buffer, tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SampleTimeResult(ResultRole.SECONDARY, \"" + method.getSimpleName() + "\", buffer, tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") + " void " + method.getSimpleName() + "_" + benchmarkKind + "_measurementLoop(InfraControl control, SampleBuffer buffer, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        long realTime = 0;");
            writer.println("        int rnd = (int)System.nanoTime();");
            writer.println("        int rndMask = 0;");
            writer.println("        long time = 0;");
            writer.println("        int currentStride = 0;");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);

            writer.println("            rnd = (rnd * 1664525 + 1013904223);");
            writer.println("            boolean sample = (rnd & rndMask) == 0;");
            writer.println("            if (sample) {");
            writer.println("                time = System.nanoTime();");
            writer.println("            }");
            writer.println("            " + emitCall(method, states) + ';');
            writer.println("            if (sample) {");
            writer.println("                buffer.add(System.nanoTime() - time);");
            writer.println("                if (currentStride++ > 1000000) {");
            writer.println("                    buffer.half();");
            writer.println("                    currentStride = 0;");
            writer.println("                    rndMask = (rndMask << 1) + 1;");
            writer.println("                }");
            writer.println("            }");

            invocationEpilog(writer, 3, method, states, true);

            writer.println("        } while(!control.isDone);");

            writer.println("    }");
            writer.println();
        }
    }

    private void generateSingleShotTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        writer.println(ident(2) + "long realTime = 0;");

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.PRIMARY, \"" + method.getSimpleName() + "\", (realTime > 0) ? realTime : (time2 - time1), tu));");
            if (!isSingleMethod) {
                writer.println(ident(3) + "results.add(new SingleShotResult(ResultRole.SECONDARY, \"" + method.getSimpleName() + "\", (realTime > 0) ? realTime : (time2 - time1), tu));");
            }
            writer.println(ident(3) + "return results;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");
    }

    private void invocationProlog(PrintWriter writer, int prefix, Element method, StateObjectHandler states, boolean pauseMeasurement) {
        if (!states.getInvocationSetups(method).isEmpty()) {
            for (String s : states.getInvocationSetups(method))
                writer.println(ident(prefix) + s);
            if (pauseMeasurement)
                writer.println(ident(prefix) + "long rt = System.nanoTime();");
            writer.println();
        }
    }

    private void invocationEpilog(PrintWriter writer, int prefix, Element method, StateObjectHandler states, boolean pauseMeasurement) {
        if (!states.getInvocationTearDowns(method).isEmpty()) {
            writer.println();
            if (pauseMeasurement)
                writer.println(ident(prefix) + "realTime += (System.nanoTime() - rt);");
            for (String s : states.getInvocationTearDowns(method))
                writer.println(ident(prefix) + s);
            writer.println();
        }
    }

    private void iterationProlog(PrintWriter writer, int prefix, Element method, StateObjectHandler states) {
        for (String s : states.getStateGetters(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "control.preSetup();");

        for (String s : states.getIterationSetups(method)) writer.println(ident(prefix) + s);
        writer.println();
    }

    private void iterationEpilog(PrintWriter writer, int prefix, Element method, StateObjectHandler states) {
        writer.println(ident(prefix) + "control.preTearDown();");

        for (String s : states.getIterationTearDowns(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "if (control.isLastIteration()) {");
        for (String s : states.getRunTearDowns(method)) writer.println(ident(prefix + 1) + s);
        for (String s : states.getStateDestructors(method)) writer.println(ident(prefix + 1) + s);
        writer.println(ident(prefix) + "}");
    }

    private String emitCall(Element method, StateObjectHandler states) {
        ExecutableElement element = (ExecutableElement) method;
        if ("void".equalsIgnoreCase(element.getReturnType().toString())) {
            return states.getImplicit("bench").localIdentifier + "." + method.getSimpleName() + "(" + states.getArgList(method) + ")";
        } else {
            return states.getImplicit("blackhole").localIdentifier + ".consume(" + states.getImplicit("bench").localIdentifier + "." + method.getSimpleName() + "(" + states.getArgList(method) + "))";
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
