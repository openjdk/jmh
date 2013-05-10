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
package org.openjdk.jmh.runner;

import org.openjdk.jmh.output.format.OutputFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Helper class for listing micro benchmarks.
 *
 * @author staffan.friberg@oracle.com, anders.astrand@oracle.com
 */
public class MicroBenchmarkList extends AbstractResourceReader {

    /** Location of the pre-compiled list of micro benchmarks */
    public static final String MICROBENCHMARK_LIST = "/META-INF/MicroBenchmarks";
    /** String that points out our resource with micro benchmarks */

    public static MicroBenchmarkList defaultList() {
        return fromResource(MICROBENCHMARK_LIST);
    }

    public static MicroBenchmarkList fromResource(String resource) {
        return new MicroBenchmarkList(null, resource, null);
    }

    public static MicroBenchmarkList fromFile(String file) {
        return new MicroBenchmarkList(file, null, null);
    }

    public static MicroBenchmarkList fromString(String line) {
        return new MicroBenchmarkList(null, null, line);
    }

    private MicroBenchmarkList(String file, String resource, String line) {
        super(file, resource, line);
    }

    /**
     * Gets all micro benchmarks from the list, sorted.
     *
     * @param excludes List of regexps to match excludes against
     * @return A list of all benchmarks, excluding matched
     */
    public Set<String> getAll(OutputFormat out, List<String> excludes) {
        return find(out, ".*", excludes);
    }

    /**
     * Gets all the micro benchmarks that matches the given regexp, sorted.
     *
     * @param regexp   Regexp to match against
     * @param excludes List of regexps to match excludes against
     * @return Names of all micro benchmarks in the list that matches the include and NOT matching excludes
     */
    public Set<String> find(OutputFormat out, String regexp, List<String> excludes) {
        return find(out, Collections.singletonList(regexp), excludes);
    }

    /**
     * Gets all the micro benchmarks that matches the given regexp, sorted
     *
     * @param regexps  List of regexps to match against
     * @param excludes List of regexps to match excludes against
     * @return Names of all micro benchmarks in the list that matches includes and NOT matching excludes
     */
    public Set<String> find(OutputFormat out, List<String> regexps, List<String> excludes) {

        // compile all patterns
        List<Pattern> includePatterns = new ArrayList<Pattern>(regexps.size());
        for (String regexp : regexps) {
            includePatterns.add(Pattern.compile(regexp));
        }
        List<Pattern> excludePatterns = new ArrayList<Pattern>(excludes.size());
        for (String regexp : excludes) {
            excludePatterns.add(Pattern.compile(regexp));
        }

        // find all benchmarks matching pattern
        Set<String> result = new TreeSet<String>();
        try {
            for (Reader r : getReaders()) {
                BufferedReader reader = new BufferedReader(r);

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.startsWith("#")) {
                        continue;
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    for (Pattern pattern : includePatterns) {
                        if (pattern.matcher(line).matches()) {
                            boolean exclude = false;

                            // excludes override
                            for (Pattern excludePattern : excludePatterns) {
                                if (excludePattern.matcher(line).matches()) {
                                    out.verbosePrintln("Excluding " + line + ", matches " + excludePattern);

                                    exclude = true;
                                    break;
                                }
                            }

                            if (!exclude) {
                                result.add(line);
                            }
                            break;
                        } else {
                            out.verbosePrintln("Excluding: " + line + ", does not match " + pattern);
                        }
                    }
                }

                reader.close();
            }

        } catch (IOException ex) {
            throw new RuntimeException("Error reading microbenchmark list", ex);
        }

        return result;
    }

}
