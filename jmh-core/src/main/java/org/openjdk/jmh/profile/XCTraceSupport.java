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

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class XCTraceSupport {
    private static final int ANY_VERSION = 0;
    private static final String HW_FAMILY_PREFIX = "hw.cpufamily: ";
    private static final String HW_TYPE_PREFIX = "hw.cputype: ";
    private static final String HW_SUBTYPE_PREFIX = "hw.cpusubtype: ";
    private static final String KPEP_DIR_PATH = "/usr/share/kpep";
    private static final String CPU_CYCLES_ARM64 = "Cycles";
    private static final String INSTRUCTIONS_ARM64 = "Instructions";
    private static final String CPU_CYCLES_X86_64 = "CORE_ACTIVE_CYCLE";
    private static final String INSTRUCTIONS_X86_64 = "INST_ALL";

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
     * Returns absolute path to xctrace executable or throws ProfilerException if it does not exist..
     *
     * xctrace is expected to be at $(xcode-select -p)/usr/bin/xctrace
     */
    static String getXCTracePath() throws ProfilerException {
        return getXCTracePath(ANY_VERSION);
    }

    /**
     * Returns absolute path to xctrace executable or throws ProfilerException if it does not exist
     * or its version is below {@code minVersion}.
     *
     * xctrace is expected to be at {@code $(xcode-select -p)/usr/bin/xctrace}
     *
     * @param minVersion a minimum required major xctrace version, like {@code 13}. Use {@code 0} to allow any version.
     */
    static String getXCTracePath(int minVersion) throws ProfilerException {
        Collection<String> out = Utils.tryWith("xcode-select", "-p");
        if (!out.isEmpty()) {
            throw new ProfilerException("\"xcode-select -p\" failed: " + out);
        }
        out = Utils.runWith("xcode-select", "-p");
        String devPath = out.stream().flatMap(l -> Arrays.stream(l.split("\n"))).findFirst().orElseThrow(
                () -> new ProfilerException("\"xcode-select -p\" output is empty"));
        File xctrace = Paths.get(devPath, "usr", "bin", "xctrace").toFile();
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
                        +launchFiles.size() + ": " + launchFiles);
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

    /**
     * Returns a string uniquely identified current CPU.
     *
     * The string has a following format: {@code <hw.cputype>_<hw.cpusubtype>_<hw.cpufamily>}, where
     * each field corresponds to a same titled sysctl property encoded in hexadecimal format.
     */
    static String getCpuIdString() throws ProfilerException {
        List<String> sysctlOut = Utils.runWith("sysctl", "hw")
                .stream()
                .flatMap(line -> Stream.of(line.split("\n")))
                .collect(Collectors.toList());
        int family = -1;
        int type = -1;
        int subtype = -1;
        for (String prop : sysctlOut) {
            if (prop.startsWith(HW_FAMILY_PREFIX)) {
                family = Integer.parseInt(prop.substring(HW_FAMILY_PREFIX.length()));
            } else if (prop.startsWith(HW_TYPE_PREFIX)) {
                type = Integer.parseInt(prop.substring(HW_TYPE_PREFIX.length()));
            } else if (prop.startsWith(HW_SUBTYPE_PREFIX)) {
                subtype = Integer.parseInt(prop.substring(HW_SUBTYPE_PREFIX.length()));
            }
        }
        if (type == -1) throw new ProfilerException("hw.cputype variable was not found");
        if (subtype == -1) throw new ProfilerException("hw.cpusubtype variable was not found");
        if (family == -1) throw new ProfilerException("hw.cpufamily variable was not found");
        return String.format("%s_%s_%s",
                Integer.toUnsignedString(type, 16),
                Integer.toUnsignedString(subtype, 16),
                Integer.toUnsignedString(family, 16));
    }

    /**
     * Returns a database file with PMU description for a current CPU.
     *
     * Usually, such a file has a path like {@code /usr/share/kpep/cpu_100000c_2_8765edea.plist} where
     * the filename part between {@code "cpu_"} and {@code ".plist"} could be obtained from
     * {@link XCTraceSupport#getCpuIdString()}.
     */
    static File getKpepFilePath() throws ProfilerException {
        return new File(KPEP_DIR_PATH, "cpu_" + getCpuIdString() + ".plist");
    }

    /**
     * Parses a given KPEP database file. These files are property list (plist) files located in {@code /usr/share/kpep}
     * and containing information about CPU's performance monitoring unit. Information includes some PMU properties
     * like a number of fixed and configurable counters, but mainly, in contains PMU events description.
     * <p/>
     * While the database itself contains a few entries about a CPU, one has to query sysctl to get enough info to
     * correctly identify the CPU and then use that info to pick up a proper KPEP file. All required machinery is
     * already implemented in {@link XCTraceSupport#getCpuIdString()}.
     * You can use {@link  XCTraceSupport#getKpepFilePath()} to get a full path to what should be a file corresponding
     * to current CPU.
     *
     * @param kpepFile a file with performance monitoring counters database.
     */
    static PerfEvents parseKpepFile(File kpepFile) throws ProfilerException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("", ".xml");
            // Convert a KPEP plist file to XML.
            // Newer plutil versions allow extracting data by path (like xpath) into a textual form directly,
            // but we can't rely on this functionality as it was added in later MacOS versions.
            Collection<String> out = Utils.tryWith("plutil",
                    "-convert", "xml1",
                    "-o", tempFile.toString(),
                    kpepFile.getAbsolutePath());
            if (!out.isEmpty()) {
                throw new ProfilerException("Failed to parse a kpep file: " + kpepFile.getAbsolutePath() +
                        ". Output: " + out);
            }
            return parseKpepXmlFile(tempFile.toFile());
        } catch (IOException e) {
            throw new ProfilerException("Failed to parse a kpep file: " + kpepFile.getAbsolutePath(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    static PerfEvents parseKpepXmlFile(File kpepXmlFile) throws ProfilerException {
        Map<String, String> aliases = new HashMap<>();
        Map<String, PerfEventInfo> description = new HashMap<>();
        long[] masks = new long[] { 0, 0 };
        String[] architecture = new String[] { "unknown" };
        parseKpepXmlFileLines(kpepXmlFile, fields -> {
            if (fields.length == 0) return;
            switch (fields[0]) {
                case "alias": // alias description, ["alias", <alias name>, <aliased event>]
                    if (fields.length == 3) {
                        aliases.put(fields[1], fields[2]);
                    }
                    break;
                case "event": // PMU event info, ["event", <event name>, <is fixed>, <mask>, <description>]
                    if (fields.length >= 4) {
                        String name = fields[1];
                        boolean isFixed = !fields[2].isEmpty();
                        long mask = isFixed ? masks[0] : masks[1];
                        if (!fields[3].isEmpty()) {
                            mask = Long.parseLong(fields[3]);
                        }
                        String desc = fields.length > 4 ? fields[4] : "No description";
                        description.put(name, new PerfEventInfo(name, mask, isFixed, desc));
                    }
                    break;
                case "architecture": // CPU architecture, ["architecture", <arm64|x86_64>]
                    architecture[0] = fields[1];
                    break;
                case "fixed_counters": // Fixed PMU counters bitmask, ["fixed_counters", <mask>]
                    masks[0] = Long.parseLong(fields[1]);
                    break;
                case "config_counters": // Configurable PMU counters bitmask, ["config_counters", <mask>]
                    masks[1] = Long.parseLong(fields[1]);
                    break;
            }
        });

        // To simplify counters configuration, let's add our own linux-perf-flavored aliases
            // to cycles and instructions counters.
            // Depending on architecture, these are tracked by different PMU events.
            switch (architecture[0]) {
                case "arm64":
                    aliases.put(PerfEvents.CPU_CYCLES_META_EVENT, CPU_CYCLES_ARM64);
                    aliases.put(PerfEvents.INSTRUCTIONS_META_EVENT, INSTRUCTIONS_ARM64);
                    break;
                case "x86_64":
                    aliases.put(PerfEvents.CPU_CYCLES_META_EVENT, CPU_CYCLES_X86_64);
                    aliases.put(PerfEvents.INSTRUCTIONS_META_EVENT, INSTRUCTIONS_X86_64);
                    break;
                default:
                    throw new ProfilerException("Unknown architecture: " + architecture[0]);
            }

            return new PerfEvents(architecture[0], masks[0], masks[1], aliases, description);

    }

    // Extracts data we're interested in from KPEP plist's XML representation.
    private static void parseKpepXmlFileLines(File xmlFile, Consumer<String[]> callback) throws ProfilerException {
        Source kpepXml = new StreamSource(xmlFile);
        Source aliasesXslt = new StreamSource(XCTraceSupport.class.getResourceAsStream("/kpep.plist.xslt"));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamResult aliasesParsed = new StreamResult(new ByteArrayOutputStream());
        try {
            transformerFactory.newTransformer(aliasesXslt).transform(kpepXml, aliasesParsed);
        } catch (TransformerException e) {
            throw new ProfilerException("Failed to transform Kpep XML file", e);
        }
        String aliasesText = aliasesParsed.getOutputStream().toString();

        for (String line : aliasesText.split("\n")) {
            // We're using '::' as a separator in case some textual fields contain tabs or other punctuation
            if (!line.contains("::")) {
                continue;
            }
            callback.accept(line.trim().split("::"));
        }
    }

    static class PerfEventInfo {
        private final String name;
        private final long counterMask;
        private final boolean isFixed;
        private final String description;

        public PerfEventInfo(String name, long counterMask, boolean isFixed, String description) {
            this.name = name;
            this.counterMask = counterMask;
            this.isFixed = isFixed;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public long getCounterMask() {
            return counterMask;
        }

        public boolean isFixed() {
            return isFixed;
        }

        public String getDescription() {
            return description;
        }
    }

    static class PerfEvents {
        static final String INSTRUCTIONS_META_EVENT = "instructions";
        static final String CPU_CYCLES_META_EVENT = "cycles";

        private final Map<String, String> eventAliases;
        private final Map<String, PerfEventInfo> supportedEvents;
        private final long fixedCountersMask;
        private final long configurableCountersMask;
        private final String architecture;

        public PerfEvents(String architecture, long fixedCountersMask, long configurableCountersMask,
                          Map<String, String> eventAliases, Map<String, PerfEventInfo> supportedEvents) {
            this.eventAliases = eventAliases;
            this.supportedEvents = supportedEvents;
            this.fixedCountersMask = fixedCountersMask;
            this.configurableCountersMask = configurableCountersMask;
            this.architecture = architecture;
        }

        int getMaxCounters() {
            return Long.bitCount(fixedCountersMask | configurableCountersMask);
        }

        long getFixedCountersMask() {
            return fixedCountersMask;
        }

        long getConfigurableCountersMask() {
            return configurableCountersMask;
        }

        String getArchitecture() {
            return architecture;
        }

        Set<String> getAliases() {
            return Collections.unmodifiableSet(eventAliases.keySet());
        }

        Set<String> getAllAliasedEvents(String event) {
            Set<String> aliases = new HashSet<>();
            while (event != null) {
                String alias = eventAliases.get(event);
                if (alias != null) {
                    aliases.add(alias);
                }
                event = alias;
            }
            return aliases;
        }

        String getAlias(String event) {
            return eventAliases.get(event);
        }

        boolean isSupportedEvent(String event) {
            return supportedEvents.containsKey(event);
        }

        Collection<PerfEventInfo> getAllEvents() {
            return Collections.unmodifiableCollection(supportedEvents.values());
        }

        PerfEventInfo getEvent(String name) {
            return supportedEvents.get(name);
        }
    }
}
