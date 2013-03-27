/**
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
package org.openjdk.jmh.runner.options;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openjdk.jmh.ForkedMain;
import org.openjdk.jmh.output.OutputFormatType;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.options.handlers.ForkOptionHandler;
import org.openjdk.jmh.runner.parameters.Defaults;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Class that handles all the options and arguments specific to the harness JVM.
 *
 * @author anders.astrand@oracle.com
 * @author sergey.kuksenko@oracle.com
 */
public class HarnessOptions extends BaseOptions {


    // test selection options
    @Argument(metaVar = "REGEXP", usage = "Microbenchmarks to run. Regexp filtering out classes or methods which are MicroBenchmarks.")
    private List<String> regexps = new ArrayList<String>();

    // micro options
    // the following list of options are not forwarding into forked JVM.
    // "forwarding option should be declared in BaseOptions class" (c) capt. O.

    @Option(name = "-f", aliases = {"--fork"}, metaVar = "{ INT }", usage = "Start each benchmark in new JVM, forking from the same JDK unless --jvm is set. Optional parameter specifies number of times harness should fork. Zero forks means \"no fork\", also \"false\" is accepted", handler = ForkOptionHandler.class)
    private int fork = -1;

    @Option(name = "-o", aliases = {"--output"}, metaVar = "FILE", usage = "Redirect output to FILE")
    private String output = null;

    @Option(name = "-of", aliases = {"--outputformat"}, metaVar = "FORMAT", usage = "Format to use for output, use --listformats to list available formats")
    private OutputFormatType outputFormat = OutputFormatType.TextReport;

    @Option(name = "--jvm", metaVar = "JVM", usage = "Custom JVM to use with fork.")
    private String jvm = null;

    @Option(name = "--jvmargs", metaVar = "JVMARGS", usage = "Custom JVM arguments for --jvm, default is to use parent process's arguments")
    private String jvmArgs = null;

    @Option(name = "--jvmclasspath", metaVar = "CLASSPATH", usage = "Custom classpath for --jvm, default is to use parent process's classpath")
    private String jvmClassPath = null;

    @Option(name = "-e", aliases = {"--exclude"}, multiValued = true, metaVar = "REGEXP", usage = "Microbenchmarks to exclude. Regexp filtering out classes or methods which are MicroBenchmarks.")
    private List<String> excludes = new ArrayList<String>();

    @Option(name = "-wm", aliases = {"--warmupmode"}, usage = "Warmup mode for warming up selected micro benchmarks. Warmup modes are BeforeAny (measurements) or BeforeEach (measurement) (original mode)")
    private WarmupMode warmupMode = WarmupMode.BEFOREEACH;

    @Option(name = "-wmb", aliases = {"--warmupmicrobenchmarks"}, multiValued = true, metaVar = "REGEXP", usage = "Microbenchmarks to run for warmup before running any other benchmarks. These micros may be different from the target micros to warm up the harness or other parts of the JVM prior to running the target micro benchmarks. Regexp filtering out classes or methods which are MicroBenchmarks.")
    private List<String> warmupMicros = new ArrayList<String>();

    // show something options
    @Option(name = "-l", aliases = {"--list"}, usage = "List available microbenchmarks and exit. Filter using available regexps.")
    private boolean list = false;

    @Option(name = "--listformats", usage = "List available output formats")
    private boolean listFormats = false;

    @Option(name = "-h", aliases = {"--help"}, usage = "Display help")
    private boolean help = false;

    @Option(name = "--listprofilers", usage = "List available profilers")
    private boolean listProfilers = false;

    /**
     * Warmup Mode enum
     */
    public enum WarmupMode {
        BEFOREANY, BEFOREEACH
    }

    /**
     * Kawaguchi's parser
     */
    private CmdLineParser parser;

    public static HarnessOptions newInstance() {
        HarnessOptions opts = new HarnessOptions();
        opts.parser = new CmdLineParser(opts);
        return opts;
    }

    private HarnessOptions() {
    }

    /**
     * Print Usage
     *
     * @param message Message to print at the top
     */
    public void printUsage(String message) {
        System.err.println(message);
        System.err.println("Usage: [options] <benchmark regexp> <benchmark regexp>");
        parser.printUsage(System.err);
    }

    /**
     * parse arguments and set fields in the Options instance
     *
     * @throws CmdLineException
     */
    public void parseArguments(String[] argv) throws CmdLineException {
        parser.parseArgument(argv);
    }

    /**
     * Helper method for assembling the command to execute the forked JVM with
     *
     * @param benchmark benchmark to execute
     * @param host host VM host
     * @param port host VM port
     * @return the final command to execute
     */
    public String getSeparateExecutionCommand(String benchmark, String annJvmArgs, String annJvmArgsPrepend, String annJvmArgsAppend, String host, int port) {

        Properties props = System.getProperties();
        String javaHome = (String) props.get("java.home");
        String separator = File.separator;
        String osName = props.getProperty("os.name");
        boolean isOnWindows = osName.contains("indows");
        String platformSpecificBinaryPostfix = isOnWindows ? ".exe" : "";

        String classPath;

        if (getJvmClassPath() != null) {
            classPath = getJvmClassPath();
        } else {
            classPath = (String) props.get("java.class.path");
        }

        if (isOnWindows) {
            classPath = '"' + classPath + '"';
        }

        String javaExecutableString;

        // use supplied jvm if given
        if (getJvm() != null) {
            javaExecutableString = getJvm();
        } else {
            // else find out which one parent is and use that
            StringBuilder javaExecutable = new StringBuilder();
            javaExecutable.append(javaHome);
            javaExecutable.append(separator);
            javaExecutable.append("bin");
            javaExecutable.append(separator);
            javaExecutable.append("java");
            javaExecutable.append(platformSpecificBinaryPostfix);
            javaExecutableString = javaExecutable.toString();
        }


        String jvmArgumentsString;
        if (getJvmArgs() != null) { // use supplied jvm args if given in cmd line
            jvmArgumentsString = getJvmArgs();
        } else if (annJvmArgs != null) { // use jvm args supplied in annotation which shuns implicit args
            jvmArgumentsString = annJvmArgs;
        } else {
            // else use same jvm args given to this runner
            StringBuilder jvmArguments = new StringBuilder();
            RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
            List<String> args = RuntimemxBean.getInputArguments();

            // prepend jvm args
            if (annJvmArgsPrepend != null) {
                jvmArguments.append(annJvmArgsPrepend).append(' ');
            }

            for (String arg : args) {
                jvmArguments.append(arg);
                jvmArguments.append(' ');
            }

            // append jvm args
            if (annJvmArgsAppend != null) {
                jvmArguments.append(annJvmArgsAppend).append(' ');
            }

            // with sweet love from Sweden: remove final delimiter
            if (jvmArguments.length() > 0) {
                jvmArguments.setLength(jvmArguments.length() - 1);
            }

            jvmArgumentsString = jvmArguments.toString();
        }

        // add any compiler oracle hints
        String compilerHintsString = "";
        {
            StringBuilder compilerHints = new StringBuilder();
            Set<String> hints = CompilerHints.defaultList().get();
            if (!hints.isEmpty()) {
                compilerHints.append("-XX:CompileCommand=quiet ");
            }
            for (String l : hints) {
                compilerHints.append("-XX:CompileCommand=");
                compilerHints.append(l);
                compilerHints.append(' ');
            }
            compilerHintsString = compilerHints.toString();
        }

        // assemble final process command

        StringBuilder command = new StringBuilder();
        command.append(javaExecutableString);

        if (!jvmArgumentsString.isEmpty()) {
            command.append(' ');
            command.append(jvmArgumentsString);
        }

        command.append(' ');
        command.append(compilerHintsString);

        command.append(" -cp ");
        command.append(classPath);
        command.append(' ');
        command.append(ForkedMain.class.getName());

        // jvm command
        String jvmCommand = command.toString();

        // if user supplied micro flags, give those as well
        String argumentCommand = this.toCommandLine();

        String connectionString = " --hostName " + host + " --hostPort " + port;

        return jvmCommand + ' ' + benchmark + (!argumentCommand.isEmpty() ? ' ' + argumentCommand : "") + connectionString;
    }


    /**
     * Getter
     *
     * @return the value
     */
    public WarmupMode getWarmupMode() {
        return warmupMode;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public List<String> getRegexps() {
        return regexps;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public List<String> getWarmupMicros() {
        return warmupMicros;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldList() {
        return list;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public String getJvm() {
        return jvm;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public String getJvmArgs() {
        return jvmArgs;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public String getJvmClassPath() {
        return jvmClassPath;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public int getForkCount() {
        return fork;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public String getOutput() {
        return output;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public OutputFormatType getOutputFormat() {
        return outputFormat;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldListFormats() {
        return listFormats;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldHelp() {
        return help;
    }


    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldListProfilers() {
        return listProfilers;
    }

}
