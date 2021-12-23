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
package org.openjdk.jmh.generators.annotations;

import org.openjdk.jmh.generators.core.GeneratorDestination;
import org.openjdk.jmh.generators.core.MetadataInfo;
import org.openjdk.jmh.util.Utils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.*;

public class APGeneratorDestinaton implements GeneratorDestination {

    private final ProcessingEnvironment processingEnv;

    public APGeneratorDestinaton(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public OutputStream newResource(String resourcePath) throws IOException {
        return processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath).openOutputStream();
    }

    @Override
    public InputStream getResource(String resourcePath) throws IOException {
        return processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", resourcePath).openInputStream();
    }

    @Override
    public Writer newClass(String className, String originatingClassName) throws IOException {
        Filer filer = processingEnv.getFiler();
        if (originatingClassName != null) {
            TypeElement originatingType = processingEnv.getElementUtils().getTypeElement(originatingClassName);
            return filer.createSourceFile(className, originatingType).openWriter();
        } else {
            return filer.createSourceFile(className).openWriter();
        }
    }

    @Override
    public void printError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public void printError(String message, MetadataInfo element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, ((APMetadataInfo)element).getElement());
    }

    @Override
    public void printError(String message, Throwable throwable) {
        printError(message + " " + Utils.throwableToString(throwable));
    }

    @Override
    public void printWarning(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    @Override
    public void printWarning(String message, MetadataInfo element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, ((APMetadataInfo)element).getElement());
    }

    @Override
    public void printWarning(String message, Throwable throwable) {
        printWarning(message + " " + Utils.throwableToString(throwable));
    }

    @Override
    public void printNote(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }
}
