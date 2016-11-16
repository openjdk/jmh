/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class CompilerHints extends AbstractResourceReader {

    public static final String LIST = "/META-INF/CompilerHints";

    // All OpenJDK/HotSpot VMs are supported
    static final String[] HINT_COMPATIBLE_JVMS = { "OpenJDK", "HotSpot" };
    // Zing is only compatible from post 5.10.*.* releases
    static final String JVM_ZING = "Zing";

    private static volatile CompilerHints defaultList;
    private static volatile String hintsFile;


    private final Set<String> hints;

    static final String XX_COMPILE_COMMAND_FILE = "-XX:CompileCommandFile=";

    public static CompilerHints defaultList() {
        if (defaultList == null) {
            defaultList = fromResource(LIST);
        }
        return defaultList;
    }

    public static String hintsFile() {
        if (hintsFile == null) {
            try {
                final Set<String> defaultHints = defaultList().get();
                List<String> hints = new ArrayList<>(defaultHints.size() + 2);
                hints.add("quiet");
                if (Boolean.getBoolean("jmh.blackhole.forceInline")) {
                    hints.add("inline,org/openjdk/jmh/infra/Blackhole.*");
                } else {
                    hints.add("dontinline,org/openjdk/jmh/infra/Blackhole.*");
                }
                hints.addAll(defaultHints);
                hintsFile = FileUtils.createTempFileWithLines("compilecommand", hints);
            } catch (IOException e) {
                throw new IllegalStateException("Error creating compiler hints file", e);
            }
        }
        return hintsFile;
    }

    public static CompilerHints fromResource(String resource) {
        return new CompilerHints(null, resource);
    }

    public static CompilerHints fromFile(String file) {
        return new CompilerHints(file, null);
    }

    private CompilerHints(String file, String resource) {
        super(file, resource, null);
        hints = Collections.unmodifiableSet(read());
    }

    /**
     * FIXME (low priority): check if supplied JVM is hint compatible. This test is applied to the Runner VM,
     * not the Forked and may therefore be wrong if the forked VM is not the same JVM
     */
    private static boolean isHintCompatibleVM() {
        String name = System.getProperty("java.vm.name");
        for (String vmName : HINT_COMPATIBLE_JVMS) {
            if (name.contains(vmName)) {
                return true;
            }
        }
        if (name.contains(JVM_ZING)) {
            // 1.*.0-zing_*.*.*.*
            String version = System.getProperty("java.version");
            try {
                // get the version digits
                String[] versionDigits = version.substring(version.indexOf('_') + 1).split("\\.");
                if (Integer.valueOf(versionDigits[0]) > 5) {
                    return true;
                } else if (Integer.valueOf(versionDigits[0]) == 5 && Integer.valueOf(versionDigits[1]) >= 10) {
                    return true;
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // unknown Zing version format
                System.err.println("ERROR: Zing version format does not match 1.*.0-zing_*.*.*.*");
            }
        }
        return false;
    }

    public Set<String> get() {
        return hints;
    }

    private Set<String> read() {
        Set<String> result = new TreeSet<>();

        try {
            for (Reader r : getReaders()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(r);
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if (line.startsWith("#")) {
                            continue;
                        }

                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        result.add(line);
                    }
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading compiler hints", ex);
        }

        return result;
    }

    /**
     * @param command command arguments list
     * @return the compiler hint files specified by the command
     */
    public static List<String> getCompileCommandFiles(List<String> command){
        List<String> compileCommandFiles = new ArrayList<>();
        for (String cmdLineWord : command) {
            if (cmdLineWord.startsWith(XX_COMPILE_COMMAND_FILE)) {
                compileCommandFiles.add(cmdLineWord.substring(XX_COMPILE_COMMAND_FILE.length()));
            }
        }
        return compileCommandFiles;
    }

    /**
     * We need to generate a compiler hints file such that it includes:
     * <ul>
     * <li> No compile command files are specified and no .hotspotrc file is available, then do JMH hints only
     * <li> No compile command files are specified and .hotspotrc file is available, then do JMH hints + .hotspotrc
     * <li> 1 to N compile command files are specified, then do JMH hints + all specified hints in files
     * </ul>
     * <p>This is a departure from default JVM behavior as the JVM would normally just take the last hints file and ignore
     * the rest.
     *
     * @param command all -XX:CompileCommandLine args will be removed and a merged file will be set
     */
    public static void addCompilerHints(List<String> command) {
        if (!isHintCompatibleVM()) {
            System.err.println("WARNING: Not a HotSpot compiler command compatible VM (\""
                    + System.getProperty("java.vm.name") + "-" + System.getProperty("java.version")
                    + "\"), compilerHints are disabled.");
            return;
        }

        List<String> hintFiles = new ArrayList<>();
        hintFiles.add(hintsFile());
        removeCompileCommandFiles(command, hintFiles);
        if (hintFiles.size() == 1) {
            File hotspotCompilerFile = new File(".hotspot_compiler");
            if (hotspotCompilerFile.exists()) {
                hintFiles.add(hotspotCompilerFile.getAbsolutePath());
            }
        }
        command.add(CompilerHints.XX_COMPILE_COMMAND_FILE + mergeHintFiles(hintFiles));
    }

    /**
     * @param command the compile command file options will be removed from this command
     * @param compileCommandFiles the compiler hint files specified by the command will be added to this list
     */
    private static void removeCompileCommandFiles(List<String> command, List<String> compileCommandFiles){
        Iterator<String> iterator = command.iterator();
        while (iterator.hasNext()) {
            String cmdLineWord = iterator.next();
            if(cmdLineWord.startsWith(XX_COMPILE_COMMAND_FILE)) {
                compileCommandFiles.add(cmdLineWord.substring(XX_COMPILE_COMMAND_FILE.length()));
                iterator.remove();
            }
        }
    }

    private static String mergeHintFiles(List<String> compileCommandFiles) {
        if (compileCommandFiles.size() == 1) {
            return compileCommandFiles.get(0);
        }
        try {
            Set<String> hints = new TreeSet<>();
            for(String file : compileCommandFiles) {
                hints.addAll(fromFile(file).get());
            }
            return FileUtils.createTempFileWithLines("compilecommand", hints);
        } catch (IOException e) {
            throw new IllegalStateException("Error merging compiler hints files", e);
        }
    }
}
