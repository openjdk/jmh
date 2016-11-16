/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.generators.core.GeneratorDestination;
import org.openjdk.jmh.generators.core.MetadataInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryGeneratorDestination implements GeneratorDestination {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    private final Map<String, StringWriter> classBodies = new HashMap<>();
    private final Map<String, StringWriter> resourceBodies = new HashMap<>();

    @Override
    public Writer newResource(String resourcePath) throws IOException {
        StringWriter sw = new StringWriter();
        resourceBodies.put(resourcePath, sw);
        return new PrintWriter(sw, true);
    }

    @Override
    public Reader getResource(String resourcePath) throws IOException {
        StringWriter sw = resourceBodies.get(resourcePath);
        if (sw == null) {
            throw new IOException("Does not exist: " + resourcePath);
        }
        return new StringReader(sw.toString());
    }

    @Override
    public Writer newClass(String className) throws IOException {
        StringWriter sw = new StringWriter();
        classBodies.put(className, sw);
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

    @Override
    public void printWarning(String message) {
        warnings.add(message);
    }

    @Override
    public void printWarning(String message, MetadataInfo element) {
        warnings.add(message);
    }

    @Override
    public void printWarning(String message, Throwable throwable) {
        warnings.add(message + ":\n" + throwable.toString());
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public Map<String, String> getClasses() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, StringWriter> e : classBodies.entrySet()) {
            result.put(e.getKey(), e.getValue().toString());
        }
        return result;
    }

    public Map<String, String> getResources() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, StringWriter> e : resourceBodies.entrySet()) {
            result.put(e.getKey(), e.getValue().toString());
        }
        return result;
    }

    @Override
    public void printNote(String message) {
        infos.add(message);
    }

    public boolean hasNotes() {
        return !infos.isEmpty();
    }

    public List<String> getNotes() {
        return infos;
    }

}
