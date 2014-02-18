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
import org.openjdk.jmh.util.internal.TreesetMultimap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
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

    private final ProcessingEnvironment processingEnv;
    private final Multimap<String, StateObject> args;
    private final Map<String, StateObject> implicits;
    private final Set<StateObject> stateObjects;
    private final Multimap<StateObject, HelperMethodInvocation> helpersByState;

    private final Map<String, Integer> globalIndexByType = new HashMap<String, Integer>();
    private final Map<String, Integer> groupIndexByType = new HashMap<String, Integer>();
    private final Map<String, Integer> localIndexByType = new HashMap<String, Integer>();

    private final HashMap<String, String> collapsedTypes = new HashMap<String, String>();
    private int collapsedIndex = 0;

    private final Set<String> claimedJmhTypes = new HashSet<String>();
    private final HashMap<String, String> jmhTypes = new HashMap<String, String>();
    private final Multimap<String, String> auxNames = new HashMultimap<String, String>();
    private final Map<String, String> auxAccessors = new HashMap<String, String>();

    public StateObjectHandler(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.args = new HashMultimap<String, StateObject>();
        this.implicits = new HashMap<String, StateObject>();
        this.stateObjects = new HashSet<StateObject>();
        this.helpersByState = new TreesetMultimap<StateObject, HelperMethodInvocation>();
    }

    private String getJMHtype(String type) {
        String jmhType = jmhTypes.get(type);
        if (jmhType == null) {
            int v = 1;
            do {
                jmhType = getBaseType(type) + "_" + v + "_jmh";
                v++;
            } while (!claimedJmhTypes.add(jmhType));
            jmhTypes.put(type, jmhType);
        }
        return jmhType;
    }

    private String getBaseType(String type) {
        return type.substring(type.lastIndexOf(".") + 1);
    }

    public void bindArg(ExecutableElement execMethod, TypeElement type) {
        State ann = type.getAnnotation(State.class);
        if (ann != null) {
            bindState(execMethod, type, ann.value(), null);
        } else {
            throw new IllegalStateException("The method parameter is not a @State: " + type);
        }
    }

    public void bindImplicit(TypeElement type, String label) {
        bindImplicit(type, label, Scope.Thread);
    }

    public void bindImplicit(TypeElement type, String label, Scope scope) {
        State ann = type.getAnnotation(State.class);
        bindState(null, type, (ann != null) ? ann.value() : scope, label);
    }

    private void bindState(ExecutableElement execMethod, TypeElement element, Scope scope, String implicitLabel) {
        Integer index;
        String className = element.getQualifiedName().toString();
        switch (scope) {
            case Benchmark: {
                index = globalIndexByType.get(className);
                if (index == null) {
                    index = 0;
                    globalIndexByType.put(className, index);
                }
                break;
            }
            case Group:
                index = groupIndexByType.get(className);
                if (index == null) {
                    index = 0;
                    groupIndexByType.put(className, index);
                }
                break;
            case Thread: {
                index = localIndexByType.get(className);
                if (index == null) {
                    index = -1;
                }
                index++;
                localIndexByType.put(className, index);
                break;
            }
            default:
                throw new IllegalStateException("Unknown scope: " + scope);
        }

        StateObject so;
        if (implicitLabel != null) {
            so = new StateObject(className, getJMHtype(className), scope, "f_" + implicitLabel, "l_" + implicitLabel);
            implicits.put(implicitLabel, so);
        } else {
            String identifier = collapseTypeName(className) + index;
            so = new StateObject(className, getJMHtype(className), scope, "f_" + identifier, "l_" + identifier);
            args.put(execMethod.getSimpleName().toString(), so);
        }

        // auxiliary result, produce the accessors
        if (element.getAnnotation(AuxCounters.class) != null) {
            if (scope != Scope.Thread) {
                throw new GenerationException("@" + AuxCounters.class.getSimpleName() +
                        " can only be used with " + Scope.class.getSimpleName() + "." + Scope.Thread + " states.", element);
            }

            for (Element sub : element.getEnclosedElements()) {
                if (sub.getKind() == ElementKind.FIELD && sub.getModifiers().contains(Modifier.PUBLIC)) {
                    String fieldType = sub.asType().toString();
                    if (fieldType.equals("int") || fieldType.equals("long")) {
                        String name = sub.getSimpleName().toString();
                        String meth = execMethod.getSimpleName().toString();
                        auxNames.put(meth, name);
                        String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name);
                        if (prev != null) {
                            throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                " injected into this method.", element);
                        }
                    }
                }

                if (sub.getKind() == ElementKind.METHOD && sub.getModifiers().contains(Modifier.PUBLIC)) {
                    String returnType = ((ExecutableElement) sub).getReturnType().toString();
                    if (returnType.equals("int") || returnType.equals("long")) {
                        String name = sub.getSimpleName().toString();
                        String meth = execMethod.getSimpleName().toString();
                        auxNames.put(meth, name);
                        String prev = auxAccessors.put(meth + name, so.localIdentifier + "." + name + "()");
                        if (prev != null) {
                            throw new GenerationException("Conflicting @" + AuxCounters.class.getSimpleName() +
                                    " counters. Make sure there are no @" + State.class.getSimpleName() + "-s with the same counter " +
                                    " injected into this method.", element);
                        }
                    }
                }
            }
        }

        stateObjects.add(so);

        // walk the type hierarchy up to discover inherited @Params
        TypeElement walk1 = element;
        do {
            for (VariableElement ve : ElementFilter.fieldsIn(walk1.getEnclosedElements())) {
                if (ve.getAnnotation(Param.class) != null) {
                    so.addParam(ve);
                }
            }
        } while ((walk1 = (TypeElement) processingEnv.getTypeUtils().asElement(walk1.getSuperclass())) != null);

        // walk the type hierarchy up to discover inherited helper methods
        TypeElement walk2 = element;
        do {
            for (ExecutableElement m : ElementFilter.methodsIn(walk2.getEnclosedElements())) {
                Setup setupAnn = m.getAnnotation(Setup.class);
                if (setupAnn != null) {
                    helpersByState.put(so, new HelperMethodInvocation(m.getSimpleName().toString(), so, setupAnn.value(), HelperType.SETUP));
                }

                TearDown tearDownAnn = m.getAnnotation(TearDown.class);
                if (tearDownAnn != null) {
                    helpersByState.put(so, new HelperMethodInvocation(m.getSimpleName().toString(), so, tearDownAnn.value(), HelperType.TEARDOWN));
                }
            }
        } while ((walk2 = (TypeElement) processingEnv.getTypeUtils().asElement(walk2.getSuperclass())) != null);
    }

    public String getArgList(Element method) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (StateObject so : args.get(method.getSimpleName().toString())) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(so.toLocal());
            i++;
        }
        return sb.toString();
    }

    public String getTypeArgList(Element method) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (StateObject so : args.get(method.getSimpleName().toString())) {
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

    public Collection<String> getHelperBlock(String method, Level helperLevel, HelperType type) {

        Collection<StateObject> states = cons(args.get(method), implicits.values(), getControls());

        // Look for the offending methods.
        // This will be used to skip the irrelevant blocks for state objects down the stream.
        Set<StateObject> hasHelpers = new HashSet<StateObject>();
        for (StateObject so : states) {
            for (HelperMethodInvocation hmi : helpersByState.get(so)) {
                if (hmi.helperLevel == helperLevel) hasHelpers.add(so);
            }
        }

        List<String> result = new ArrayList<String>();

        // Handle Thread object helpers
        for (StateObject so : states) {
            if (so.scope != Scope.Thread) continue;
            if (!hasHelpers.contains(so)) continue;

            if (type == HelperType.SETUP) {
                result.add("if (!" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : helpersByState.get(so)) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        result.add("    " + so.localIdentifier + "." + mi.name + "();");
                    }
                }
                result.add("    " + so.localIdentifier + ".ready" + helperLevel + " = true;");
                result.add("}");
            }

            if (type == HelperType.TEARDOWN) {
                result.add("if (" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : helpersByState.get(so)) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        result.add("    " + so.localIdentifier + "." + mi.name + "();");
                    }
                }
                result.add("    " + so.localIdentifier + ".ready" + helperLevel + " = false;");
                result.add("}");
            }
        }

        // Handle Benchmark/Group object helpers
        for (StateObject so : states) {
            if (so.scope != Scope.Benchmark && so.scope != Scope.Group) continue;
            if (!hasHelpers.contains(so)) continue;

            if (type == HelperType.SETUP) {
                result.add("while(!" + so.type + ".setup" + helperLevel + "MutexUpdater.compareAndSet(" + so.localIdentifier + ", 0, 1)) {");
                result.add("    if (Thread.interrupted()) throw new InterruptedException();");
                result.add("}");
                result.add("try {");
                result.add("    if (!" + so.localIdentifier + ".ready" + helperLevel + ") {");
                for (HelperMethodInvocation mi : helpersByState.get(so)) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP) {
                        result.add("        " + so.localIdentifier + "." + mi.name + "();");
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
                for (HelperMethodInvocation mi : helpersByState.get(so)) {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN) {
                        result.add("        " + so.localIdentifier + "." + mi.name + "();");
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

    public Collection<String> getInvocationSetups(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Invocation, HelperType.SETUP);
    }

    public Collection<String> getInvocationTearDowns(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Invocation, HelperType.TEARDOWN);
    }

    public Collection<String> getIterationSetups(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Iteration, HelperType.SETUP);
    }

    public Collection<String> getIterationTearDowns(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Iteration, HelperType.TEARDOWN);
    }

    public Collection<String> getRunSetups(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Trial, HelperType.SETUP);
    }

    public Collection<String> getRunTearDowns(Element method) {
        return getHelperBlock(method.getSimpleName().toString(), Level.Trial, HelperType.TEARDOWN);
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
            for (String paramName : so.getParamsLabels()) {
                result.add("            " + so.fieldIdentifier + "." + paramName + " = " + so.getParamAccessor(paramName) + ";");
            }
            for (HelperMethodInvocation hmi : helpersByState.get(so)) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("            " + so.fieldIdentifier + "." + hmi.name + "();");
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
            for (String paramName : so.getParamsLabels()) {
                result.add("                val." + paramName + " = " + so.getParamAccessor(paramName) + ";");
            }
            for (HelperMethodInvocation hmi : helpersByState.get(so)) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("                val." + hmi.name + "();");
            }
            result.add("                " + "val.ready" + Level.Trial + " = true;");
            result.add("          " + so.fieldIdentifier + " = val;");
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
            for (String paramName : so.getParamsLabels()) {
                result.add("            local." + paramName + " = " + so.getParamAccessor(paramName) + ";");
            }
            for (HelperMethodInvocation hmi : helpersByState.get(so)) {
                if (hmi.helperLevel != Level.Trial) continue;
                if (hmi.type != HelperType.SETUP) continue;
                result.add("            local." + hmi.name + "();");
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

    public Collection<String> getStateDestructors(Element method) {
        Collection<StateObject> sos = cons(args.get(method.getSimpleName().toString()), implicits.values());

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

    public List<String> getStateGetters(Element method) {
        List<String> result = new ArrayList<String>();
        for (StateObject so : cons(args.get(method.getSimpleName().toString()), implicits.values(), getControls())) {
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
            sb.append("    boolean jmh_b3_pad_").append(p);
            for (int q = 1; q < 16; q++) {
                sb.append(", jmh_b3_pad_").append(p).append("_").append(q);
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

    private String collapseTypeName(String e) {
        if (collapsedTypes.containsKey(e)) {
            return collapsedTypes.get(e);
        }

        String[] strings = e.split("\\.");
        String name = strings[strings.length - 1].toLowerCase();

        String collapsedName = name + (collapsedIndex++) + "_";
        collapsedTypes.put(e, collapsedName);
        return collapsedName;
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

    public Collection<String> getAuxResultNames(Element method) {
        return auxNames.get(method.getSimpleName().toString());
    }

    public String getAuxResultAccessor(Element method, String name) {
        return auxAccessors.get(method.getSimpleName().toString() + name);
    }

}
