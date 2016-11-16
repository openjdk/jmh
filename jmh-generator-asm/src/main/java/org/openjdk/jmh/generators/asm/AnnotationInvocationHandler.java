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
package org.openjdk.jmh.generators.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class AnnotationInvocationHandler extends AnnotationVisitor implements InvocationHandler {
    private final String className;
    private final Multimap<String, Object> values;

    public AnnotationInvocationHandler(String className, AnnotationVisitor annotationVisitor) {
        super(Opcodes.ASM4, annotationVisitor);
        this.className = className;
        this.values = new HashMultimap<>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String member = method.getName();
        Class<?> returnType = method.getReturnType();
        Class<?>[] paramTypes = method.getParameterTypes();

        if (member.equals("equals") && paramTypes.length == 1 &&
            paramTypes[0] == Object.class)
            return equalsImpl(args[0]);
        if (paramTypes.length != 0)
            throw new AssertionError("Too many parameters for an annotation method");
        switch (member) {
            case "toString":
                return toStringImpl();
            case "hashCode":
                return hashcodeImpl();
            case "annotationType":
                throw new IllegalStateException("annotationType is not implemented");
        }

        /*
          Unfortunately, we can not pre-process these values when walking the annotation
          with ASM, since we are oblivious of exact types. Try to match the observed values
          right here, based on the method return type.
        */

        Collection<Object> vs = values.get(member);
        if (vs == null || vs.isEmpty()) {
            return method.getDefaultValue();
        }

        if (!returnType.isArray()) {
            Object res = peelSingle(vs);

            if (returnType.isEnum()) {
                return parseEnum(returnType, res);
            }

            // String will return as is; primitive will auto-box
            return res;
        } else {
            Class<?> componentType = returnType.getComponentType();

            if (componentType.isEnum()) {
                // Dealing with Enum[]:
                Object res = Array.newInstance(componentType, vs.size());
                int c = 0;
                for (Object v : vs) {
                    Array.set(res, c, parseEnum(componentType, v));
                    c++;
                }
                return res;

            } else if (componentType.isAssignableFrom(String.class)) {
                // Dealing with String[]:
                return vs.toArray(new String[vs.size()]);

            } else {
                // "Dealing" with primitive array:
                // We do not have any primitive-array-valued annotations yet, so we don't bother to implement this.
                throw new IllegalStateException("Primitive arrays are not handled yet");
            }
        }
    }

    private Object peelSingle(Collection<Object> vs) {
        Object res;
        if (vs.size() == 1) {
            res = vs.iterator().next();
        } else {
            throw new IllegalStateException("Expected to see a single value, but got " + vs.size());
        }
        return res;
    }

    private String toStringImpl() {
        StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(className);
        sb.append("(");
        for (String k : values.keys()) {
            sb.append(k);
            sb.append(" = ");
            sb.append(values.get(k));
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private Object parseEnum(Class<?> type, Object res) throws Exception {
        if (res == null) {
            throw new IllegalStateException("The argument is null");
        }
        if (!(res instanceof String)) {
            throw new IllegalStateException("The argument is not String, but " + res.getClass());
        }
        Method m = type.getMethod("valueOf", String.class);
        return m.invoke(null, res);
    }

    private int hashcodeImpl() {
        int result = className.hashCode();
        for (String k : values.keys()) {
            result = 31 * result + k.hashCode();
        }
        return result;
    }

    private boolean equalsImpl(Object arg) {
        AnnotationInvocationHandler other = asOneOfUs(arg);
        if (other != null) {
            if (!className.equals(other.className)) {
                return false;
            }

            Set<String> keys = new HashSet<>();
            keys.addAll(values.keys());
            keys.addAll(other.values.keys());

            for (String k : keys) {
                Collection<Object> o1 = values.get(k);
                Collection<Object> o2 = other.values.get(k);

                if (o1 == null || o2 == null) {
                    return false;
                }

                if (o1.size() != o2.size()) {
                    return false;
                }

                if (!o1.containsAll(o2) || !o2.containsAll(o1)) {
                    return false;
                }
            }
            return true;
        } else {
            throw new IllegalStateException("Expected to see only AnnotationInvocationHandler-backed annotations");
        }
    }

    private AnnotationInvocationHandler asOneOfUs(Object o) {
        if (Proxy.isProxyClass(o.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(o);
            if (handler instanceof AnnotationInvocationHandler)
                return (AnnotationInvocationHandler) handler;
        }
        return null;
    }

    @Override
    public void visit(String name, Object value) {
        values.put(name, value);
        super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        values.put(name, value);
        super.visitEnum(name, desc, value);
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        return new AnnotationVisitor(Opcodes.ASM4, super.visitArray(name)) {
            @Override
            public void visitEnum(String n, String desc, String value) {
                values.put(name, value);
                super.visitEnum(n, desc, value);
            }

            @Override
            public void visit(String n, Object value) {
                values.put(name, value);
                super.visit(n, value);
            }
        };
    }
}
