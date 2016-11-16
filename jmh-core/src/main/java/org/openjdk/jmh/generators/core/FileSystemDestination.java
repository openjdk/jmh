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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileSystemDestination implements GeneratorDestination {

    private final File resourceDir;
    private final File sourceDir;
    private final List<SourceError> sourceErrors;
    private final List<SourceWarning> sourceWarnings;

    public FileSystemDestination(File resourceDir, File sourceDir) {
        this.resourceDir = resourceDir;
        this.sourceDir = sourceDir;
        this.sourceErrors = new ArrayList<>();
        this.sourceWarnings = new ArrayList<>();
    }

    @Override
    public Writer newResource(String resourcePath) throws IOException {
        String pathName = resourceDir.getAbsolutePath() + "/" + resourcePath;
        File p = new File(pathName.substring(0, pathName.lastIndexOf("/")));
        if (!p.mkdirs() && !p.isDirectory()) {
            throw new IOException("Unable to create " + p.getAbsolutePath());
        }
        return new FileWriter(new File(pathName));
    }

    @Override
    public Reader getResource(String resourcePath) throws IOException {
        String pathName = resourceDir.getAbsolutePath() + "/" + resourcePath;
        File p = new File(pathName.substring(0, pathName.lastIndexOf("/")));
        if (!p.isFile() && !p.exists()) {
            throw new IOException("Unable to open " + pathName);
        }
        return new FileReader(new File(pathName));
    }

    @Override
    public Writer newClass(String className) throws IOException {
        String pathName = sourceDir.getAbsolutePath() + "/" + className.replaceAll("\\.", "/");
        File p = new File(pathName.substring(0, pathName.lastIndexOf("/")));
        if (!p.mkdirs() && !p.isDirectory()) {
            throw new IOException("Unable to create " + p.getAbsolutePath());
        }
        return new FileWriter(new File(pathName + ".java"));
    }

    @Override
    public void printError(String message) {
        sourceErrors.add(new SourceError(message));
    }

    @Override
    public void printError(String message, MetadataInfo element) {
        sourceErrors.add(new SourceElementError(message, element));
    }

    @Override
    public void printError(String message, Throwable throwable) {
        sourceErrors.add(new SourceThrowableError(message, throwable));
    }

    public boolean hasErrors() {
        return !sourceErrors.isEmpty();
    }

    public Collection<SourceError> getErrors() {
        return sourceErrors;
    }

    @Override
    public void printWarning(String message) {
        sourceWarnings.add(new SourceWarning(message));
    }

    @Override
    public void printWarning(String message, MetadataInfo element) {
        sourceWarnings.add(new SourceElementWarning(message, element));
    }

    @Override
    public void printWarning(String message, Throwable throwable) {
        sourceWarnings.add(new SourceThrowableWarning(message, throwable));
    }

    public boolean hasWarnings() {
        return !sourceWarnings.isEmpty();
    }

    public Collection<SourceWarning> getWarnings() {
        return sourceWarnings;
    }

    @Override
    public void printNote(String message) {
        // do nothing
    }
}
