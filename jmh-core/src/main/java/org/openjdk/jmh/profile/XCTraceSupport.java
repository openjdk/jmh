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
package org.openjdk.jmh.profile;

import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class XCTraceSupport {
    private static final int ANY_VERSION = 0;

    private XCTraceSupport() {
    }

    static void exportTable(String xctracePath, String runFile, String outputFile,
                            XCTraceTableHandler.ProfilingTableType table) {
        Collection<String> out = Utils.tryWith(
                xctracePath, "export",
                "--input", runFile,
                "--output", outputFile,
                "--xpath",
                "/trace-toc/run/data/table[@schema=\"" + table.tableName + "\"]"
        );
        if (!out.isEmpty()) {
            throw new IllegalStateException(out.toString());
        }
    }

    static void exportTableOfContents(String xctracePath, String runFile, String outputFile) {
        Collection<String> out = Utils.tryWith(
                xctracePath, "export",
                "--input", runFile,
                "--output", outputFile,
                "--toc"
        );
        if (!out.isEmpty()) {
            throw new IllegalStateException(out.toString());
        }
    }

    static Collection<String> recordCommandPrefix(String xctracePath, String runFile, String template) {
        return Arrays.asList(
                xctracePath, "record",
                "--template", template,
                "--output", runFile,
                "--target-stdout", "-",
                "--launch", "--"
        );
    }

    /**
     * Returns absolute path to xctrace executable or throws ProfilerException if it does not exist.
     * <p>
     * xctrace is expected to be at $(xcode-select -p)/usr/bin/xctrace
     */
    static String getXCTracePath() throws ProfilerException {
        return getXCTracePath(ANY_VERSION);
    }

    static Path getXCodeDevToolsPath() throws ProfilerException {
        Collection<String> out = Utils.tryWith("xcode-select", "-p");
        if (!out.isEmpty()) {
            throw new ProfilerException("\"xcode-select -p\" failed: " + out);
        }
        out = Utils.runWith("xcode-select", "-p");
        String devPath = out.stream().flatMap(l -> Arrays.stream(l.split("\n"))).findFirst().orElseThrow(
                () -> new ProfilerException("\"xcode-select -p\" output is empty"));
        return Paths.get(devPath);
    }

    /**
     * Returns absolute path to xctrace executable or throws ProfilerException if it does not exist
     * or its version is below {@code minVersion}.
     * <p>
     * xctrace is expected to be at {@code $(xcode-select -p)/usr/bin/xctrace}
     *
     * @param minVersion a minimum required major xctrace version, like {@code 13}. Use {@code 0} to allow any version.
     */
    static String getXCTracePath(int minVersion) throws ProfilerException {
        File xctrace = getXCodeDevToolsPath().resolve(Paths.get("usr", "bin", "xctrace")).toFile();
        String xctracePath = xctrace.getAbsolutePath();
        if (!xctrace.exists()) {
            throw new ProfilerException("xctrace was not found at " + xctracePath);
        }
        Collection<String> versionOut = Utils.runWith(xctracePath, "version");
        String versionString = versionOut.stream().flatMap(l -> Arrays.stream(l.split("\n")))
                .filter(l -> l.contains("xctrace version"))
                .findFirst()
                .orElseThrow(() -> new ProfilerException("\"xctrace version\" failed: " + versionOut));

        checkVersion(versionString, minVersion);

        return xctrace.getAbsolutePath();
    }

    private static void checkVersion(String versionString, int minVersion) throws ProfilerException {
        String extractedVersion = versionString.split("xctrace version ")[1].split(" ")[0];
        int majorVersion = Integer.parseInt(extractedVersion.split("\\.")[0]);

        if (majorVersion < minVersion) {
            throw new ProfilerException(
                    "xctrace version (" + versionString + ") is too low (required at least " + minVersion + ").");
        }
    }

    static Path findTraceFile(Path parent) {
        try (Stream<Path> files = Files.list(parent)) {
            List<Path> launchFiles = files
                    .filter(path -> path.getFileName().toString().startsWith("Launch"))
                    .collect(Collectors.toList());
            if (launchFiles.size() != 1) {
                throw new IllegalStateException("Expected only one launch file, found " +
                        launchFiles.size() + ": " + launchFiles);
            }
            return launchFiles.get(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void removeDirectory(Path path) {
        if (!path.toFile().exists()) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Path createTemporaryDirectoryName() {
        // In general, it's unsafe to create a random file name and then create a file/dir itself.
        // But it should be fine for profiling purposes.
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) {
            throw new IllegalStateException("System temporary folder is unknown.");
        }
        for (int i = 0; i < 5; i++) {
            String dirname = "jmh-xctrace-results-" + System.nanoTime();
            Path path = Paths.get(tempDir, dirname);
            if (!path.toFile().exists()) {
                return path;
            }
        }
        throw new IllegalStateException("Can't create a temporary folder for a run.");
    }

    static void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path destPath = destination.resolve(source.relativize(dir));
                Files.copy(dir, destPath);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path destFilePath = destination.resolve(source.relativize(file));
                Files.copy(file, destFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
