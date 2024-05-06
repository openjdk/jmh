/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.it.profilers;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class XCTraceSupportTest {
    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private static void runOnlyOnMac() {
        Assume.assumeTrue("Make sense only on MacOS",
                System.getProperty("os.name").toLowerCase().contains("mac"));
    }

    private static void runOnlyIfXCodeInstalled() {
        Collection<String> out = Utils.tryWith("xcode-select", "-p");
        Assume.assumeTrue("xcode-select failed: " + out, out.isEmpty());
    }

    private static class KpepParser {
        private final Method parseKpepFile;

        KpepParser() throws ReflectiveOperationException {
            // XCTraceSupport is package-private in jmh-core, but jmh-core-it is a much more suitable place to
            // run tests of this kind. That's why we're doing all that murky stuff.
            Class<?> xctraceSupport = Class.forName("org.openjdk.jmh.profile.XCTraceSupport");
            parseKpepFile = xctraceSupport.getDeclaredMethod("parseKpepFile", File.class);
            parseKpepFile.setAccessible(true);
        }

        Object parseKpepFile(File file) throws ReflectiveOperationException {
            return parseKpepFile.invoke(null, file);
        }
    }

    @Test
    public void testKpepFilesParsing() throws Exception {
        runOnlyOnMac();

        File kpepDir = new File("/usr/share/kpep");
        Assume.assumeTrue("kpep files dir not found: " + kpepDir.getAbsolutePath(), kpepDir.canExecute());

        File[] plistFiles = kpepDir.listFiles((dir, name) -> name.endsWith(".plist"));
        Assert.assertNotNull(plistFiles);

        KpepParser parser = new KpepParser();

        for (File kpepFile : plistFiles) {
            Object db;
            try {
                db = parser.parseKpepFile(kpepFile);
            } catch (Throwable e) {
                throw new AssertionError("Failed to parse " + kpepFile.getAbsolutePath(), e);
            }
            Assert.assertNotNull("parseKpepFile returned null for " + kpepFile.getAbsolutePath(), db);
        }
    }

    private void checkKpepParsingFailed(File plistFile) throws Exception {
        KpepParser parser = new KpepParser();
        try {
            parser.parseKpepFile(plistFile);
            Assert.fail();
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof ProfilerException)) {
                throw new AssertionError("Profiler exception was expected", e.getCause());
            }
        }
    }

    @Test
    public void testIllFormattedKpepParsing() throws Exception {
        runOnlyOnMac();

        File tempFile = FileUtils.tempFile(".plist");
        try {
            // plutil converts text file to xml, so that would result
            // in a successfully generated xml document with a wrong schema
            Files.write(tempFile.toPath(), "123".getBytes(StandardCharsets.UTF_8));

            checkKpepParsingFailed(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testInvalidKpepParsing() throws Exception {
        runOnlyOnMac();

        File tempFile = FileUtils.tempFile(".plist");
        try {
            // Generate a file that is not a valid plist file
            byte[] bytes = new byte[1024];
            new Random().nextBytes(bytes);
            Files.write(tempFile.toPath(), bytes);

            checkKpepParsingFailed(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testInstrumentsPackage() throws Exception {
        runOnlyOnMac();
        runOnlyIfXCodeInstalled();

        Class<?> xctraceSupport = Class.forName("org.openjdk.jmh.profile.XCTraceSupport");
        Method buildPkg = xctraceSupport.getDeclaredMethod("buildInstrumentsPMCSamplingPackage",
                File.class, long.class, Collection.class);
        buildPkg.setAccessible(true);

        File generatedPackage = new File(TMP_DIR, UUID.randomUUID() + ".pkg");
        try {
            buildPkg.invoke(null, generatedPackage, 100L, Arrays.asList("INST_ALL", "CORE_ACTIVE_CYCLE"));
            Assert.assertTrue(generatedPackage.exists());
        } finally {
            generatedPackage.delete();
        }
    }
}
