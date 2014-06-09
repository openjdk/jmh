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
package org.openjdk.jmh.ct;

import junit.framework.Assert;
import org.openjdk.jmh.generators.asm.ASMGeneratorSource;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.GeneratorDestination;
import org.openjdk.jmh.generators.core.MetadataInfo;
import org.openjdk.jmh.generators.reflective.RFGeneratorSource;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileTest {

    private static final String GENERATOR_TYPE = System.getProperty("jmh.ct.generator", "notset");

    public static void assertFail(Class<?> klass) {
        TestGeneratorDestination destination = doTest(klass);
        if (!destination.hasErrors()) {
            Assert.fail("Should have failed.");
        }
    }

    public static void assertFail(Class<?> klass, String error) {
        TestGeneratorDestination destination = doTest(klass);
        if (!destination.hasErrors()) {
            Assert.fail("Should have failed.");
        }

        boolean contains = false;
        for (String e : destination.getErrors()) {
            System.err.println(e);
            contains |= e.contains(error);
        }
        Assert.assertTrue("Failure message should contain \"" + error + "\", but was \"" + destination.getErrors() + "\"", contains);
    }

    public static void assertOK(Class<?> klass) {
        TestGeneratorDestination destination = doTest(klass);

        if (destination.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed with:\n");
            for (String e : destination.getErrors()) {
                sb.append(e).append("\n");
            }
            Assert.fail(sb.toString());
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);

        try {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(new File(System.getProperty("java.io.tmpdir"))));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        Collection<JavaSourceFromString> sources = new ArrayList<JavaSourceFromString>();
        for (String name : destination.classBodies.keySet()) {
            sources.add(new JavaSourceFromString(name, destination.classBodies.get(name).toString()));
        }

        JavaCompiler.CompilationTask task = javac.getTask(null, fm, diagnostics, null, null, sources);

        boolean success = task.call();

        if (!success) {
            for (JavaSourceFromString src : sources) {
                System.out.println(src.getCharContent(false));
            }
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.println(diagnostic.getKind() + " at line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null));
            }
            Assert.fail("Unable to compile the generated code");
        }
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean iee) {
            return code;
        }
    }

    private static TestGeneratorDestination doTest(Class<?> klass) {
        if (GENERATOR_TYPE.equalsIgnoreCase("reflection")) {
            return doTestReflection(klass);
        }
        if (GENERATOR_TYPE.equalsIgnoreCase("bytecode")) {
            return doTestBytecode(klass);
        }
        throw new IllegalStateException("Unhandled compile test generator: " + GENERATOR_TYPE);
    }

    private static TestGeneratorDestination doTestReflection(Class<?> klass) {
        RFGeneratorSource source = new RFGeneratorSource();
        TestGeneratorDestination destination = new TestGeneratorDestination();
        source.processClasses(klass);

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);
        return destination;
    }

    private static TestGeneratorDestination doTestBytecode(Class<?> klass) {
        ASMGeneratorSource source = new ASMGeneratorSource();
        TestGeneratorDestination destination = new TestGeneratorDestination();

        String name = "/" + klass.getCanonicalName().replaceAll("\\.", "/") + ".class";
        try {
            source.processClass(klass.getResourceAsStream(name));
        } catch (IOException e) {
            throw new IllegalStateException(name, e);
        }

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);
        return destination;
    }

    public static class TestGeneratorDestination implements GeneratorDestination {

        List<String> errors = new ArrayList<String>();

        private Map<String, StringWriter> classBodies = new HashMap<String, StringWriter>();

        @Override
        public Writer newResource(String resourcePath) throws IOException {
            return new PrintWriter(System.out, true);
        }

        @Override
        public Writer newClass(String className) throws IOException {
            StringWriter sw = classBodies.get(className);
            if (sw != null) {
                throw new IllegalStateException("Already writing the class");
            } else {
                sw = new StringWriter();
                classBodies.put(className, sw);
            }
            return new PrintWriter(sw, true);
        }

        @Override
        public void printError(String message) {
            errors.add(message);
        }

        @Override
        public void printError(String message, MetadataInfo element) {
            errors.add(message);
        }

        @Override
        public void printError(String message, Throwable throwable) {
            errors.add(message + ":\n" + throwable.toString());
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }


}
