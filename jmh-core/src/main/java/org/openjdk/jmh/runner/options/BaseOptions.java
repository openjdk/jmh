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
package org.openjdk.jmh.runner.options;

import org.kohsuke.args4j.Option;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.runner.options.handlers.BenchmarkModeTypeOptionHandler;
import org.openjdk.jmh.runner.options.handlers.BooleanOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ProfilersOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ThreadCountsOptionHandler;
import org.openjdk.jmh.runner.options.handlers.ThreadsOptionHandler;
import org.openjdk.jmh.runner.options.handlers.TimeUnitOptionHandler;
import org.openjdk.jmh.runner.options.handlers.TimeValueOptionHandler;
import org.openjdk.jmh.runner.parameters.Defaults;
import org.openjdk.jmh.runner.parameters.TimeValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Class that handles options which are omni-present everywhere.
 *
 * Boolean/boolean options getters name conventions:
 * - method name is prefixed by "is" or "should" when the Option class gives exact answer
 * - method name is prefixed by "get" when the method is just a getter and meaning of the option clarified somewhere else
 *
 * @author sergey.kuksenko@oracle.com
 */
public class BaseOptions {


    /*
     *  Conventions for options processing (unless otherwise specified):
     *  - int options:
     *              negative value means unset
     *  - Boolean options:
     *              null means unset, TRUE/FALSE means true/false;
     *              default values should be processed explicitly
     *  - boolean options:
     *              may be used only for options with false default value
     *              may be set to true in cmdLine, can't be set to false explicitly
     *
     */

    @Option(name = "-i", aliases = {"--iterations"}, metaVar = "INT", usage = "Number of iterations.")
    protected int iterations = -1;

    @Option(name = "-r", aliases = {"--runtime"}, metaVar = "TIME", usage = "Run time for each iteration. Examples: 100s, 200ms; defaults to " + Defaults.ITERATION_TIME_SECS + "s", handler = TimeValueOptionHandler.class)
    protected TimeValue runTime = null;

    @Option(name = "-wi", aliases = {"--warmupiterations"}, metaVar = "INT", usage = "Number of warmup iterations to run.")
    protected int warmupIterations = -1;

    @Option(name = "-w", aliases = {"--warmup"}, metaVar = "TIME", usage = "Run time for warmup iterations. Result not used when calculating score. Examples 100s, 200ms; defaults to " + Defaults.WARMUP_TIME_SECS + "", handler = TimeValueOptionHandler.class)
    protected TimeValue warmupTime = null;

    @Option(name = "-bm", aliases = {"--mode"}, multiValued = false, metaVar = "MODE", usage = "Benchmark mode", handler = BenchmarkModeTypeOptionHandler.class)
    protected List<Mode> benchMode = null;

    @Option(name = "-t", aliases = {"--threads"}, usage = "Number of threads to run the microbenchmark with. Special value \"max\" or 0 will use Runtime.availableProcessors()", handler = ThreadsOptionHandler.class)
    protected int threads = -1;

    @Option(name = "-si", aliases = {"--synciterations"}, usage = "Should the harness continue to load each thread with work untill all threads are done with their measured work? Default is " + Defaults.SHOULD_SYNCH_ITERATIONS, handler = BooleanOptionHandler.class)
    protected Boolean synchIterations = null; // true

    @Option(name = "-gc", usage = "Should do System.gc() between iterations?", handler = BooleanOptionHandler.class)
    protected boolean gcEachIteration = false;

    @Option(name = "-v", aliases = {"--verbose"}, usage = "Verbose mode, default off", handler = BooleanOptionHandler.class)
    protected boolean verbose = false;

    @Option(name = "-odr", aliases = {"--outputdetailedresults"}, usage = "Output detailed results. Default is false", handler = BooleanOptionHandler.class)
    protected boolean outputDetailedResults = false;

    @Option(name = "-foe", usage = "Fail the harness on benchmark erro?", handler = BooleanOptionHandler.class)
    protected boolean failOnError = false;

    @Option(name = "-prof", aliases = {"--useprofiler"}, multiValued = false, usage = "Use profilers for collecting additional info, use --listprofilers to list available profilers", handler = ProfilersOptionHandler.class)
    protected Set<ProfilerFactory.Profilers> profilers = EnumSet.noneOf(ProfilerFactory.Profilers.class);

    @Option(name = "-tu", aliases = {"--timeunit"}, usage = "Output time unit. Available values: m, s, ms, us, ns", handler = TimeUnitOptionHandler.class)
    protected TimeUnit timeUnit = null;

    /**
     * returns canonical command line containing all options (differ than default value)
     *
     * @return
     */
    public String[] toCommandLine() {
        List<String> sb = new ArrayList<String>();
        List<Field> fields = getOptionFields();
        for (Field f : fields) {
            String opImage = fieldToCommandLineImage(f);
            if (opImage != null && !opImage.isEmpty()) {
                Collections.addAll(sb, opImage.split("[ ]+"));
            }
        }
        return sb.toArray(new String[sb.size()]);
    }

    /**
     * returns list of option fields sorted alphabetically by their option names
     *
     * @return
     */
    private List<Field> getOptionFields() {
        Field[] fields = BaseOptions.class.getDeclaredFields();
        List<Field> optionFields = new ArrayList<Field>();
        // find fields (and thus, options) to not send to the forked process
        for (Field field : fields) {
            if (field.getAnnotation(Option.class) != null) {
                optionFields.add(field);
            }
        }
        Collections.sort(optionFields, new Comparator<Field>() {
            @Override
            public int compare(Field f1, Field f2) {
                Option o1 = f1.getAnnotation(Option.class);
                Option o2 = f2.getAnnotation(Option.class);
                return o1.name().compareTo(o2.name());
            }
        });
        return optionFields;
    }

    private String fieldToCommandLineImage(Field f) {
        Class<?> t = f.getType();
        if (Integer.TYPE.equals(t)) {
            return intFieldToString(f);
        }
        if (Boolean.class.equals(t)) {
            return bigBoolFieldToString(f);
        }
        if (Boolean.TYPE.equals(t)) {
            return primitiveBoolFieldToString(f);
        }
        if (List.class.equals(t)) {
            return listFieldToString(f);
        }
        if (Set.class.equals(t)) {
            return setFieldToString(f);
        }
        if (TimeValue.class.equals(t)) {
            return timeValueFieldToString(f);
        }
        if (TimeUnit.class.equals(t)) {
            return timeUnitFieldToString(f);
        }
        if (Mode.class.equals(t)) {
            return benchTypeFieldToString(f);
        }
        throw new IllegalArgumentException("Unknown forwarding field type, field="+f.toString());
    }

    private String intFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof Integer && (Integer) value >= 0) {
                return f.getAnnotation(Option.class).name() + " " + value;
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String bigBoolFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof Boolean) {
                return f.getAnnotation(Option.class).name() + " " + value;
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String primitiveBoolFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof Boolean && (Boolean) value) {
                return f.getAnnotation(Option.class).name() + " " + value;
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String listFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof List && !((List) value).isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(f.getAnnotation(Option.class).name()).append(' ');
                boolean isTail = false;
                for (Object o : (List) value) {
                    if (isTail) {
                        sb.append(',');
                    }
                    sb.append(o);
                    isTail = true;
                }
                return sb.toString();
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String setFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof Set && !((Set) value).isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(f.getAnnotation(Option.class).name()).append(' ');
                boolean isTail = false;
                for (Object o : (Set) value) {
                    if (isTail) {
                        sb.append(',');
                    }
                    if( o instanceof ProfilerFactory.Profilers) {
                        sb.append(((ProfilerFactory.Profilers) o).id());
                    } else {
                        sb.append(o);
                    }
                    isTail = true;
                }
                return sb.toString();
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String timeValueFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof TimeValue) {
                return f.getAnnotation(Option.class).name() + " " + ((TimeValue) value).toCommandLine();
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String timeUnitFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof TimeUnit) {
                return f.getAnnotation(Option.class).name() + " " + TimeUnitOptionHandler.toString((TimeUnit)value);
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    private String benchTypeFieldToString(Field f) {
        try {
            Object value = f.get(this);
            if (value != null && value instanceof Mode) {
                return f.getAnnotation(Option.class).name() + " " + value;
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError("Caused by: " + e.getMessage());
        }
    }

    /**
     * Getter
     *
     * @return the value
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public TimeValue getRuntime() {
        return runTime;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public TimeValue getWarmupTime() {
        return warmupTime;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public int getWarmupIterations() {
        return warmupIterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldDoGC() {
        return gcEachIteration;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public Boolean getSynchIterations() {
        return synchIterations;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Getter
     *
     * @return the value
     */
    public boolean shouldOutputDetailedResults() {
        return outputDetailedResults;
    }

    /**
     * Should fail the harness on test error?
     * @return the value
     */
    public boolean shouldFailOnError() {
        return failOnError;
    }

    /**
     * Getter
     * @return the value
     */
    public Set<ProfilerFactory.Profilers> getProfilers() {
        return profilers;
    }

    public Collection<Mode> getBenchModes() {
        return benchMode;
    }
}
