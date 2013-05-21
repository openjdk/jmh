/**
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.logic.BlackHole;
import org.openjdk.jmh.logic.Loop;
import org.openjdk.jmh.logic.results.AverageTimePerOp;
import org.openjdk.jmh.logic.results.OpsPerTimeUnit;
import org.openjdk.jmh.logic.results.RawResultPair;
import org.openjdk.jmh.logic.results.Result;
import org.openjdk.jmh.logic.results.SampleTimePerOp;
import org.openjdk.jmh.logic.results.SingleShotTime;
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
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author staffan.friberg@oracle.com
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GenerateMicroBenchmarkProcessor extends AbstractProcessor {

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
                        generateClasses(typeElementSetEntry.getKey(), typeElementSetEntry.getValue());
                    }
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
    private Map<BenchmarkType, Map<String, MethodGroup>> validateAndSplit(TypeElement clazz, Set<? extends Element> methods) {
        // validate against rogue fields
        if (clazz.getAnnotation(State.class) == null || clazz.getModifiers().contains(Modifier.ABSTRACT)) {
            for (VariableElement field : ElementFilter.fieldsIn(clazz.getEnclosedElements())) {
                // allow static fields
                if (field.getModifiers().contains(Modifier.STATIC)) continue;

                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Field \"" + field + "\" is declared within " +
                                "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                "This can result in unspecified behavior, and prohibited.",
                        field);
            }
        }

        Map<BenchmarkType, Map<String, MethodGroup>> result = new EnumMap<BenchmarkType, Map<String, MethodGroup>>(BenchmarkType.class);
        for (BenchmarkType b : BenchmarkType.values()) {
            result.put(b, new TreeMap<String, MethodGroup>());
        }

        boolean classStrictFP = clazz.getModifiers().contains(Modifier.STRICTFP);

        for (Element method : methods) {
            validateSignature(clazz, method);
            GenerateMicroBenchmark mbAn = method.getAnnotation(GenerateMicroBenchmark.class);

            boolean methodStrictFP = method.getModifiers().contains(Modifier.STRICTFP);

            EnumSet<BenchmarkType> anns = EnumSet.noneOf(BenchmarkType.class);
            Collections.addAll(anns, mbAn.value());

            if (anns.contains(BenchmarkType.All)) {
                for (BenchmarkType type : BenchmarkType.values()) {
                    if (type == BenchmarkType.All) continue;
                    MethodGroup group = getMethodGroup(result, method, type);
                    group.addStrictFP(classStrictFP);
                    group.addStrictFP(methodStrictFP);
                    group.addMethod(method, getThreads(method));
                }
            } else {
                for (BenchmarkType type : anns) {
                    MethodGroup group = getMethodGroup(result, method, type);
                    group.addStrictFP(classStrictFP);
                    group.addStrictFP(methodStrictFP);
                    group.addMethod(method, getThreads(method));
                }
            }
        }
        return result;
    }

    private MethodGroup getMethodGroup(Map<BenchmarkType, Map<String, MethodGroup>> result, Element method, BenchmarkType type) {
        Group groupAnn = method.getAnnotation(Group.class);
        String groupName = (groupAnn != null) ? groupAnn.value() : method.getSimpleName().toString();
        Map<String, MethodGroup> groups = result.get(type);
        MethodGroup methodGroup = groups.get(groupName);
        if (methodGroup == null) {
            methodGroup = new MethodGroup(convertToJavaIdentfier(groupName));
            groups.put(groupName, methodGroup);
        }
        return methodGroup;
    }

    public static String convertToJavaIdentfier(String id) {
        char[] result = new char[id.length()];
        for (int i = 0; i < result.length; i++) {
            char c = id.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result[i] = c;
            } else {
                result[i] = '_';
            }
        }
        return String.valueOf(result);
    }

    private void generateClasses(TypeElement clazz, Set<? extends Element> methods) {
        Map<BenchmarkType, Map<String, MethodGroup>> perKind = validateAndSplit(clazz, methods);
        for (Map.Entry<BenchmarkType, Map<String, MethodGroup>> e : perKind.entrySet()) {
            if (!e.getValue().isEmpty()) {
                generateClass(e.getKey(), clazz, e.getValue());
            }
        }
    }

    private String packageNameByType(BenchmarkType bt) {
        switch (bt) {
            case OpsPerTimeUnit:
                return "throughput";
            case AverageTimePerOp:
                return "avgtime";
            case SampleTimePerOp:
                return "sampletime";
            case SingleShotTime:
                return "oneshot";
            default:
                processingEnv.getMessager().printMessage(Kind.ERROR, "Unknown type of method to process - " + bt);
                throw new AssertionError("Shouldn't be here");
        }
    }

    /**
     * Create and generate Java code for a class and it's methods
     *
     * @param benchmarkKind
     * @param clazz
     * @param methods
     */
    private void generateClass(BenchmarkType benchmarkKind, TypeElement clazz, Map<String, MethodGroup> methods) {
        try {
            String sourcePackage = packageName(clazz);
            if (sourcePackage.isEmpty()) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Microbenchmark should have package other than default (" + clazz + ")");
                return;
            }

            // Build package name and class name for the Class to generate
            String generatedPackageName = sourcePackage + ".generated." + packageNameByType(benchmarkKind);
            String generatedClassName = clazz.getSimpleName().toString();

            // Create file and open an outputstream
            JavaFileObject jof = processingEnv.getFiler().createSourceFile(generatedPackageName + "." + generatedClassName, clazz);
            PrintWriter writer = new PrintWriter(jof.openOutputStream());

            // Write package and imports
            writer.println("package " + generatedPackageName + ';');
            writer.println();

            generateImport(writer);
            // Write class header
            writer.println(generateClassAnnotation(methods.keySet()));
            writer.println("public final class " + generatedClassName + " {");
            writer.println();
            generatePadding(writer);

            generateFields(writer);

            StateObjectHandler states = new StateObjectHandler(processingEnv);

            // benchmark instance is implicit
            states.bindImplicit(clazz, "bench", Scope.Thread);

            // default blackhole is implicit
            states.bindImplicit(processingEnv.getElementUtils().getTypeElement(BlackHole.class.getCanonicalName()), "blackhole", Scope.Thread);

            // Write all methods
            for (String groupName : methods.keySet()) {
                for (Element method : methods.get(groupName).methods()) {
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

                generateMethod(benchmarkKind, writer, methods.get(groupName), states);
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
            processingEnv.getMessager().printMessage(Kind.ERROR, ex.getMessage());
        }
    }

    private void verifyState(TypeElement type) {
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + State.class.getSimpleName()
                            + " annotation only supports public classes, "
                            + type, type);
        }
        if (type.getNestingKind().isNested() && !type.getModifiers().contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + State.class.getSimpleName()
                            + " annotation does not support inner classes, "
                            + type, type);
        }

        boolean hasDefaultConstructor = false;
        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            hasDefaultConstructor |= (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC));
        }

        if (!hasDefaultConstructor) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + State.class.getSimpleName()
                            + " annotation can only be applied to the classes having the default public constructor, "
                            + type, type);
        }
    }

    private void generateFields(PrintWriter writer) {
        // nothing here
    }

    private void validateSignature(TypeElement clazz, Element method) {
        if (!(method instanceof ExecutableElement)
                || !validMethodSignature((ExecutableElement) method)) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + GenerateMicroBenchmark.class.getSimpleName()
                            + " annotation only supports methods with @State-bearing typed parameters, "
                            + clazz + '.' + method);
        }
        if (method.getAnnotation(GenerateMicroBenchmark.class).value().length == 0) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + GenerateMicroBenchmark.class.getSimpleName()
                            + " annotation should have one or more " + BenchmarkType.class.getSimpleName() + " parameters, "
                            + clazz + '.' + method);
        }
    }

    private void generatePadding(PrintWriter writer) {
        // Generate padding
        writer.println(
                "    public volatile int pad01, pad02, pad03, pad04, pad05, pad06, pad07, pad08;\n" +
                "    public volatile int pad11, pad12, pad13, pad24, pad15, pad16, pad17, pad18;\n" +
                "    public volatile int pad21, pad22, pad23, pad34, pad25, pad26, pad27, pad28;\n" +
                "    public volatile int pad31, pad32, pad33, pad44, pad35, pad36, pad37, pad38;\n" +
                "    ");
    }

    private void generateImport(PrintWriter writer) {
        writer.println("import " + List.class.getName() + ';');
        writer.println("import " + Arrays.class.getName() + ';');
        writer.println("import " + TimeUnit.class.getName() + ';');
        writer.println("import " + Generated.class.getName() + ';');
        writer.println();
        writer.println("import " + Loop.class.getName() + ';');
        writer.println("import " + BlackHole.class.getName() + ';');
        writer.println("import " + Result.class.getName() + ';');
        writer.println("import " + OpsPerTimeUnit.class.getName() + ';');
        writer.println("import " + AverageTimePerOp.class.getName() + ';');
        writer.println("import " + SampleTimePerOp.class.getName() + ';');
        writer.println("import " + SingleShotTime.class.getName() + ';');
        writer.println("import " + SampleBuffer.class.getName() + ';');
        writer.println("import " + MicroBenchmark.class.getName() + ';');
        writer.println("import " + BenchmarkType.class.getName() + ';');
        writer.println("import " + Fork.class.getName() + ';');
        writer.println("import " + Measurement.class.getName() + ';');
        writer.println("import " + Threads.class.getName() + ';');
        writer.println("import " + Warmup.class.getName() + ';');
        writer.println("import " + RawResultPair.class.getName() + ';');
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


    /**
     * Generate the annotation telling this class has been generated
     *
     * @param methods
     * @return
     */
    private static String generateClassAnnotation(Set<String> methods) {
        StringBuilder sb = new StringBuilder("@Generated({");
        for (String method : methods) {
            sb.append('"').append(method).append("\",");
        }
        sb.setCharAt(sb.length() - 1, '}');
        sb.append(')');
        return sb.toString();
    }


    private TimeUnit findTimeUnit(MethodGroup methodGroup) {
        OutputTimeUnit ann = methodGroup.methods().iterator().next().getEnclosingElement().getAnnotation(OutputTimeUnit.class);;
        for (Element method : methodGroup.methods()) {
            ann = guardedSet(ann, method.getAnnotation(OutputTimeUnit.class));
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
    private void generateMethod(BenchmarkType benchmarkKind, PrintWriter writer, MethodGroup methodGroup, StateObjectHandler states) {
        writer.println();
        for (String ann : generateMethodAnnotations(benchmarkKind, methodGroup)) {
            writer.println("    " + ann);
        }
        final TimeUnit timeUnit = findTimeUnit(methodGroup);
        switch (benchmarkKind) {
            case OpsPerTimeUnit:
                generateOpsPerTimeUnit(writer, methodGroup, getOperationsPerInvocation(methodGroup), timeUnit, states);
                break;
            case AverageTimePerOp:
                generateAverageTime(writer, methodGroup, getOperationsPerInvocation(methodGroup), timeUnit, states);
                break;
            case SampleTimePerOp:
                generateTimeDistribution(writer, methodGroup, timeUnit, states);
                break;
            case SingleShotTime:
                generateSingleShot(writer, methodGroup, timeUnit, states);
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
                ann = guardedSet(ann, operationsPerInvocation);
            }
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
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "The " + OperationsPerInvocation.class.getSimpleName()
                            + " needs to be greater than 0, "
                            + method.getEnclosingElement() + '.' + method);
        }
        if (!method.getModifiers().contains(Modifier.PUBLIC) && !method.getModifiers().contains(Modifier.PROTECTED)) {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "benchmark method '" +
                            method.getEnclosingElement() + '.' + method +
                            "' should be public or protected");
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

    private static String generateWarmupAnnotation(Element method, BenchmarkType kind) {
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

    private static String generateMeasurementAnnotation(Element method, BenchmarkType kind) {
        Map<String, String> map = measurementToMap(null, method.getAnnotation(Measurement.class));
        map = measurementToMap(map, method.getEnclosingElement().getAnnotation(Measurement.class));
        if (map != null && !map.isEmpty()) {
            return "@" + Measurement.class.getSimpleName() + "(" + annotationMapToString(map) + ")";
        }
        return null;
    }

    private static String generateThreadsAnnotation(Element method, BenchmarkType kind) {
        if (kind != BenchmarkType.SingleShotTime) {
            Threads tAnnotation = method.getAnnotation(Threads.class);
            if (tAnnotation != null && tAnnotation.value() >= 0) {
                return "@" + Threads.class.getSimpleName() + "(" + tAnnotation.value() + ")";
            }
            tAnnotation = method.getEnclosingElement().getAnnotation(Threads.class);
            if (tAnnotation != null && tAnnotation.value() >= 0) {
                return "@" + Threads.class.getSimpleName() + "(" + tAnnotation.value() + ")";
            }
        }
        return null;
    }

    private static int getThreads(Element method) {
        Threads tAnnotation = method.getAnnotation(Threads.class);
        if (tAnnotation != null && tAnnotation.value() >= 0) {
            return tAnnotation.value();
        }
        tAnnotation = method.getEnclosingElement().getAnnotation(Threads.class);
        if (tAnnotation != null && tAnnotation.value() >= 0) {
            return tAnnotation.value();
        }
        return 1;
    }

    private static Map<String, String> forkToMap(Map<String, String> map, Fork fAnnotation) {
        if (fAnnotation != null) {
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, !fAnnotation.jvmArgs().trim().isEmpty(), "jvmArgs", fAnnotation.jvmArgs().trim());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, !fAnnotation.jvmArgsAppend().trim().isEmpty(), "jvmArgsAppend", fAnnotation.jvmArgsAppend().trim());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, !fAnnotation.jvmArgsPrepend().trim().isEmpty(), "jvmArgsPrepend", fAnnotation.jvmArgsPrepend().trim());
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, fAnnotation.value() != 1, "value", Integer.toString(fAnnotation.value()));
            map = CollectionUtils.conditionalPutAndCreateTreeMapIfAbsent(map, fAnnotation.warmups() > 0, "warmups", Integer.toString(fAnnotation.warmups()));
        }
        return map;
    }

    private static String generateForkAnnotation(Element method, BenchmarkType kind) {
        Fork forkAnnotation = method.getAnnotation(Fork.class);
        Fork upperForkAnnotation = method.getEnclosingElement().getAnnotation(Fork.class);
        if (forkAnnotation != null || upperForkAnnotation != null) {
            Map<String, String> map = forkToMap(null, forkAnnotation);
            map = forkToMap(map, upperForkAnnotation);
            if (map == null || map.isEmpty()) {
                return "@" + Fork.class.getSimpleName();
            }
            if (map.containsKey("value")) {
                map.put("value", map.get("value"));
            }
            if (map.containsKey("jvmArgs")) {
                map.put("jvmArgs", "\"" + map.get("jvmArgs") + "\"");
            }
            if (map.containsKey("jvmArgsAppend")) {
                map.put("jvmArgsAppend", "\"" + map.get("jvmArgsAppend") + "\"");
            }
            if (map.containsKey("jvmArgsPrepend")) {
                map.put("jvmArgsPrepend", "\"" + map.get("jvmArgsPrepend") + "\"");
            }
            return "@" + Fork.class.getSimpleName() + "(" + annotationMapToString(map) + ")";
        }
        return null;
    }

    private static String generateMBAnnotation(BenchmarkType kind) {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(MicroBenchmark.class.getSimpleName());
        if (kind != BenchmarkType.OpsPerTimeUnit) {
            sb.append("(BenchmarkType.").append(kind).append(')');
        }
        return sb.toString();
    }

    private List<String> generateMethodAnnotations(BenchmarkType kind, MethodGroup methodGroup) {
        int totalThreads = 0;
        String warmupAnn = null;
        String measurementAnn = null;
        String forkAnn = null;

        for (Element method : methodGroup.methods()) {
            totalThreads += getThreads(method);
            warmupAnn = guardedSet(warmupAnn, generateWarmupAnnotation(method, kind));
            measurementAnn = guardedSet(measurementAnn, generateMeasurementAnnotation(method, kind));
            forkAnn = guardedSet(forkAnn, generateForkAnnotation(method, kind));
        }

        List<String> annotations = new ArrayList<String>();
        annotations.add(generateMBAnnotation(kind));
        annotations.add("@" + Threads.class.getSimpleName() + "(" + totalThreads + ")");
        annotations = CollectionUtils.addIfNotNull(annotations, warmupAnn);
        annotations = CollectionUtils.addIfNotNull(annotations, measurementAnn);
        annotations = CollectionUtils.addIfNotNull(annotations, forkAnn);
        return annotations;
    }

    private <T> T guardedSet(T prev, T cur) {
        if (prev == null) {
            return cur;
        } else {
            if (cur == null || prev.equals(cur)) {
                return prev;
            } else {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Colliding annotations: " + prev + " vs. " + cur);
                return null; // unreachable anyway
            }
        }
    }

    private void generateOpsPerTimeUnit(PrintWriter writer, MethodGroup methodGroup, long opsPerInv, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Result " + methodGroup.getName() + "(Loop loop) throws Throwable { ");
        writer.println();

        methodProlog(writer, methodGroup);

        int threadTally = 0;

        for (Element method : methodGroup.methods()) {

            // determine the sibling bounds
            int threads = methodGroup.getMethodThreads(method);
            int loId = threadTally;
            int hiId = threadTally + threads;
            threadTally = hiId;

            writer.println(ident(2) + "if (" + loId + " <= siblingId && siblingId < " + hiId + ") { ");

            iterationProlog(writer, 3, method, states);


            // synchronize iterations prolog: first peeled iteration
            invocationProlog(writer, 3, method, states, false);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, false);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "loop.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmup()) {");

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
            writer.println(ident(3) + "loop.enable();");
            writer.println(ident(3) + "RawResultPair res = " + method.getSimpleName() + "_measurementLoop(loop, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "loop.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmdown()) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");

            // iteration prolog
            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "return new OpsPerTimeUnit(\"" + method.getSimpleName() + "\", res.operations, res.time, TimeUnit." + timeUnit + ");");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") + " RawResultPair " + method.getSimpleName() + "_measurementLoop(Loop loop, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        long operations = 0;");
            writer.println("        long pauseTime = 0;");
            writer.println("        long startTime = System.nanoTime();");
            writer.println("        Loop.Data ld = loop.data;");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println("            operations++;");
            writer.println("        } while(!ld.isDone);");
            writer.println("        long stopTime = System.nanoTime();");
            writer.println("        return new RawResultPair(operations * " + opsPerInv + "L, (stopTime - startTime) - pauseTime);");
            writer.println("    }");
            writer.println();
        }
    }

    private void generateAverageTime(PrintWriter writer, MethodGroup methodGroup, long opsPerInv, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Result " + methodGroup.getName() + "(Loop loop) throws Throwable { ");

        methodProlog(writer, methodGroup);

        int threadTally = 0;
        for (Element method : methodGroup.methods()) {

            // determine the sibling bounds
            int threads = methodGroup.getMethodThreads(method);
            int loId = threadTally;
            int hiId = threadTally + threads;
            threadTally = hiId;

            writer.println(ident(2) + "if (" + loId + " <= siblingId && siblingId < " + hiId + ") { ");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: first peeled iteration
            invocationProlog(writer, 3, method, states, false);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, false);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "loop.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmup()) {");

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
            writer.println(ident(3) + "loop.enable();");
            writer.println(ident(3) + "RawResultPair res = " + method.getSimpleName() + "_measurementLoop(loop, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "loop.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmdown()) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "return new AverageTimePerOp(\"" + method.getSimpleName() + "\", res.operations, res.time, TimeUnit." + timeUnit + ");");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") +  " RawResultPair " + method.getSimpleName() + "_measurementLoop(Loop loop, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        long operations = 0;");
            writer.println("        long pauseTime = 0;");
            writer.println("        long start = System.nanoTime();");
            writer.println("        Loop.Data ld = loop.data;");
            writer.println("        do {");

            invocationProlog(writer, 3, method, states, true);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, true);

            writer.println("            operations++;");
            writer.println("        } while(!ld.isDone);");
            writer.println("        long end = System.nanoTime();");
            writer.println("        return new RawResultPair(operations * " + opsPerInv + "L, (end - start) - pauseTime);");
            writer.println("    }");
            writer.println();
        }
    }

    private void methodProlog(PrintWriter writer, MethodGroup methodGroup) {
        writer.println(ident(2) + "if (!threadId_inited) {");
        writer.println(ident(2) + "    threadId = threadSelector.getAndIncrement();");
        writer.println(ident(2) + "    threadId_inited = true;");
        writer.println(ident(2) + "}");

        writer.println(ident(2) + "int groupId = threadId / " + methodGroup.getTotalThreadCount() + ";");
        writer.println(ident(2) + "int siblingId = threadId % " + methodGroup.getTotalThreadCount() + ";");
        writer.println();
    }

    private String prefix(String argList) {
        if (argList.trim().isEmpty()) {
            return "";
        } else {
            return ", " + argList;
        }
    }

    private void generateTimeDistribution(PrintWriter writer, MethodGroup methodGroup, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Result " + methodGroup.getName() + "(Loop loop) throws Throwable { ");
        writer.println();

        methodProlog(writer, methodGroup);

        int threadTally = 0;
        for (Element method : methodGroup.methods()) {
            // determine the sibling bounds
            int threads = methodGroup.getMethodThreads(method);
            int loId = threadTally;
            int hiId = threadTally + threads;
            threadTally = hiId;

            writer.println(ident(2) + "if (" + loId + " <= siblingId && siblingId < " + hiId + ") { ");

            iterationProlog(writer, 3, method, states);

            // synchronize iterations prolog: first peeled iteration
            invocationProlog(writer, 3, method, states, false);
            writer.println(ident(3) + emitCall(method, states) + ';');
            invocationEpilog(writer, 3, method, states, false);

            // synchronize iterations prolog: announce ready
            writer.println(ident(3) + "loop.announceWarmupReady();");

            // synchronize iterations prolog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmup()) {");

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
            writer.println(ident(3) + "Result res = " + method.getSimpleName() + "_measurementLoop(loop, " + states.getImplicit("bench").toLocal() + ", " + states.getImplicit("blackhole").toLocal() + prefix(states.getArgList(method)) + ");");

            // control objects get a special treatment
            for (StateObject so : states.getControls()) {
                writer.println(ident(3) + so.localIdentifier + ".stopMeasurement = true;");
            }

            // synchronize iterations epilog: announce ready
            writer.println(ident(3) + "loop.announceWarmdownReady();");

            // synchronize iterations epilog: catchup loop
            writer.println(ident(3) + "while (loop.shouldContinueWarmdown()) {");

            invocationProlog(writer, 4, method, states, false);
            writer.println(ident(4) + emitCall(method, states) + ';');
            invocationEpilog(writer, 4, method, states, false);

            writer.println(ident(3) + "}");
            writer.println();

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "return res;");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");

        writer.println(ident(1) + "}");

        // measurement loop bodies
        for (Element method : methodGroup.methods()) {
            writer.println("    public " + (methodGroup.isStrictFP() ? "strictfp" : "") + " Result " + method.getSimpleName() + "_measurementLoop(Loop loop, " + states.getImplicit("bench").toTypeDef() + ", " + states.getImplicit("blackhole").toTypeDef() + prefix(states.getTypeArgList(method)) + ") throws Throwable {");
            writer.println("        SampleBuffer buffer = new SampleBuffer();");
            writer.println("        long pauseTime = 0;");
            writer.println("        Loop.Data ld = loop.data;");
            writer.println("        long rnd = System.nanoTime();");
            writer.println("        long rndMask = 1;");
            writer.println("        loop.enable();");
            writer.println("        do {");

            invocationProlog(writer, 4, method, states, true);

            writer.println("            rnd = (rnd * 0x5DEECE66DL + 0xBL) & (0xFFFFFFFFFFFFL);");
            writer.println("            if ((rnd & rndMask) == 0) {");
            writer.println("                long time1 = System.nanoTime();");
            writer.println("                " + emitCall(method, states) + ';');
            writer.println("                long time2 = System.nanoTime();");
            writer.println("                boolean flipped = buffer.add(time2 - time1);");
            writer.println("                ");
            writer.println("                if (flipped) {");
            writer.println("                    if (rndMask != 0xFFFFFFFFFFFFL) {");
            writer.println("                        rndMask = (rndMask << 1) + 1;");
            writer.println("                    }");
            writer.println("                }");
            writer.println("            } else { ");
            writer.println("                " + emitCall(method, states) + ';');
            writer.println("            }");

            invocationEpilog(writer, 4, method, states, true);

            writer.println("        } while(!ld.isDone);");
            writer.println("        return new SampleTimePerOp(\"" + method.getSimpleName() + "\", buffer, TimeUnit." + timeUnit + ");");
            writer.println("    }");
            writer.println();
        }
    }

    private void generateSingleShot(PrintWriter writer, MethodGroup methodGroup, TimeUnit timeUnit, StateObjectHandler states) {
        writer.println(ident(1) + "public Result " + methodGroup.getName() + "(Loop loop) throws Throwable { ");

        methodProlog(writer, methodGroup);

        writer.println(ident(2) + "long pauseTime = 0;");

        int threadTally = 0;
        for (Element method : methodGroup.methods()) {

            // determine the sibling bounds
            int threads = methodGroup.getMethodThreads(method);
            int loId = threadTally;
            int hiId = threadTally + threads;
            threadTally = hiId;

            writer.println(ident(2) + "if (" + loId + " <= siblingId && siblingId < " + hiId + ") { ");

            iterationProlog(writer, 3, method, states);

            invocationProlog(writer, 3, method, states, false);

            writer.println(ident(3) + "long time1 = System.nanoTime();");
            writer.println(ident(3) + emitCall(method, states) + ';');
            writer.println(ident(3) + "long time2 = System.nanoTime();");

            invocationEpilog(writer, 3, method, states, false);

            iterationEpilog(writer, 3, method, states);

            writer.println(ident(3) + "return new SingleShotTime(\"" + method.getSimpleName() + "\",  (time2 - time1) - pauseTime, TimeUnit." + timeUnit + ");");
            writer.println(ident(2) + "} else");
        }
        writer.println(ident(3) + "throw new IllegalStateException(\"Harness failed to distribute threads among groups properly\");");
        writer.println();

        writer.println(ident(1) + "}");
    }

    private void invocationProlog(PrintWriter writer, int prefix, Element method, StateObjectHandler states, boolean pauseMeasurement) {
        if (!states.getInvocationSetups(method).isEmpty()) {
            if (pauseMeasurement)
                writer.println(ident(prefix) + "long ptS = System.nanoTime();");
            for (String s : states.getInvocationSetups(method))
                writer.println(ident(prefix) + s);
            if (pauseMeasurement)
                writer.println(ident(prefix) + "pauseTime += (System.nanoTime() - ptS);");
            writer.println();
        }
    }

    private void invocationEpilog(PrintWriter writer, int prefix, Element method, StateObjectHandler states, boolean pauseMeasurement) {
        if (!states.getInvocationTearDowns(method).isEmpty()) {
            writer.println();
            if (pauseMeasurement)
                writer.println(ident(prefix) + "long ptT = System.nanoTime();");
            for (String s : states.getInvocationTearDowns(method))
                writer.println(ident(prefix) + s);
            if (pauseMeasurement)
                writer.println(ident(prefix) + "pauseTime += (System.nanoTime() - ptT);");
            writer.println();
        }
    }

    private void iterationProlog(PrintWriter writer, int prefix, Element method, StateObjectHandler states) {
        for (String s : states.getStateGetters(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "loop.preSetup();");

        for (String s : states.getIterationSetups(method)) writer.println(ident(prefix) + s);
        writer.println();
    }

    private void iterationEpilog(PrintWriter writer, int prefix, Element method, StateObjectHandler states) {
        writer.println(ident(prefix) + "loop.preTearDown();");

        for (String s : states.getIterationTearDowns(method)) writer.println(ident(prefix) + s);
        writer.println();

        writer.println(ident(prefix) + "if (loop.isLastIteration()) {");
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
