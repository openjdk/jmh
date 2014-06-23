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
package org.openjdk.jmh.generators.bytecode;

import org.openjdk.jmh.generators.asm.ASMGeneratorSource;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.generators.core.SourceError;
import org.openjdk.jmh.generators.reflection.RFGeneratorSource;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public class JmhBytecodeGenerator {

    public static final String GENERATOR_TYPE_DEFAULT = "default";
    public static final String GENERATOR_TYPE_ASM = "asm";
    public static final String GENERATOR_TYPE_REFLECTION = "reflection";

    public static final String DEFAULT_GENERATOR_TYPE = GENERATOR_TYPE_REFLECTION;

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: generator <compiled-bytecode-dir> <output-source-dir> <output-resource-dir> [generator-type]");
            System.exit(1);
        }
        File compiledBytecodeDirectory = new File(args[0]);
        File outputSourceDirectory = new File(args[1]);
        File outputResourceDirectory = new File(args[2]);

        String generatorType = DEFAULT_GENERATOR_TYPE;
        if (args.length >= 4) {
            if (!args[3].equalsIgnoreCase(GENERATOR_TYPE_DEFAULT)) {
                generatorType = args[3];
            }
        }

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        URLClassLoader amendedCL = new URLClassLoader(
                new URL[]{compiledBytecodeDirectory.toURI().toURL()},
                Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(amendedCL);

        FileSystemDestination destination = new FileSystemDestination(outputResourceDirectory, outputSourceDirectory);

        Collection<File> classes = FileUtils.getClasses(compiledBytecodeDirectory);
        System.out.println("Processing " + classes.size() + " classes from " + compiledBytecodeDirectory + " with \"" + generatorType + "\" generator");
        System.out.println("Writing out Java source to "  + outputSourceDirectory + " and resources to " + outputResourceDirectory);

        GeneratorSource source = null;
        if (generatorType.equalsIgnoreCase(GENERATOR_TYPE_ASM)) {
            ASMGeneratorSource src = new ASMGeneratorSource();
            src.processClasses(classes);
            source = src;
        } else if (generatorType.equalsIgnoreCase(GENERATOR_TYPE_REFLECTION)) {
            RFGeneratorSource src = new RFGeneratorSource();
            for (File f : classes) {
                String name = f.getAbsolutePath().substring(compiledBytecodeDirectory.getAbsolutePath().length() + 1);
                name = name.replaceAll("\\\\", ".");
                name = name.replaceAll("/", ".");
                if (name.endsWith(".class")) {
                    src.processClasses(Class.forName(name.substring(0, name.length() - 6), false, amendedCL));
                }
            }
            source = src;
        } else {
            System.err.println("Unknown generator type: " + generatorType);
            System.exit(1);
        }

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);

        if (destination.hasErrors()) {
            for (SourceError e : destination.getErrors()) {
                System.err.println(e.toString() + "\n");
            }
            System.exit(1);
        }
    }

}
