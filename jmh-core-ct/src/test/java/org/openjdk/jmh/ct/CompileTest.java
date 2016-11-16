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
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.generators.reflection.RFGeneratorSource;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;

public class CompileTest {

    private static final String GENERATOR_TYPE = System.getProperty("jmh.ct.generator", "notset");

    private static final String SRC_PREFIX = "SRC: ";

    public static void assertFail(Class<?> klass) {
        InMemoryGeneratorDestination destination = new InMemoryGeneratorDestination();
        boolean success = doTest(klass, destination);
        if (success) {
            Assert.fail("Should have failed.");
        }
    }

    public static void assertFail(Class<?> klass, String error) {
        InMemoryGeneratorDestination destination = new InMemoryGeneratorDestination();
        boolean success = doTest(klass, destination);
        if (success) {
            Assert.fail("Should have failed.");
        }

        List<String> testErrors = new ArrayList<>();
        boolean contains = false;
        for (String e : destination.getErrors()) {
            if (!e.startsWith(SRC_PREFIX)) {
                testErrors.add(e);
                contains |= e.contains(error);
            }
            System.err.println(e);
        }
        Assert.assertTrue("Failure message should contain \"" + error + "\", but was \"" + testErrors + "\"", contains);
    }

    public static void assertOK(Class<?> klass) {
        InMemoryGeneratorDestination destination = new InMemoryGeneratorDestination();
        boolean success = doTest(klass, destination);
        if (!success) {
            for (String e : destination.getErrors()) {
                System.err.println(e);
            }
            Assert.fail("Should have passed.");
        }
    }

    private static boolean doTest(Class<?> klass, InMemoryGeneratorDestination destination) {
        if (GENERATOR_TYPE.equalsIgnoreCase("reflection")) {
            RFGeneratorSource source = new RFGeneratorSource();
            source.processClasses(klass);
            return doTestOther(source, destination);
        } else if (GENERATOR_TYPE.equalsIgnoreCase("asm")) {
            ASMGeneratorSource source = new ASMGeneratorSource();
            String name = "/" + klass.getCanonicalName().replaceAll("\\.", "/") + ".class";
            try {
                source.processClass(klass.getResourceAsStream(name));
            } catch (IOException e) {
                throw new IllegalStateException(name, e);
            }
            return doTestOther(source, destination);
        } else if (GENERATOR_TYPE.equalsIgnoreCase("annprocess")) {
            return doTestAnnprocess(klass, destination);
        } else
            throw new IllegalStateException("Unhandled compile test generator: " + GENERATOR_TYPE);
    }


    public static boolean doTestOther(GeneratorSource source, InMemoryGeneratorDestination destination) {
        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);

        if (destination.hasErrors()) {
            return false;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);
        setupClassOutput(fm);

        Collection<JavaSourceFromString> sources = new ArrayList<>();
        for (Map.Entry<String, String> e : destination.getClasses().entrySet()) {
            sources.add(new JavaSourceFromString(e.getKey(), e.getValue()));
        }

        JavaCompiler.CompilationTask task = javac.getTask(null, fm, diagnostics, Collections.singleton("-proc:none"), null, sources);
        boolean success = task.call();

        if (!success) {
            for (JavaSourceFromString src : sources) {
                destination.printError(SRC_PREFIX + src.getCharContent(false).toString());
            }
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                destination.printError(diagnostic.getKind() + " at line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null));
            }
        }

        return success;
    }

    private static boolean doTestAnnprocess(Class<?> klass, InMemoryGeneratorDestination destination) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);
        setupClassOutput(fm);

        String name = "/" + klass.getCanonicalName().replaceAll("\\.", "/") + ".java";
        String shortName = klass.getName();

        InputStream stream = klass.getResourceAsStream(name);
        Assert.assertNotNull(name + " is not found", stream);

        try {
            Collection<String> lines = FileUtils.readAllLines(new InputStreamReader(stream));
            String file = Utils.join(lines, "\n");

            Collection<JavaSourceFromString> sources = Collections.singleton(new JavaSourceFromString(shortName, file));
            JavaCompiler.CompilationTask task = javac.getTask(null, fm, diagnostics, null, null, sources);

            boolean success = task.call();

            if (!success) {
                for (JavaSourceFromString src : sources) {
                    destination.printError(SRC_PREFIX + src.getCharContent(false).toString());
                }
                for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                    destination.printError(diagnostic.getKind() + " at line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null));
                }
            }
            return success;
        } catch (IOException e) {
            return false;
        }
    }

    private static void setupClassOutput(StandardJavaFileManager fm) {
        try {
            File tmp = File.createTempFile("jmh-core-ct", "temp");
            if (!tmp.delete()) {
                throw new IOException("Cannot delete temp file: " + tmp);
            }
            if (!tmp.mkdirs()) {
                throw new IOException("Cannot create temp dir: " + tmp);
            }
            tmp.deleteOnExit();
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(tmp));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
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

}
