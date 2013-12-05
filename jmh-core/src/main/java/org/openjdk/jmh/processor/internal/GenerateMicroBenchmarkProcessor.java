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
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
import org.openjdk.jmh.runner.MicroBenchmarkList;
import org.openjdk.jmh.util.AnnotationUtils;
import org.openjdk.jmh.util.internal.CollectionUtils;
import org.openjdk.jmh.util.internal.SampleBuffer;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author staffan.friberg@oracle.com
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GenerateMicroBenchmarkProcessor extends AbstractProcessor {

    private final Set<BenchmarkInfo> benchmarkInfos = new HashSet<BenchmarkInfo>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateMicroBenchmark.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                for (TypeElement annotation : annotations) {
                    // Build a Set of classes with a list of annotated methods
                    Map<TypeElement, Set<? extends Element>> clazzes = buildAnnotatedSet(annotation, roundEnv);

                    // Generate code for all found Classes and Methods
                    for (Map.Entry<TypeElement, Set<? extends Element>> typeElementSetEntry : clazzes.entrySet()) {
                        TypeElement clazz = typeElementSetEntry.getKey();
                        try {
                            BenchmarkInfo info = validateAndSplit(clazz, typeElementSetEntry.getValue());
                            generateClass(clazz, info);
                            benchmarkInfos.add(info);
                        } catch (GenerationException ge) {
                            processingEnv.getMessager().printMessage(Kind.ERROR, ge.getMessage(), ge.getElement());
                        }
                    }
                }
            } else {
                // Processing completed, final round. Print all added methods to file
                try {
                    FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                            MicroBenchmarkList.MICROBENCHMARK_LIST.substring(1));
                    PrintWriter writer = new PrintWriter(file.openWriter());
                    for (BenchmarkInfo info : benchmarkInfos) {
                        for (String method : info.methodGroups.keySet()) {
                            MethodGroup group = info.methodGroups.get(method);
                            for (Mode m : group.getModes()) {
                                writer.println(info.userName + "." + method + ", " + info.generatedName + "." + method + ", " + m + ", " + group.getThreads());
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

    /**
     * Build a set of Classes which has annotated methods in them
     *
     * @param te
     * @param roundEnv
     * @return
     */
    private Map<TypeElement, Set<? extends Element>> buildAnnotatedSet(TypeElement te, RoundEnvironment roundEnv) {
        Map<TypeElement, Set<? extends Element>> result = new HashMap<TypeElement, Set<? extends Element>>();
        for (Element method : roundEnv.getElementsAnnotatedWith(te)) {
            TypeElement teClass = processingEnv.getElementUtils().getTypeElement(method.getEnclosingElement().toString());
            if (result.get(teClass) == null) {
                Set<Element> set = new LinkedHashSet<Element>();
                for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
                    if (element.getEnclosingElement().equals(teClass)) {
                        set.add(element);
                    }
                }

                if (!teClass.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
                    result.put(teClass, set);
                }
            }
        }
        return result;
    }

    /**
     * Do benchmark method validation and split methods set to set's per each benchmark kind.
     * Result sets may intersect.
     *
     * @param methods
     * @return
     */
    private BenchmarkInfo validateAndSplit(TypeElement clazz, Set<? extends Element> methods) {
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

        for (Element m : methods) {
            if (m.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                        + " method can not be abstract.", m);
            }
            if (m.getModifiers().contains(Modifier.PRIVATE)) {
                throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                        + " method can not be private.", m);
            }
            if (m.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                if (clazz.getAnnotation(State.class) == null) {
                    throw new GenerationException("@" + GenerateMicroBenchmark.class.getSimpleName()
                            + " method can only be synchronized if the enclosing class is annotated with "
                            + "@" + State.class.getSimpleName() + ".", m);
                    }
            }
        }

        Map<String, MethodGroup> result = new TreeMap<String, MethodGroup>();

        boolean classStrictFP = clazz.getModifiers().contains(Modifier.STRICTFP);

        for (Element method : methods) {
            validateSignature(clazz, method);

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

            BenchmarkMode mbAn = method.getAnnotation(BenchmarkMode.class);
            if (mbAn != null) {
                group.addModes(mbAn.value());
            } else {
                mbAn = method.getEnclosingElement().getAnnotation(BenchmarkMode.class);
                if (mbAn != null) {
                    group.addModes(mbAn.value());
                }
            }

            group.addStrictFP(classStrictFP);
            group.addStrictFP(methodStrictFP);
            group.addMethod(method, (method.getAnnotation(GroupThreads.class) != null) ? method.getAnnotation(GroupThreads.class).value() : 1);
        }

        // enforce the default value
        for (MethodGroup group : result.values()) {
            if (group.getModes().isEmpty()) {
                group.addModes(Mode.Throughput);
            }
        }

        // check the @Group preconditions,
        // ban some of the surprising configurations
        //
        for (MethodGroup group : result.values()) {
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

        String sourcePackage = packageName(clazz);
        if (sourcePackage.isEmpty()) {
            throw new GenerationException("Microbenchmark should have package other than default.", clazz);
        }

        // Build package name and class name for the Class to generate
        String generatedPackageName = sourcePackage + ".generated";
        String generatedClassName = clazz.getSimpleName().toString();

        return new BenchmarkInfo(clazz.getQualifiedName().toString(), generatedPackageName, generatedClassName, result);
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
                    // Final checks...
                    verifyAnnotations(method);

                    // Look for method signature and figure out state bindings
                    ExecutableElement execMethod = (ExecutableElement) method;
                    for (VariableElement element : execMethod.getParameters()) {
                        TypeElement stateType = (TypeElement) processingEnv.getTypeUtils().asElement(element.asType());
                        verifyState(stateType);
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

    private void verifyState(TypeElement type) {
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            throw new GenerationException(
                    "The " + State.class.getSimpleName() + " annotation only supports public classes.",
                    type);
        }
        if (type.getNestingKind().isNested() && !type.getModifiers().contains(Modifier.STATIC)) {
            throw new GenerationException(
                    "The " + State.class.getSimpleName()
                            + " annotation does not support inner classes, make sure the class is nested (static).",
                    type);
        }

        boolean hasDefaultConstructor = false;
        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            hasDefaultConstructor |= (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC));
        }

        if (!hasDefaultConstructor) {
            throw new GenerationException(
                    "The " + State.class.getSimpleName()
                            + " annotation can only be applied to the classes having the default public constructor.",
                    type);
        }
    }

    private void generateFields(PrintWriter writer) {
        // nothing here
    }

    private void validateSignature(TypeElement clazz, Element method) {
        if (!(method instanceof ExecutableElement)
                || !validMethodSignature((ExecutableElement) method)) {
            throw new GenerationException(
                    "The " + GenerateMicroBenchmark.class.getSimpleName()
                            + " annotation only supports methods with @State-bearing typed parameters.",
                    method);
        }
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
        writer.println("import " + List.class.getName() + ';');
        writer.println("import " + AtomicInteger.class.getName() + ';');
        writer.println("import " + AtomicIntegerFieldUpdater.class.getName() + ';');
        writer.println("import " + Collection.class.getName() + ';');
        writer.println("import " + Collections.class.getName() + ';');
        writer.println("import " + ArrayList.class.getName() + ';');
        writer.println("import " + Arrays.class.getName() + ';');
        writer.println("import " + TimeUnit.class.getName() + ';');
        writer.println("import " + Generated.class.getName() + ';');
        writer.println("import " + CompilerControl.class.getName() + ';');
        writer.println();
        writer.println("import " + InfraControl.class.getName() + ';');
        writer.println("import " + ThreadControl.class.getName() + ';');
        writer.println("import " + BlackHole.class.getName() + ';');
        writer.println("import " + Result.class.getName() + ';');
        writer.println("import " + ThroughputResult.class.getName() + ';');
        writer.println("import " + AverageTimeResult.class.getName() + ';');
        writer.println("import " + SampleTimeResult.class.getName() + ';');
        writer.println("import " + SingleShotResult.class.getName() + ';');
        writer.println("import " + SampleBuffer.class.getName() + ';');
        writer.println("import " + Mode.class.getName() + ';');
        writer.println("import " + Fork.class.getName() + ';');
        writer.println("import " + Measurement.class.getName() + ';');
        writer.println("import " + Threads.class.getName() + ';');
        writer.println("import " + Warmup.class.getName() + ';');
        writer.println("import " + BenchmarkMode.class.getName() + ';');
        writer.println("import " + RawResults.class.getName() + ';');
        writer.println("import " + ResultRole.class.getName() + ';');
        writer.println();
    }

    /**
     * Check that the method signature is correct for GenerateMicrobenchmark methods
     *
     * @param element The annotated method
     * @return True iff the method has the correct signature
     */
    public boolean validMethodSignature(ExecutableElement element) {
        Types typeUtils = processingEnv.getTypeUtils();
        for (VariableElement var : element.getParameters()) {
            if (typeUtils.asElement(var.asType()).getAnnotation(State.class) == null) {
                return false;
            }
        }
        return true;
    }


    /**
     * Get the package name part of a class
     *
     * @param clazz
     * @return the package name or "" if no package
     */
    private static String packageName(TypeElement clazz) {
        String fullName = clazz.getQualifiedName().toString();
        int index = fullName.lastIndexOf('.');

        if (index > 0) {
            return fullName.substring(0, index);
        }

        return "";
    }


    private TimeUnit findTimeUnit(MethodGroup methodGroup) {
        OutputTimeUnit ann = methodGroup.methods().iterator().next().getEnclosingElement().getAnnotation(OutputTimeUnit.class);
        for (Element method : methodGroup.methods()) {
            ann = guardedSet(ann, method.getAnnotation(OutputTimeUnit.class), method);
        }

        if (ann == null) {
            try {
                java.lang.reflect.Method value = OutputTimeUnit.class.getMethod("value");
                return (TimeUnit) value.getDefaultValue();
            } catch (NoSuchMethodException e) {
                throw new AssertionError("Shouldn't be here");
            } catch (ClassCastException e) {
                throw new AssertionError("Shouldn't be here");
            }
        } else {
            return ann.value();
        }
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
        for (String ann : generateMethodAnnotations(methodGroup)) {
            writer.println("    " + ann);
        }
        final TimeUnit timeUnit = findTimeUnit(methodGroup);
        switch (benchmarkKind) {
            case Throughput:
                generateThroughput(writer, benchmarkKind, methodGroup, getOperationsPerInvocation(methodGroup), timeUnit, states);
                break;
            case AverageTime:
                generateAverageTime(writer, benchmarkKind, methodGroup, getOperationsPerInvocation(methodGroup), timeUnit, states);
                break;
            case SampleTime:
                generateSampleTime(writer, benchmarkKind, methodGroup, timeUnit, states);
                break;
            case SingleShotTime:
                generateSingleShotTime(writer, benchmarkKind, methodGroup, timeUnit, states);
                break;
            default:
                throw new AssertionError("Shouldn't be here");
        }
    }

    private long getOperationsPerInvocation(MethodGroup methodGroup) {
        OperationsPerInvocation ann = null;
        for (Element method : methodGroup.methods()) {
            OperationsPerInvocation operationsPerInvocation = method.getAnnotation(OperationsPerInvocation.class);
            if (operationsPerInvocation != null && operationsPerInvocation.value() > 1) {
                ann = guardedSet(ann, operationsPerInvocation, method);
            }

            ann = guardedSet(ann, method.getEnclosingElement().getAnnotation(OperationsPerInvocation.class), method);
        }
        return (ann != null) ? ann.value() : 1;
    }

    /**
     * Verifying that all annotations data is valid
     *
     * @param method
     */
    private void verifyAnnotations(Element method) {
        OperationsPerInvocation operationsPerInvocation = method.getAnnotation(OperationsPerInvocation.class);
        if (operationsPerInvocation != null && operationsPerInvocation.value() < 1) {
            throw new GenerationException(
                    "The " + OperationsPerInvocation.class.getSimpleName()
                            + " needs to be greater than 0.",
                    method);
        }
        if (!method.getModifiers().contains(Modifier.PUBLIC) && !method.getModifiers().contains(Modifier.PROTECTED)) {
            throw new GenerationException(
                    "@" + GenerateMicroBenchmark.class.getSimpleName() + " method should be public or protected.",
                    method);
        }
    }


    private static String annotationMapToString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        boolean hasOptions = false;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (hasOptions) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(" = ").append(e.getValue());
            hasOptions = true;
        }
        return sb.toString();
    }

    private static Map<String, String> warmupToMap(Map<String, String> map, Warmup wAnnotation) {
        if (wAnnotation != null) {
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, wAnnotation.iterations() >= 0, "iterations", Integer.toString(wAnnotation.iterations()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, wAnnotation.time() >= 0L, "time", String.valueOf(wAnnotation.time()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, wAnnotation.timeUnit() != null, "timeUnit", "TimeUnit." + String.valueOf(wAnnotation.timeUnit()));
        }
        return map;
    }

    private static String generateWarmupAnnotation(Element method) {
        Map<String, String> map = warmupToMap(null, method.getAnnotation(Warmup.class));
        map = warmupToMap(map, method.getEnclosingElement().getAnnotation(Warmup.class));
        if (map != null && !map.isEmpty()) {
            return "@" + Warmup.class.getSimpleName() + "(" + annotationMapToString(map) + ")";
        }
        return null;
    }

    private static Map<String, String> measurementToMap(Map<String, String> map, Measurement mAnnotation) {
        if (mAnnotation != null) {
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, mAnnotation.iterations() >= 0, "iterations", Integer.toString(mAnnotation.iterations()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, mAnnotation.time() >= 0L, "time", String.valueOf(mAnnotation.time()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, mAnnotation.timeUnit() != null, "timeUnit", "TimeUnit." + String.valueOf(mAnnotation.timeUnit()));
        }
        return map;
    }

    private static String generateMeasurementAnnotation(Element method) {
        Map<String, String> map = measurementToMap(null, method.getAnnotation(Measurement.class));
        map = measurementToMap(map, method.getEnclosingElement().getAnnotation(Measurement.class));
        if (map != null && !map.isEmpty()) {
            return "@" + Measurement.class.getSimpleName() + "(" + annotationMapToString(map) + ")";
        }
        return null;
    }

    private static int getThreads(Element method) {
        Threads tAnnotation = method.getAnnotation(Threads.class);
        if (tAnnotation != null && tAnnotation.value() > Integer.MIN_VALUE) {
            return tAnnotation.value();
        }
        tAnnotation = method.getEnclosingElement().getAnnotation(Threads.class);
        if (tAnnotation != null && tAnnotation.value() > Integer.MIN_VALUE) {
            return tAnnotation.value();
        }
        return 1;
    }

    private static Map<String, String> forkToMap(Map<String, String> map, Fork fAnnotation) {
        if (fAnnotation != null) {
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsentAndQuote(map, AnnotationUtils.isSet(fAnnotation.jvmArgs()),        "jvmArgs", fAnnotation.jvmArgs());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsentAndQuote(map, AnnotationUtils.isSet(fAnnotation.jvmArgsAppend()),  "jvmArgsAppend", fAnnotation.jvmArgsAppend());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsentAndQuote(map, AnnotationUtils.isSet(fAnnotation.jvmArgsPrepend()), "jvmArgsPrepend", fAnnotation.jvmArgsPrepend());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, fAnnotation.value() != 1, "value", Integer.toString(fAnnotation.value()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, fAnnotation.warmups() > 0, "warmups", Integer.toString(fAnnotation.warmups()));
        }
        return map;
    }

    private static String generateForkAnnotation(Element method) {
        Fork forkAnnotation = method.getAnnotation(Fork.class);
        Fork upperForkAnnotation = method.getEnclosingElement().getAnnotation(Fork.class);
        if (forkAnnotation != null || upperForkAnnotation != null) {
            Map<String, String> map = forkToMap(null, forkAnnotation);
            map = forkToMap(map, upperForkAnnotation);
            if (map == null || map.isEmpty()) {
                return "@" + Fork.class.getSimpleName();
            }
            return "@" + Fork.class.getSimpleName() + "(" + annotationMapToString(map) + ")";
        }
        return null;
    }

    private List<String> generateMethodAnnotations(MethodGroup methodGroup) {
        int totalThreads = 0;
        String warmupAnn = null;
        String measurementAnn = null;
        String forkAnn = null;

        for (Element method : methodGroup.methods()) {
            totalThreads += getThreads(method);
            warmupAnn = guardedSet(warmupAnn, generateWarmupAnnotation(method), method);
            measurementAnn = guardedSet(measurementAnn, generateMeasurementAnnotation(method), method);
            forkAnn = guardedSet(forkAnn, generateForkAnnotation(method), method);
        }

        List<String> annotations = new ArrayList<String>();

        if (methodGroup.methods().size() == 1) {
            // only honor this setting for non-@Group benchmarks
            annotations.add("@" + Threads.class.getSimpleName() + "(" + totalThreads + ")");
        }
        annotations = CollectionUtils.addIfNotNull(annotations, warmupAnn);
        annotations = CollectionUtils.addIfNotNull(annotations, measurementAnn);
        annotations = CollectionUtils.addIfNotNull(annotations, forkAnn);
        return annotations;
    }

    private <T> T guardedSet(T prev, T cur, Element element) {
        if (prev == null) {
            return cur;
        } else {
            if (cur == null || prev.equals(cur)) {
                return prev;
            } else {
                throw new GenerationException("Colliding annotations: " + prev + " vs. " + cur, element);
            }
        }
    }

    private void generateThroughput(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, long opsPerInv, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");
        writer.println();

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + "RawResults res = new RawResults(" + opsPerInv + "L);");
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
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + timeUnit + ";");
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

    private void generateAverageTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, long opsPerInv, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + "RawResults res = new RawResults(" + opsPerInv + "L);");
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
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + timeUnit + ";");
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

    private void generateSampleTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");
        writer.println();

        methodProlog(writer, methodGroup);

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
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
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + timeUnit + ";");
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
            writer.println("        long rnd = System.nanoTime();");
            writer.println("        long rndMask = 0;");
            writer.println("        long time = 0;");
            writer.println("        int currentStride = 0;");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);

            writer.println("            rnd = rnd * 6364136223846793005L + 1442695040888963407L;");
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

    private void generateSingleShotTime(PrintWriter writer, Mode benchmarkKind, MethodGroup methodGroup, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Collection<? extends Result> " + methodGroup.getName() + "_" + benchmarkKind + "(InfraControl control, ThreadControl threadControl) throws Throwable {");

        methodProlog(writer, methodGroup);

        writer.println(ident(2) + "long realTime = 0;");

        boolean isSingleMethod = (methodGroup.methods().size() == 1);
        int subGroup = -1;
        for (Element method : methodGroup.methods()) {
            subGroup++;

            writer.println(ident(2) + "if (threadControl.subgroup == " + subGroup + ") {");

            iterationProlog(writer, 3, method, states);

            invocationProlog(writer, 3, method, states, false);

            writer.println(ident(3) + "long time1 = System.nanoTime();");
            writer.println(ident(3) + emitCall(method, states) + ';');
            writer.println(ident(3) + "long time2 = System.nanoTime();");

            invocationEpilog(writer, 3, method, states, false);

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "Collection<Result> results = new ArrayList<Result>();");
            writer.println(ident(3) + "TimeUnit tu = (control.timeUnit != null) ? control.timeUnit : TimeUnit." + timeUnit + ";");
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
