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

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.logic.Control;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class StateObjectHandler {

    private final CompilerControlPlugin compileControl;

    private final Identifiers identifiers;

    private final Multimap<String, StateObject> args;
    private final Map<String, StateObject> implicits;
    private final Set<StateObject> stateObjects;

    private final Multimap<String, String> auxNames = new HashMultimap<String, String>();
    private final Map<String, String> auxAccessors = new HashMap<String, String>();

    public StateObjectHandler(CompilerControlPlugin compileControl) {
        this.compileControl = compileControl;
        this.args = new HashMultimap<String, StateObject>();
        this.implicits = new HashMap<String, StateObject>();
        this.stateObjects = new HashSet<StateObject>();
        this.identifiers = new Identifiers();
    }

    public void bindMethodGroup(MethodGroup mg) {
        int index = 0;

        for (MethodInfo method : mg.methods()) {
            for (ParameterInfo p : method.getParameters()) {
                ClassInfo ci = p.getType();

                State ann = BenchmarkGeneratorUtils.getAnnSuper(ci, State.class);
                if (ann == null) {
                    throw new GenerationException("The method parameter is not a @" + State.class.getSimpleName() + ": ", p);
                }

                Scope scope = ann.value();

                String identifier;
                switch (scope) {
                    case Benchmark:
                    case Group: {
                        identifier = "G";
                        break;
                    }
                    case Thread: {
                        identifier = String.valueOf(index++);
                        break;
                    }
                    default:
                        throw new GenerationException("Unknown scope: " + scope, ci);
                }

                StateObject so = new StateObject(identifiers, ci.getQualifiedName(), scope, identifier);
                stateObjects.add(so);
                args.put(method.getName(), so);
                bindState(method, so, ci);
            }
        }
    }

    public void bindImplicit(ClassInfo ci, String label, Scope scope) {
        State ann = BenchmarkGeneratorUtils.getAnnSuper(ci, State.class);
        StateObject so = new StateObject(identifiers, ci.getQualifiedName(), (ann != null) ? ann.value() : scope, label);
        stateObjects.add(so);
        implicits.put(label, so);
        bindState(null, so, ci);
    }

    private void bindState(MethodInfo execMethod, StateObject so, ClassInfo ci) {
        // auxiliary result, produce the accessors
        if (ci.getAnnotation(AuxCounters.class) != null) {
            if (so.scope != Scope.Thread) {
                throw new GenerationException("@" + AuxCounters.class.getSimpleName() +
                        " can only be used with " + Scope.class.getSimpleName() + "." + Scope.Thread + " states.", ci);
            }

            for (FieldInfo sub : ci.getFields()) {
                if (sub.isPublic()) {
                    String fieldType = sub.getType();
                    if (fieldType.equals("int") || fieldType.equals("long")) {
                        String name = sub.getName();
                        String meth = execMethod.getName();
                        auxNames.put(meth, name);
                        String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name);
                        if (prev != null) {
                            throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                " injected into this method.", sub);
                        }
                    }
                }
            }

            for (MethodInfo sub : ci.getMethods()) {
                if (sub.isPublic()) {
                    String returnType = sub.getReturnType();
                    if (returnType.equals("int") || returnType.equals("long")) {
                        String name = sub.getName();
                        String meth = execMethod.getName();
                        auxNames.put(meth, name);
                        String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name + "()");
                        if (prev != null) {
                            throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                    " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                    " injected into this method.", sub);
                        }
                    }
                }
            }
        }

        // walk the type hierarchy up to discover inherited @Params
        for (FieldInfo fi : BenchmarkGeneratorUtils.getAllFields(ci)) {
            if (fi.getAnnotation(Param.class) != null) {
                so.addParam(fi);
            }
        }

        // put the @State objects helper methods
        for (MethodInfo mi : BenchmarkGeneratorUtils.getMethods(ci)) {
            Setup setupAnn = mi.getAnnotation(Setup.class);
            if (setupAnn != null) {
                if (!mi.getParameters().isEmpty()) {
                    throw new GenerationException("@" + Setup.class.getSimpleName() + " methods should have no arguments.", mi);
                }
                so.addHelper(new HelperMethodInvocation(mi, so, setupAnn.value(), HelperType.SETUP));
                compileControl.defaultForceInline(mi);
            }

            TearDown tearDownAnn = mi.getAnnotation(TearDown.class);
            if (tearDownAnn != null) {
                if (!mi.getParameters().isEmpty()) {
                    throw new GenerationException("@" + TearDown.class.getSimpleName() + " methods should have no arguments.", mi);
                }
                so.addHelper(new HelperMethodInvocation(mi, so, tearDownAnn.value(), HelperType.TEARDOWN));
                compileControl.defaultForceInline(mi);
            }
        }
    }

    public String getArgList(MethodInfo methodInfo) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (StateObject so : args.get(methodInfo.getName())) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(so.toLocal());
            i++;
        }
        return sb.toString();
    }

    public String getTypeArgList(MethodInfo methodInfo) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (StateObject so : args.get(methodInfo.getName())) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(so.toTypeDef());
            i++;
        }
        return sb.toString();
    }

    public static Collection<StateObject> cons(Collection<StateObject>... colls) {
        SortedSet<StateObject> r = new TreeSet<StateObject>(StateObject.ID_COMPARATOR);
        for (Collection<StateObject> coll : colls) {
            r.addAll(coll);
        }
        return r;
    }

    public Collection<String> getHelperBlock(MethodInfo method, Level helperLevel, HelperType type) {

        // Look for the offending methods.
        // This will be used to skip the irrelevant blocks for state objects down the stream.
        Set<StateObject> states = new HashSet<StateObject>();
        for (StateObject so : cons(args.get(method.getName()), implicits.values(), getControls())) {
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel == helperLevel) states.add(so);
            }
        }

        List<String> result = new ArrayList<String>();

        // Handle Thread object helpers
        for (StateObject so : states) {
            if (so.scope != Scope.Thread) continue;

            if (type == HelperType.SETUP) {
                result.add("if (!" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        result.add("    " + so.localIdentifier + "." + mi.method.getName() + "();");
                    }
                }
                result.add("    " + so.localIdentifier + ".ready" + helperLevel + " = true;");
                result.add("}");
            }

            if (type == HelperType.TEARDOWN) {
                result.add("if (" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        result.add("    " + so.localIdentifier + "." + mi.method.getName() + "();");
                    }
                }
                result.add("    " + so.localIdentifier + ".ready" + helperLevel + " = false;");
                result.add("}");
            }
        }

        // Handle Benchmark/Group object helpers
        for (StateObject so : states) {
            if (so.scope != Scope.Benchmark && so.scope != Scope.Group) continue;

            if (type == HelperType.SETUP) {
                result.add("while(!" + so.type + ".setup" + helperLevel + "MutexUpdater.compareAndSet(" + so.localIdentifier + ", 0, 1)) {");
                result.add("    if (Thread.interrupted()) throw new InterruptedException();");
                result.add("}");
                result.add("try {");
                result.add("    if (!" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        result.add("        " + so.localIdentifier + "." + mi.method.getName() + "();");
                    }
                }
                result.add("        " + so.localIdentifier + ".ready" + helperLevel + " = true;");
                result.add("    }");
                result.add("} finally {");
                result.add("    " + so.type + ".setup" + helperLevel + "MutexUpdater.set(" + so.localIdentifier + ", 0);");
                result.add("}");
            }

            if (type == HelperType.TEARDOWN) {
                result.add("while(!" + so.type + ".tear" + helperLevel + "MutexUpdater.compareAndSet(" + so.localIdentifier + ", 0, 1)) {");
                result.add("    if (Thread.interrupted()) throw new InterruptedException();");
                result.add("}");
                result.add("try {");
                result.add("    if (" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : so.getHelpers()) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        result.add("        " + so.localIdentifier + "." + mi.method.getName() + "();");
                    }
                }
                result.add("        " + so.localIdentifier + ".ready" + helperLevel + " = false;");
                result.add("    }");
                result.add("} finally {");
                result.add("    " + so.type + ".tear" + helperLevel + "MutexUpdater.set(" + so.localIdentifier + ", 0);");
                result.add("}");
            }
        }

        return result;
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

        List<String> result = new ArrayList<String>();

        for (StateObject so : sos) {
            if (so.scope != Scope.Benchmark) continue;

            result.add("");
            result.add("static volatile " + so.type + " " + so.fieldIdentifier + ";");
            result.add("");
            result.add(so.type + " tryInit_" + so.fieldIdentifier + "(InfraControl control, " + so.type + " val) throws Throwable {");
            result.add("    synchronized(this.getClass()) {");
            result.add("        if (" + so.fieldIdentifier + " == null) {");
            result.add("            " + so.fieldIdentifier + " = val;");
            result.add("        }");
            result.add("        if (!" + so.fieldIdentifier + ".ready" + Level.Trial + ") {");
            if (!so.getParamsLabels().isEmpty()) {
                result.add("            Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                result.add("            f = " + so.getParam(paramName).getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                result.add("            f.setAccessible(true);");
                result.add("            f.set(" + so.fieldIdentifier + ", " + so.getParamAccessor(paramName) + ");");
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("            " + so.fieldIdentifier + "." + hmi.method.getName() + "();");
            }
            result.add("            " + so.fieldIdentifier + ".ready" + Level.Trial + " = true;");
            result.add("        }");
            result.add("    }");
            result.add("    return " + so.fieldIdentifier + ";");
            result.add("}");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Thread) continue;

            result.add("");
            result.add(so.type + " " + so.fieldIdentifier + ";");
            result.add("");
            result.add(so.type + " tryInit_" + so.fieldIdentifier + "(InfraControl control, " + so.type + " val) throws Throwable {");
            result.add("    if (" + so.fieldIdentifier + " == null) {");
            if (!so.getParamsLabels().isEmpty()) {
                result.add("            Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                result.add("        f = " + so.getParam(paramName).getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                result.add("        f.setAccessible(true);");
                result.add("        f.set(val, " + so.getParamAccessor(paramName) + ");");
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("        val." + hmi.method.getName() + "();");
            }
            result.add("        " + "val.ready" + Level.Trial + " = true;");
            result.add("        " + so.fieldIdentifier + " = val;");
            result.add("    }");
            result.add("    return " + so.fieldIdentifier + ";");
            result.add("}");
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Group) continue;

            result.add("");
            result.add("static java.util.Map<Integer, " + so.type + "> " + so.fieldIdentifier + "_map = java.util.Collections.synchronizedMap(new java.util.HashMap<Integer, " + so.type + ">());");
            result.add("");
            result.add(so.type + " tryInit_" + so.fieldIdentifier + "(InfraControl control, int groupId, " + so.type + " val) throws Throwable {");
            result.add("    synchronized(this.getClass()) {");
            result.add("        " + so.type + " local = " + so.fieldIdentifier + "_map.get(groupId);");
            result.add("        if (local == null) {");
            result.add("            " + so.fieldIdentifier + "_map.put(groupId, val);");
            result.add("            local = val;");
            result.add("        }");
            result.add("        if (!local.ready" + Level.Trial + ") {");
            if (!so.getParamsLabels().isEmpty()) {
                result.add("            Field f;");
            }
            for (String paramName : so.getParamsLabels()) {
                result.add("            f = " + so.getParam(paramName).getDeclaringClass().getQualifiedName() + ".class.getDeclaredField(\"" + paramName + "\");");
                result.add("            f.setAccessible(true);");
                result.add("            f.setAccessible(true);");
                result.add("            f.set(local, " + so.getParamAccessor(paramName) + ");");
            }
            for (HelperMethodInvocation hmi : so.getHelpers()) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("            local." + hmi.method.getName() + "();");
            }
            result.add("            " + "local.ready" + Level.Trial + " = true;");
            result.add("            " + so.fieldIdentifier + "_map.put(groupId, val);");
            result.add("        }");
            result.add("        return local;");
            result.add("    }");
            result.add("}");
        }
        return result;
    }

    public Collection<String> getStateDestructors(MethodInfo method) {
        Collection<StateObject> sos = cons(args.get(method.getName()), implicits.values());

        List<String> result = new ArrayList<String>();
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
            result.add("    " + so.fieldIdentifier + "_map.remove(threadControl.group);");
            result.add("}");
        }
        return result;
    }

    public List<String> getStateGetters(MethodInfo method) {
        List<String> result = new ArrayList<String>();
        for (StateObject so : cons(args.get(method.getName()), implicits.values(), getControls())) {
            switch (so.scope) {
                case Benchmark:
                case Thread:
                    result.add(so.type + " " + so.localIdentifier + " = tryInit_" + so.fieldIdentifier + "(control, new " + so.type + "());");
                    break;
                case Group:
                    result.add(so.type + " " + so.localIdentifier + " = tryInit_" + so.fieldIdentifier + "(control, threadControl.group, new " + so.type + "());");
                    break;
                default:
                    throw new IllegalStateException("Unhandled scope: " + so.scope);
            }
        }
        return result;
    }

    public List<String> getStateOverrides() {
        Set<String> visited = new HashSet<String>();

        List<String> result = new ArrayList<String>();
        for (StateObject so : cons(stateObjects)) {
            if (!visited.add(so.userType)) continue;
            result.add("static class " + so.type + "_B1 extends " + so.userType + " {");
            padding(result, "b1");
            result.add("}");
            result.add("");
            result.add("static class " + so.type + "_B2 extends " + so.type + "_B1 {");


            for (Level level : Level.values()) {
                result.add("    public volatile int setup" + level + "Mutex;");
                result.add("    public volatile int tear" + level + "Mutex;");
                result.add("    public final static AtomicIntegerFieldUpdater setup" + level + "MutexUpdater = " +
                        "AtomicIntegerFieldUpdater.newUpdater(" + so.type + "_B2.class, \"setup" + level + "Mutex\");");
                result.add("    public final static AtomicIntegerFieldUpdater tear" + level + "MutexUpdater = " +
                        "AtomicIntegerFieldUpdater.newUpdater(" + so.type + "_B2.class, \"tear" + level + "Mutex\");");
                result.add("");
            }

            switch (so.scope) {
                case Benchmark:
                case Group:
                    for (Level level : Level.values()) {
                        result.add("    public volatile boolean ready" + level + ";");
                    }
                    break;
                case Thread:
                    for (Level level : Level.values()) {
                        result.add("    public boolean ready" + level + ";");
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown state scope: " + so.scope);
            }

            result.add("}");
            result.add("");
            result.add("static class " + so.type + "_B3 extends " + so.type + "_B2 {");
            padding(result, "b3");
            result.add("}");
            result.add("");
            result.add("static final class " + so.type + " extends " + so.type + "_B3 {");
            result.add("}");
            result.add("");
        }
        return result;
    }

    public static void padding(List<String> lines, String suffix) {
        for (int p = 0; p < 16; p++) {
            StringBuilder sb = new StringBuilder();
            sb.append("    private boolean p").append(suffix).append("_").append(p);
            for (int q = 1; q < 16; q++) {
                sb.append(", p").append(suffix).append("_").append(p).append("_").append(q);
            }
            sb.append(";");
            lines.add(sb.toString());
        }
    }

    public void clearArgs() {
        args.clear();
    }

    public Collection<String> getFields() {
        return Collections.emptyList();
    }

    public StateObject getImplicit(String label) {
        return implicits.get(label);
    }

    public Collection<StateObject> getControls() {
        Collection<StateObject> s = new ArrayList<StateObject>();
        for (StateObject so : cons(args.values())) {
            if (so.userType.equals(Control.class.getName())) {
                s.add(so);
            }
        }
        return s;
    }

    public Collection<String> getAuxResultNames(MethodInfo method) {
        return auxNames.get(method.getName());
    }

    public String getAuxResultAccessor(MethodInfo method, String name) {
        return auxAccessors.get(method.getName() + name);
    }

}
