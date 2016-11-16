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
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

class StateObjectHandler {

    private final CompilerControlPlugin compileControl;

    private final Identifiers identifiers;

    private final Multimap<String, StateObject> roots;
    private final Multimap<String, ClassInfo> specials;

    private final Set<StateObject> stateObjects;
    private final Map<String, StateObject> implicits;


    private final Multimap<String, String> benchmarkArgs;

    private final Multimap<String, String> auxNames = new HashMultimap<>();
    private final Map<String, AuxCounters.Type> auxType = new HashMap<>();
    private final Map<String, String> auxAccessors = new HashMap<>();
    private final Map<String, Boolean> auxResettable = new HashMap<>();

    public StateObjectHandler(CompilerControlPlugin compileControl) {
        this.compileControl = compileControl;
        this.roots = new HashMultimap<>();
        this.benchmarkArgs = new HashMultimap<>();
        this.implicits = new HashMap<>();
        this.specials = new HashMultimap<>();
        this.stateObjects = new HashSet<>();
        this.identifiers = new Identifiers();
    }

    public static void validateState(ClassInfo state) {
        // Because of https://bugs.openjdk.java.net/browse/JDK-8031122,
        // we need to preemptively check the annotation value, and
        // the API can only allow that by catching the exception, argh.
        try {
            State ann = BenchmarkGeneratorUtils.getAnnSuper(state, State.class);
            if (ann != null) {
                ann.value();
            }
        } catch (IncompleteAnnotationException iae) {
            throw new GenerationException("The @" + State.class.getSimpleName() +
                    " annotation should have the explicit " + Scope.class.getSimpleName() + " argument",
                    state);
        }

        if (!state.isPublic()) {
            throw new GenerationException("The instantiated @" + State.class.getSimpleName() +
                    " annotation only supports public classes.", state);
        }

        if (state.isFinal()) {
            throw new GenerationException("The instantiated @" + State.class.getSimpleName() +
                    " annotation does not support final classes. This class is not " , state);
        }

        if (state.isInner()) {
            throw new GenerationException("The instantiated @" + State.class.getSimpleName() +
                    " annotation does not support inner classes, make sure your class is static.", state);
        }

        if (state.isAbstract()) {
            throw new GenerationException("The instantiated @" + State.class.getSimpleName() +
                    " class cannot be abstract.", state);
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
            throw new GenerationException("The @" + State.class.getSimpleName() +
                    " annotation can only be applied to the classes having the default public constructor.",
                    state);
        }

        // validate rogue annotations on classes
        BenchmarkGeneratorUtils.checkAnnotations(state);
        for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(state)) {
            BenchmarkGeneratorUtils.checkAnnotations(fi);
        }

        // validate rogue annotations on methods
        for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(state)) {
            BenchmarkGeneratorUtils.checkAnnotations(mi);
        }

        // check @Setup/@TearDown have only @State arguments
        for (MethodInfo mi : BenchmarkGeneratorUtils.getAllMethods(state)) {
            if (mi.getAnnotation(Setup.class) != null || mi.getAnnotation(TearDown.class) != null) {
                validateStateArgs(mi);
            }
        }
    }

    public static void validateStateArgs(MethodInfo e) {
        for (ParameterInfo var : e.getParameters()) {
            if (BenchmarkGeneratorUtils.getAnnSuper(var.getType(), State.class) != null) continue;
            if (isSpecialClass(var.getType())) continue;

            throw new GenerationException(
                        "Method parameters should be either @" + State.class.getSimpleName() + " classes", // TODO: Change the message
                        e);
        }
    }

    private static boolean isSpecialClass(ClassInfo ci) {
        String name = ci.getQualifiedName();
        return
                name.equals(BenchmarkParams.class.getCanonicalName()) ||
                name.equals(IterationParams.class.getCanonicalName()) ||
                name.equals(ThreadParams.class.getCanonicalName()) ||
                name.equals(Blackhole.class.getCanonicalName()) ||
                name.equals(Control.class.getCanonicalName())
                ;
    }

    private String getSpecialClassAccessor(ClassInfo pci) {
        String name = pci.getQualifiedName();
        if (name.equals(BenchmarkParams.class.getCanonicalName()))  return "benchmarkParams";
        if (name.equals(IterationParams.class.getCanonicalName()))  return "iterationParams";
        if (name.equals(ThreadParams.class.getCanonicalName()))     return "threadParams";
        if (name.equals(Blackhole.class.getCanonicalName()))        return "blackhole";
        if (name.equals(Control.class.getCanonicalName()))          return "notifyControl";
        throw new GenerationException("Internal error, unhandled special class: " + pci, pci);
    }

    public State getState(ClassInfo ci, ParameterInfo pi) {
        State ann = BenchmarkGeneratorUtils.getAnnSuper(ci, State.class);
        if (ann == null) {
            throw new GenerationException("The method parameter is not a @" + State.class.getSimpleName() + ": ", pi);
        }
        return ann;
    }

    public void bindMethods(ClassInfo holder, MethodGroup mg) {
        for (MethodInfo method : mg.methods()) {
            // Bind the holder implicitly:
            {
                State ann = BenchmarkGeneratorUtils.getAnnSuper(holder, State.class);
                Scope scope = (ann != null) ? ann.value() : Scope.Thread;
                StateObject holderSo = new StateObject(identifiers, holder, scope);
                stateObjects.add(holderSo);
                implicits.put("bench", holderSo);
                bindState(method, holderSo, holder);

                resolveDependencies(method, holder, holderSo);
            }

            // Check that all arguments are states.
            validateStateArgs(method);

            // Bind all @Benchmark parameters
            for (ParameterInfo ppi : method.getParameters()) {
                ClassInfo pci = ppi.getType();

                if (isSpecialClass(pci)) {
                    benchmarkArgs.put(method.getName(), getSpecialClassAccessor(pci));
                    specials.put(method.getName(), pci);
                } else {
                    StateObject pso = new StateObject(identifiers, pci, getState(pci, ppi).value());
                    stateObjects.add(pso);
                    roots.put(method.getName(), pso);
                    benchmarkArgs.put(method.getName(), pso.toLocal());
                    bindState(method, pso, pci);

                    resolveDependencies(method, pci, pso);
                }
            }
        }
    }

    public static void validateNoCycles(MethodInfo method) {
        try {
            validateNoCyclesStep(Collections.<String>emptyList(), method, true);
        } catch (StackOverflowError e) {
            // "YOLO Engineering"
            throw new GenerationException("@" + State.class.getSimpleName() +
                    " dependency cycle is detected.", method);
        }
    }

    private static void validateNoCyclesStep(List<String> states, MethodInfo method, boolean includeHolder) {
        List<ClassInfo> stratum = new ArrayList<>();
        if (includeHolder) {
            stratum.add(method.getDeclaringClass());
        }
        for (ParameterInfo ppi : method.getParameters()) {
            stratum.add(ppi.getType());
        }

        List<String> newStates = new ArrayList<>();
        newStates.addAll(states);
        for (ClassInfo ci : stratum) {
            newStates.add(ci.getQualifiedName());
        }

        for (ClassInfo ci : stratum) {
            for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(ci)) {
                if (mi.getAnnotation(Setup.class) != null || mi.getAnnotation(TearDown.class) != null) {
                    validateNoCyclesStep(newStates, mi, false);
                }
            }
        }
    }

    /**
     * Recursively resolve if there are any other states referenced through helper methods.
     */
    private void resolveDependencies(MethodInfo method, ClassInfo pci, StateObject pso) {

        for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(pci)) {
            if (mi.getAnnotation(Setup.class) != null || mi.getAnnotation(TearDown.class) != null) {
                for (ParameterInfo pi : mi.getParameters()) {
                    ClassInfo ci = pi.getType();

                    if (isSpecialClass(ci)) {
                        pso.helperArgs.put(mi.getQualifiedName(), getSpecialClassAccessor(ci));
                        specials.put(mi.getQualifiedName(), ci);
                    } else {
                        StateObject so = new StateObject(identifiers, ci, getState(ci, pi).value());

                        if (!pso.helperArgs.get(mi.getQualifiedName()).contains(so.toLocal())) {
                            stateObjects.add(so);
                            pso.depends.add(so);
                            pso.helperArgs.put(mi.getQualifiedName(), so.toLocal());

                            bindState(method, so, ci);
                            resolveDependencies(method, ci, so);
                        }
                    }
                }
            }
        }
    }

    private void bindState(MethodInfo execMethod, StateObject so, ClassInfo ci) {
        // Check it is a valid state
        validateState(ci);

        // auxiliary result, produce the accessors
        AuxCounters auxCountAnn = ci.getAnnotation(AuxCounters.class);
        if (auxCountAnn != null) {
            if (so.scope != Scope.Thread) {
                throw new GenerationException("@" + AuxCounters.class.getSimpleName() +
                        " can only be used with " + Scope.class.getSimpleName() + "." + Scope.Thread + " states.", ci);
            }

            for (FieldInfo sub : ci.getFields()) {
                if (sub.isPublic()) {
                    if (!isAuxCompatible(sub.getType().getQualifiedName())) {
                        throw new GenerationException("Illegal type for the public field in @" + AuxCounters.class.getSimpleName() + ".", sub);
                    }
                    String name = sub.getName();
                    String meth = execMethod.getName();
                    auxNames.put(meth, name);
                    auxType.put(name, auxCountAnn.value());
                    auxResettable.put(name, true);
                    String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name);
                    if (prev != null) {
                        throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                " injected into this method.", sub);
                    }
                }
            }

            for (MethodInfo sub : ci.getMethods()) {
                if (sub.isPublic() && !sub.getReturnType().equals("void")) {
                    if (!isAuxCompatible(sub.getReturnType())) {
                        throw new GenerationException("Illegal type for the return type of public method in @" + AuxCounters.class.getSimpleName() + ".", sub);
                    }

                    String name = sub.getName();
                    String meth = execMethod.getName();
                    auxNames.put(meth, name);
                    auxType.put(name, auxCountAnn.value());
                    auxResettable.put(name, false);
                    String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name + "()");
                    if (prev != null) {
                        throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                " injected into this method.", sub);
                    }
                }
            }
        }

        // walk the type hierarchy up to discover inherited @Params
        for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(ci)) {
            if (fi.getAnnotation(Param.class) != null) {
                checkParam(fi);
                so.addParam(fi);
            }
        }

        // put the @State objects helper methods
        for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(ci)) {
            Setup setupAnn = mi.getAnnotation(Setup.class);
            if (setupAnn != null) {
                checkHelpers(mi, Setup.class);
                so.addHelper(new HelperMethodInvocation(mi, so, setupAnn.value(), HelperType.SETUP));
                compileControl.defaultForceInline(mi);
            }

            TearDown tearDownAnn = mi.getAnnotation(TearDown.class);
            if (tearDownAnn != null) {
                checkHelpers(mi, TearDown.class);
                so.addHelper(new HelperMethodInvocation(mi, so, tearDownAnn.value(), HelperType.TEARDOWN));
                compileControl.defaultForceInline(mi);
            }
        }
    }

    private boolean isAuxCompatible(String typeName) {
        if (typeName.equals("byte")     || typeName.equals("java.lang.Byte")) return true;
        if (typeName.equals("short")    || typeName.equals("java.lang.Short")) return true;
        if (typeName.equals("int")      || typeName.equals("java.lang.Integer")) return true;
        if (typeName.equals("float")    || typeName.equals("java.lang.Float")) return true;
        if (typeName.equals("long")     || typeName.equals("java.lang.Long")) return true;
        if (typeName.equals("double")   || typeName.equals("java.lang.Double")) return true;
        return false;
    }

    private void checkParam(FieldInfo fi) {
        if (fi.isFinal()) {
            throw new GenerationException(
                    "@" + Param.class.getSimpleName() + " annotation is not acceptable on final fields.",
                    fi);
        }

        if (BenchmarkGeneratorUtils.getAnnSyntax(fi.getDeclaringClass(), State.class) == null) {
            throw new GenerationException(
                    "@" + Param.class.getSimpleName() + " annotation should be placed in @" + State.class.getSimpleName() +
                            "-annotated class.", fi);
        }

        ClassInfo type = fi.getType();

        if (!isParamTypeAcceptable(type)) {
            throw new GenerationException(
                    "@" + Param.class.getSimpleName() + " can only be placed over the annotation-compatible types:" +
                            " primitives, primitive wrappers, Strings, or enums.", fi);
        }

        String[] values = fi.getAnnotation(Param.class).value();

        if (values.length == 1 && values[0].equalsIgnoreCase(Param.BLANK_ARGS)) {
            if (!fi.getType().isEnum()) {
                throw new GenerationException(
                    "@" + Param.class.getSimpleName() + " should provide the default parameters.", fi);
            } else {
                // if type is enum then don't need to check conformity
            }
        } else {
            for (String val : values) {
                if (!isParamValueConforming(fi, val, type)) {
                    throw new GenerationException(
                            "Some @" + Param.class.getSimpleName() + " values can not be converted to target type: " +
                                    "\"" + val + "\" can not be converted to " + type,
                            fi
                    );
                }
            }
        }
    }

    private boolean isParamTypeAcceptable(ClassInfo type) {
        String typeName = type.getQualifiedName();
        if (type.isEnum()) return true;
        if (typeName.equals("java.lang.String")) return true;
        if (typeName.equals("boolean")  || typeName.equals("java.lang.Boolean")) return true;
        if (typeName.equals("byte")     || typeName.equals("java.lang.Byte")) return true;
        if (typeName.equals("char")     || typeName.equals("java.lang.Character")) return true;
        if (typeName.equals("short")    || typeName.equals("java.lang.Short")) return true;
        if (typeName.equals("int")      || typeName.equals("java.lang.Integer")) return true;
        if (typeName.equals("float")    || typeName.equals("java.lang.Float")) return true;
        if (typeName.equals("long")     || typeName.equals("java.lang.Long")) return true;
        if (typeName.equals("double")   || typeName.equals("java.lang.Double")) return true;
        return false;
    }

    private boolean isParamValueConforming(FieldInfo fi, String val, ClassInfo type) {
        String typeName = type.getQualifiedName();

        if (type.isEnum()) {
            if (type.getEnumConstants().contains(val)) {
                return true;
            }
        }

        if (typeName.equals("java.lang.String")) {
            return true;
        }
        if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
            return (val.equals("true") || val.equals("false"));
        }
        if (typeName.equals("byte") || typeName.equals("java.lang.Byte")) {
            try {
                Byte.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        if (typeName.equals("char") || typeName.equals("java.lang.Character")) {
            return (val.length() == 1);
        }
        if (typeName.equals("short") || typeName.equals("java.lang.Short")) {
            try {
                Short.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        if (typeName.equals("int") || typeName.equals("java.lang.Integer")) {
            try {
                Integer.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        if (typeName.equals("float") || typeName.equals("java.lang.Float")) {
            try {
                Float.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        if (typeName.equals("long") || typeName.equals("java.lang.Long")) {
            try {
                Long.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        if (typeName.equals("double") || typeName.equals("java.lang.Double")) {
            try {
                Double.valueOf(val);
                return true;
            } catch (NumberFormatException nfe) {
            }
        }
        return false;
    }

    private void checkHelpers(MethodInfo mi, Class<? extends Annotation> annClass) {
        // OK to have these annotation for @State objects
        if (BenchmarkGeneratorUtils.getAnnSuper(mi.getDeclaringClass(), State.class) == null) {
            if (!mi.getDeclaringClass().isAbstract()) {
                throw new GenerationException(
                        "@" + TearDown.class.getSimpleName() + " annotation is placed within " +
                                "the class not having @" + State.class.getSimpleName() + " annotation. " +
                                "This has no behavioral effect, and prohibited.",
                        mi);
            }
        }

        if (!mi.isPublic()) {
            throw new GenerationException(
                    "@" + annClass.getSimpleName() + " method should be public.",
                    mi);
        }

        if (!mi.getReturnType().equalsIgnoreCase("void")) {
            throw new GenerationException(
                    "@" + annClass.getSimpleName() + " method should not return anything.",
                    mi);
        }
    }

    public String getBenchmarkArgList(MethodInfo methodInfo) {
        return Utils.join(benchmarkArgs.get(methodInfo.getName()), ", ");
    }

    public String getArgList(MethodInfo methodInfo) {
        return getArgList(stateOrder(methodInfo, false));
    }

    public String getArgList(Collection<StateObject> sos) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (StateObject so : sos) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(so.toLocal());
            i++;
        }
        return sb.toString();
    }

    public String getTypeArgList(MethodInfo methodInfo) {
        return getTypeArgList(stateOrder(methodInfo, false));
    }

    public String getTypeArgList(Collection<StateObject> sos) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (StateObject so : sos) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(so.toTypeDef());
            i++;
        }
        return sb.toString();
    }

    @SafeVarargs
    public static Collection<StateObject> cons(Collection<StateObject>... colls) {
        SortedSet<StateObject> r = new TreeSet<>(StateObject.ID_COMPARATOR);
        for (Collection<StateObject> coll : colls) {
            r.addAll(coll);
        }
        return r;
    }

    public Collection<String> getHelperBlock(MethodInfo method, Level helperLevel, HelperType type) {

        // Look for the offending methods.
        // This will be used to skip the irrelevant blocks for state objects down the stream.
        List<StateObject> statesForward = new ArrayList<>();
        for (StateObject so : stateOrder(method, true)) {
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel == helperLevel) {
                    statesForward.add(so);
                    break;
                }
            }
        }

        List<StateObject> statesReverse = new ArrayList<>();
        for (StateObject so : stateOrder(method, false)) {
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel == helperLevel) {
                    statesReverse.add(so);
                    break;
                }
            }
        }

        List<String> result = new ArrayList<>();

        // Handle Thread object helpers
        for (StateObject so : statesForward) {
            if (so.scope != Scope.Thread) continue;

            if (type == HelperType.SETUP) {
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        Collection<String> args = so.helperArgs.get(mi.method.getQualifiedName());
                        result.add(so.localIdentifier + "." + mi.method.getName() + "(" + Utils.join(args, ",") + ");");
                    }
                }
            }
        }

        for (StateObject so : statesReverse) {
            if (so.scope != Scope.Thread) continue;

            if (type == HelperType.TEARDOWN) {
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        Collection<String> args = so.helperArgs.get(mi.method.getQualifiedName());
                        result.add(so.localIdentifier + "." + mi.method.getName() + "(" + Utils.join(args, ",") + ");");
                    }
                }
            }
        }

        // Handle Benchmark/Group object helpers
        for (StateObject so : statesForward) {
            if (so.scope != Scope.Benchmark && so.scope != Scope.Group) continue;

            if (type == HelperType.SETUP) {
                result.add("if (" + so.type + ".setup" + helperLevel + "MutexUpdater.compareAndSet(" + so.localIdentifier + ", 0, 1)) {");
                result.add("    try {");
                result.add("        if (control.isFailing) throw new FailureAssistException();");
                result.add("        if (!" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        Collection<String> args = so.helperArgs.get(mi.method.getQualifiedName());
                        result.add("            " + so.localIdentifier + "." + mi.method.getName() + "(" + Utils.join(args, ",") + ");");
                    }
                }
                result.add("            " + so.localIdentifier + ".ready" + helperLevel + " = true;");
                result.add("        }");
                result.add("    } catch (Throwable t) {");
                result.add("        control.isFailing = true;");
                result.add("        throw t;");
                result.add("    } finally {");
                result.add("        " + so.type + ".setup" + helperLevel + "MutexUpdater.set(" + so.localIdentifier + ", 0);");
                result.add("    }");
                result.add("} else {");
                result.add("    while (" + so.type + ".setup" + helperLevel + "MutexUpdater.get(" + so.localIdentifier + ") == 1) {");
                result.add("        if (control.isFailing) throw new FailureAssistException();");
                result.add("        if (Thread.interrupted()) throw new InterruptedException();");
                result.add("    }");
                result.add("}");
            }
        }

        for (StateObject so : statesReverse) {
            if (so.scope != Scope.Benchmark && so.scope != Scope.Group) continue;

            if (type == HelperType.TEARDOWN) {
                result.add("if (" + so.type + ".tear" + helperLevel + "MutexUpdater.compareAndSet(" + so.localIdentifier + ", 0, 1)) {");
                result.add("    try {");
                result.add("        if (control.isFailing) throw new FailureAssistException();");
                result.add("        if (" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        Collection<String> args = so.helperArgs.get(mi.method.getQualifiedName());
                        result.add("            " + so.localIdentifier + "." + mi.method.getName() + "(" + Utils.join(args, ",") + ");");
                    }
                }
                result.add("            " + so.localIdentifier + ".ready" + helperLevel + " = false;");
                result.add("        }");
                result.add("    } catch (Throwable t) {");
                result.add("        control.isFailing = true;");
                result.add("        throw t;");
                result.add("    } finally {");
                result.add("        " + so.type + ".tear" + helperLevel + "MutexUpdater.set(" + so.localIdentifier + ", 0);");
                result.add("    }");
                result.add("} else {");

                // We don't need to actively busy-wait for Trial, it is way past the measurement window,
                // and we would not need measurement threads anymore after this is over. Therefore, it
                // is OK to exponentially back off.
                if (helperLevel == Level.Trial) {
                    result.add("    long " + so.localIdentifier + "_backoff = 1;");
                }

                result.add("    while (" + so.type + ".tear" + helperLevel + "MutexUpdater.get(" + so.localIdentifier + ") == 1) {");

                if (helperLevel == Level.Trial) {
                    result.add("        TimeUnit.MILLISECONDS.sleep(" + so.localIdentifier + "_backoff);");
                    result.add("        " + so.localIdentifier + "_backoff = Math.max(1024, " + so.localIdentifier + "_backoff * 2);");
                }

                result.add("        if (control.isFailing) throw new FailureAssistException();");
                result.add("        if (Thread.interrupted()) throw new InterruptedException();");
                result.add("    }");
                result.add("}");
            }
        }

        return result;
    }

    public boolean hasInvocationStubs(MethodInfo method) {
        return !getInvocationSetups(method).isEmpty() || !getInvocationTearDowns(method).isEmpty();
    }

    public Collection<String> getInvocationSetups(MethodInfo method) {
        return getHelperBlock(method, Level.Invocation, HelperType.SETUP);
    }

    public Collection<String> getInvocationTearDowns(MethodInfo method) {
        return getHelperBlock(method, Level.Invocation, HelperType.TEARDOWN);
    }

    public Collection<String> getIterationSetups(MethodInfo method) {
        return getHelperBlock(method, Level.Iteration, HelperType.SETUP);
    }

    public Collection<String> getIterationTearDowns(MethodInfo method) {
        return getHelperBlock(method, Level.Iteration, HelperType.TEARDOWN);
    }

    public Collection<String> getRunSetups(MethodInfo method) {
        return getHelperBlock(method, Level.Trial, HelperType.SETUP);
    }

    public Collection<String> getRunTearDowns(MethodInfo method) {
        return getHelperBlock(method, Level.Trial, HelperType.TEARDOWN);
    }

    public List<String> getStateInitializers() {
        Collection<StateObject> sos = cons(stateObjects);

        List<String> result = new ArrayList<>();

        for (StateObject so : sos) {
            if (so.scope != Scope.Benchmark) continue;

            result.add("");
            result.add("static volatile " + so.type + " " + so.fieldIdentifier + ";");
            result.add("");
            result.add(so.type + " _jmh_tryInit_" + so.fieldIdentifier + "(InfraControl control" + soDependency_TypeArgs(so) + ") throws Throwable {");
            result.add("    " + so.type + " val = " + so.fieldIdentifier + ";");
            result.add("    if (val != null) {");
            result.add("        return val;");
            result.add("    }");
            result.add("    synchronized(this.getClass()) {");
            result.add("        try {");
            result.add("        if (control.isFailing) throw new FailureAssistException();");
            result.add("        val = " + so.fieldIdentifier + ";");
            result.add("        if (val != null) {");
            result.add("            return val;");
            result.add("        }");
            result.add("        val = new " + so.type + "();");
            if (!so.getParamsLabels().isEmpty()) {
                result.add("        Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                for (FieldInfo paramField : so.getParam(paramName)) {
                    result.add("        f = " + paramField.getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                    result.add("        f.setAccessible(true);");
                    result.add("        f.set(val, " + so.getParamAccessor(paramField) + ");");
                }
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                Collection<String> args = so.helperArgs.get(hmi.method.getQualifiedName());
                result.add("        val." + hmi.method.getName() + "(" + Utils.join(args, ",") + ");");
            }
            result.add("        val.ready" + Level.Trial + " = true;");
            result.add("        " + so.fieldIdentifier + " = val;");
            result.add("        } catch (Throwable t) {");
            result.add("            control.isFailing = true;");
            result.add("            throw t;");
            result.add("        }");
            result.add("    }");
            result.add("    return val;");
            result.add("}");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Thread) continue;

            result.add("");
            result.add(so.type + " " + so.fieldIdentifier + ";");
            result.add("");
            result.add(so.type + " _jmh_tryInit_" + so.fieldIdentifier + "(InfraControl control" + soDependency_TypeArgs(so) + ") throws Throwable {");
            result.add("    if (control.isFailing) throw new FailureAssistException();");
            result.add("    " + so.type + " val = " + so.fieldIdentifier + ";");
            result.add("    if (val == null) {");
            result.add("        val = new " + so.type + "();");

            if (!so.getParamsLabels().isEmpty()) {
                result.add("            Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                for (FieldInfo paramField : so.getParam(paramName)) {
                    result.add("            f = " + paramField.getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                    result.add("            f.setAccessible(true);");
                    result.add("            f.set(val, " + so.getParamAccessor(paramField) + ");");
                }
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                Collection<String> args = so.helperArgs.get(hmi.method.getQualifiedName());
                result.add("        val." + hmi.method.getName() + "(" + Utils.join(args, ",") + ");");
            }
            result.add("        " + so.fieldIdentifier + " = val;");
            result.add("    }");
            result.add("    return val;");
            result.add("}");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Group) continue;

            result.add("");
            result.add("static java.util.Map<Integer, " + so.type + "> " + so.fieldIdentifier + "_map = java.util.Collections.synchronizedMap(new java.util.HashMap<Integer, " + so.type + ">());");
            result.add("");
            result.add(so.type + " _jmh_tryInit_" + so.fieldIdentifier + "(InfraControl control" + soDependency_TypeArgs(so) + ") throws Throwable {");
            result.add("    int groupIdx = threadParams.getGroupIndex();");
            result.add("    " + so.type + " val = " + so.fieldIdentifier + "_map.get(groupIdx);");
            result.add("    if (val != null) {");
            result.add("        return val;");
            result.add("    }");
            result.add("    synchronized(this.getClass()) {");
            result.add("        try {");
            result.add("        if (control.isFailing) throw new FailureAssistException();");
            result.add("        val = " + so.fieldIdentifier + "_map.get(groupIdx);");
            result.add("        if (val != null) {");
            result.add("            return val;");
            result.add("        }");
            result.add("        val = new " + so.type + "();");
            if (!so.getParamsLabels().isEmpty()) {
                result.add("        Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                for(FieldInfo paramField : so.getParam(paramName)) {
                    result.add("        f = " + paramField.getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                    result.add("        f.setAccessible(true);");
                    result.add("        f.set(val, " + so.getParamAccessor(paramField) + ");");
                }
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                Collection<String> args = so.helperArgs.get(hmi.method.getQualifiedName());
                result.add("        val." + hmi.method.getName() + "(" + Utils.join(args, ",") + ");");
            }
            result.add("        " + "val.ready" + Level.Trial + " = true;");
            result.add("        " + so.fieldIdentifier + "_map.put(groupIdx, val);");
            result.add("        } catch (Throwable t) {");
            result.add("            control.isFailing = true;");
            result.add("            throw t;");
            result.add("        }");
            result.add("    }");
            result.add("    return val;");
            result.add("}");
        }
        return result;
    }

    private String soDependency_TypeArgs(StateObject so) {
        return (so.depends.isEmpty() ? "" : ", " + getTypeArgList(so.depends));
    }

    private String soDependency_Args(StateObject so) {
        return (so.depends.isEmpty() ? "" : ", " + getArgList(so.depends));
    }

    public Collection<String> getStateDestructors(MethodInfo method) {
        Collection<StateObject> sos = stateOrder(method, false);

        List<String> result = new ArrayList<>();
        for (StateObject so : sos) {
            if (so.scope != Scope.Benchmark) continue;
            result.add("synchronized(this.getClass()) {");
            result.add("    " + so.fieldIdentifier + " = null;");
            result.add("}");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Thread) continue;
            result.add("" + so.fieldIdentifier + " = null;");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Group) continue;
            result.add("synchronized(this.getClass()) {");
            result.add("    " + so.fieldIdentifier + "_map.remove(threadParams.getGroupIndex());");
            result.add("}");
        }
        return result;
    }

    public List<String> getStateGetters(MethodInfo method) {
        List<String> result = new ArrayList<>();
        for (StateObject so : stateOrder(method, true)) {
            result.add(so.type + " " + so.localIdentifier + " = _jmh_tryInit_" + so.fieldIdentifier + "(control" + soDependency_Args(so) + ");");
        }
        return result;
    }

    private LinkedHashSet<StateObject> stateOrder(MethodInfo method, boolean reverse) {
        // Linearize @State dependency DAG
        List<StateObject> linearOrder = new ArrayList<>();

        List<StateObject> stratum = new ArrayList<>();

        // These are roots
        stratum.addAll(roots.get(method.getName()));
        stratum.addAll(implicits.values());

        // Recursively walk the DAG
        while (!stratum.isEmpty()) {
            linearOrder.addAll(stratum);
            List<StateObject> newStratum = new ArrayList<>();
            for (StateObject so : stratum) {
                newStratum.addAll(so.depends);
            }
            stratum = newStratum;
        }

        if (reverse) {
            Collections.reverse(linearOrder);
        }

        return new LinkedHashSet<>(linearOrder);
    }

    public void writeStateOverrides(BenchmarkGeneratorSession sess, GeneratorDestination dst) throws IOException {

        for (StateObject so : cons(stateObjects)) {
            if (!sess.generatedStateOverrides.add(so.userType)) continue;

            {
                PrintWriter pw = new PrintWriter(dst.newClass(so.packageName + "." + so.type + "_B1"));

                pw.println("package " + so.packageName + ";");

                pw.println("import " + so.userType + ";");

                pw.println("public class " + so.type + "_B1 extends " + so.userType + " {");
                Paddings.padding(pw);
                pw.println("}");

                pw.close();
            }

            {
                PrintWriter pw = new PrintWriter(dst.newClass(so.packageName + "." + so.type + "_B2"));

                pw.println("package " + so.packageName + ";");

                pw.println("import " + AtomicIntegerFieldUpdater.class.getCanonicalName() + ";");

                pw.println("public class " + so.type + "_B2 extends " + so.type + "_B1 {");

                for (Level level : Level.values()) {
                    pw.println("    public volatile int setup" + level + "Mutex;");
                    pw.println("    public volatile int tear" + level + "Mutex;");
                    pw.println("    public final static AtomicIntegerFieldUpdater<" + so.type + "_B2> setup" + level + "MutexUpdater = " +
                            "AtomicIntegerFieldUpdater.newUpdater(" + so.type + "_B2.class, \"setup" + level + "Mutex\");");
                    pw.println("    public final static AtomicIntegerFieldUpdater<" + so.type + "_B2> tear" + level + "MutexUpdater = " +
                            "AtomicIntegerFieldUpdater.newUpdater(" + so.type + "_B2.class, \"tear" + level + "Mutex\");");
                    pw.println("");
                }

                switch (so.scope) {
                    case Benchmark:
                    case Group:
                        for (Level level : Level.values()) {
                            pw.println("    public volatile boolean ready" + level + ";");
                        }
                        break;
                    case Thread:
                        // these flags are redundant for single thread
                        break;
                    default:
                        throw new IllegalStateException("Unknown state scope: " + so.scope);
                }

                pw.println("}");

                pw.close();
            }

            {
                PrintWriter pw = new PrintWriter(dst.newClass(so.packageName + "." + so.type + "_B3"));

                pw.println("package " + so.packageName + ";");
                pw.println("public class " + so.type + "_B3 extends " + so.type + "_B2 {");
                Paddings.padding(pw);
                pw.println("}");
                pw.println("");

                pw.close();
            }

            {
                PrintWriter pw = new PrintWriter(dst.newClass(so.packageName + "." + so.type));

                pw.println("package " + so.packageName + ";");
                pw.println("public class " + so.type + " extends " + so.type + "_B3 {");
                pw.println("}");
                pw.println("");

                pw.close();
            }
        }
    }

    public Collection<String> getFields() {
        return Collections.emptyList();
    }

    public StateObject getImplicit(String label) {
        return implicits.get(label);
    }

    public void addImports(PrintWriter writer) {
        for (StateObject so : cons(stateObjects)) {
            writer.println("import " + so.packageName + "." + so.type + ";");
        }
    }

    public Collection<String> getAuxResets(MethodInfo method) {
        Collection<String> result = new ArrayList<>();
        for (String name : auxNames.get(method.getName())) {
            if (auxResettable.get(name)) {
                result.add(auxAccessors.get(method.getName() + name) + " = 0;");
            }
        }
        return result;
    }

    public Collection<String> getAuxResults(MethodInfo method, String opResName) {
        Collection<String> result = new ArrayList<>();
        for (String ops : auxNames.get(method.getName())) {
            AuxCounters.Type type = auxType.get(ops);
            switch (type) {
                case OPERATIONS:
                    result.add("new " + opResName + "(ResultRole.SECONDARY, \"" + ops + "\", " +
                            auxAccessors.get(method.getName() + ops) + ", res.getTime(), benchmarkParams.getTimeUnit())");
                    break;
                case EVENTS:
                    result.add("new ScalarResult(\"" + ops + "\", " + auxAccessors.get(method.getName() + ops) + ", \"#\", AggregationPolicy.SUM)");
                    break;
                default:
                    throw new GenerationException("Unknown @" + AuxCounters.class + " type: " + type, method);
            }
        }
        return result;
    }
}
