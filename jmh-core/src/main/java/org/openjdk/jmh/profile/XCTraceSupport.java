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

import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class XCTraceSupport {
    private static final int ANY_VERSION = 0;
    private static final String HW_FAMILY_PREFIX = "hw.cpufamily: ";
    private static final String HW_TYPE_PREFIX = "hw.cputype: ";
    private static final String HW_SUBTYPE_PREFIX = "hw.cpusubtype: ";
    private static final String KPEP_DIR_PATH = "/usr/share/kpep";
    public static final String INSTRUMENTS_PACKAGE_TEMPLATE = "/xctracenorm.instrpkg";

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

    static Collection<String> recordWithPackageCommandPrefix(String xctracePath, String runFile, File pkg,
                                                             String instrument) {
        return Arrays.asList(
                xctracePath, "record",
                "--package", pkg.getAbsolutePath(),
                "--instrument", instrument,
                "--output", runFile,
                "--target-stdout", "-",
                "--launch", "--"
        );
    }

    /**
     * Returns absolute path to xctrace executable or throws ProfilerException if it does not exist..
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
     * Generates a string uniquely identifying the Instruments package.
     */
    static String generateInstrumentsPackageDigest(int samplingRateMsec, Collection<String> pmuEvents) {
        MessageDigest md;
        try {
            // MD5 is one of the algorithms that have to be supported,
            // and its digest is the shortest one (16 bytes), which might be handy for file names.
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not implemented", e);
        }
        InputStream templateStream = XCTraceSupport.class.getResourceAsStream(INSTRUMENTS_PACKAGE_TEMPLATE);
        byte[] buffer = new byte[1024];
        int bytesRead = -1;
        try {
            while ((bytesRead = templateStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read instruments package template", e);
        }
        md.update(Integer.toString(samplingRateMsec).getBytes(StandardCharsets.UTF_8));
        pmuEvents.stream().sorted().forEachOrdered(event -> md.update(event.getBytes(StandardCharsets.UTF_8)));
        // poor man's byte[] to hexadecimal string conversion
        return new BigInteger(md.digest()).toString(16);
    }

    /**
     * Builds a "CPU Counters" based Instruments package that'll sample {@code pmcEvents}
     * at {@code samplingRateMillis} rate. The package building is performed by {@code instrumentbuilder} tool
     * bundled with Xcode.
     *
     * @param dstPath            a path where a generated package needs to be saved.
     * @param samplingRateMillis the rate at which PMC needs to be sampled, it should be greater than zero.
     * @param pmcEvents          the list of PMC to sample, it should be non-empty.
     */
    static void buildInstrumentsPMCSamplingPackage(File dstPath, long samplingRateMillis, Collection<String> pmcEvents)
            throws ProfilerException {
        if (samplingRateMillis <= 0) {
            throw new IllegalArgumentException(
                    "Sampling rate must be a positive integer, but it is " + samplingRateMillis);
        }
        if (pmcEvents.isEmpty()) {
            throw new IllegalArgumentException("PMC events list must contain at least one event");
        }
        InputStream templateStream = XCTraceSupport.class.getResourceAsStream(INSTRUMENTS_PACKAGE_TEMPLATE);
        String template = new BufferedReader(new InputStreamReader(templateStream))
                .lines()
                .collect(Collectors.joining("\n"));

        template = template.replace("SAMPLING_RATE_MICROS",
                        Long.toString(TimeUnit.MILLISECONDS.toMicros(samplingRateMillis)))
                .replace("PMC_EVENTS_LIST",
                        pmcEvents.stream().map(event -> String.format("<string>%s</string>", event))
                                .collect(Collectors.joining()));

        String instrBuilderPath = getXCodeDevToolsPath().resolve(
                Paths.get("usr", "bin", "instrumentbuilder")).toString();

        File instrpkg;
        try {
            instrpkg = FileUtils.tempFile(".instrpkg");
            Files.write(instrpkg.toPath(), template.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ProfilerException(e);
        }
        Collection<String> output = Utils.runWith(
                instrBuilderPath,
                "-o", dstPath.getAbsolutePath(), // output file
                "-i", instrpkg.getAbsolutePath(), // input package file
                "-l", "CPU Counters" // packages to link with
        );
        output.stream()
                .filter(line -> line.contains("Package distribution file created at"))
                .findFirst()
                .orElseThrow(() -> new ProfilerException("Can't create an Instruments package:\n"
                        + String.join("\n", output)));
        instrpkg.delete();
    }

    /**
     * Returns a string uniquely identified current CPU.
     * <p>
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
     * <p>
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
     * <p>
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
                throw new ProfilerException("Failed to parse a KPEP file: " + kpepFile.getAbsolutePath() +
                        ". Output: " + out);
            }
            return parseKpepXmlFile(tempFile.toFile());
        } catch (IOException e) {
            throw new ProfilerException("Failed to parse a KPEP file: " + kpepFile.getAbsolutePath(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Parses a KPEP database from XML format.
     */
    static PerfEvents parseKpepXmlFile(File kpepXmlFile) throws ProfilerException {
        Map<String, String> aliases = new HashMap<>();
        Map<String, PerfEventInfo> description = new HashMap<>();
        long[] masks = new long[]{0, 0};
        String[] architecture = new String[]{"unknown"};
        parseKpepXmlFileLines(kpepXmlFile, fields -> {
            if (fields.length == 0) return;
            switch (fields[0]) {
                case "alias": // alias description, ["alias", <alias name>, <aliased event>]
                    if (fields.length == 3) {
                        aliases.put(fields[1], fields[2]);
                    }
                    break;
                case "event": { // PMU event info, ["event", <event name>, <is fixed>, <mask>, <description>, <fallback>]
                    String name = fields[1];
                    boolean isFixed = fields.length > 2 && !fields[2].isEmpty();
                    long mask = isFixed ? masks[0] : masks[1];
                    if (fields.length > 3 && !fields[3].isEmpty()) {
                        mask = Long.parseLong(fields[3]);
                    }
                    String desc = fields.length > 4 && !fields[4].isEmpty() ? fields[4] : "No description";
                    String fallback = fields.length > 5 ? fields[5] : null;
                    description.put(name, new PerfEventInfo(name, mask, isFixed, desc, fallback));
                    break;
                }
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

        if (architecture[0].equals("unknown")) {
            throw new ProfilerException("KPEP file was not parsed correctly: CPU architecture info is missing.");
        }

        aliases.forEach((k, v) -> description.put(k, description.get(v)));

        return new PerfEvents(CpuArch.fromString(architecture[0]), masks[0], masks[1], aliases, description);
    }

    private static void parseKpepXmlFileLines(File xmlFile, Consumer<String[]> callback) throws ProfilerException {
        Source kpepXml = new StreamSource(xmlFile);
        Source aliasesXslt = new StreamSource(XCTraceSupport.class.getResourceAsStream("/kpep.plist.xslt"));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamResult aliasesParsed = new StreamResult(new ByteArrayOutputStream());
        try {
            transformerFactory.newTransformer(aliasesXslt).transform(kpepXml, aliasesParsed);
        } catch (TransformerException e) {
            throw new ProfilerException("Failed to transform KPEP XML file", e);
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

    enum CpuArch {
        AARCH64,
        X86_64,
        UNKNOWN;

        static CpuArch fromString(String arch) {
            switch (arch) {
                case "aarch64":
                case "arm64":
                    return AARCH64;
                case "amd64":
                case "x86_64":
                    return X86_64;
                default:
                    return UNKNOWN;
            }
        }
    }

    static class PerfEventInfo {
        private final String name;
        private final long counterMask;
        private final boolean isFixed;
        private final String description;

        private final String fallbackEvent;

        public PerfEventInfo(String name, long counterMask, boolean isFixed, String description, String fallback) {
            this.name = name;
            this.counterMask = counterMask;
            this.isFixed = isFixed;
            this.description = description;
            fallbackEvent = fallback;
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

        public String getFallbackEvent() {
            return fallbackEvent;
        }
    }

    static class PerfEvents {
        private final Map<String, String> eventAliases;
        private final Map<String, PerfEventInfo> supportedEvents;
        private final long fixedCountersMask;
        private final long configurableCountersMask;
        private final CpuArch architecture;

        public PerfEvents(CpuArch architecture, long fixedCountersMask, long configurableCountersMask,
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

        CpuArch getArchitecture() {
            return architecture;
        }

        Set<String> getAliases() {
            return Collections.unmodifiableSet(eventAliases.keySet());
        }

        Collection<PerfEventInfo> getAllEvents() {
            return Collections.unmodifiableCollection(supportedEvents.values());
        }

        PerfEventInfo getEvent(String name) {
            return supportedEvents.get(name);
        }
    }
}
