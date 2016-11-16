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
package org.openjdk.jmh.util;

import sun.misc.Unsafe;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Utils {

    private static final Unsafe U;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private Utils() {

    }

    private static final ConcurrentMap<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    public static Pattern lazyCompile(String pattern) {
        Pattern patt = PATTERNS.get(pattern);
        if (patt == null) {
            patt = Pattern.compile(pattern);
            PATTERNS.put(pattern, patt);
        }
        return patt;
    }

    public static <T extends Comparable<T>> T min(Collection<T> ts) {
        T min = null;
        for (T t : ts) {
            if (min == null) {
                min = t;
            } else {
                min = min.compareTo(t) < 0 ? min : t;
            }
        }
        return min;
    }

    public static <T extends Comparable<T>> T max(Collection<T> ts) {
        T max = null;
        for (T t : ts) {
            if (max == null) {
                max = t;
            } else {
                max = max.compareTo(t) > 0 ? max : t;
            }
        }
        return max;
    }

    public static String[] concat(String[] t1, String[] t2) {
        String[] r = new String[t1.length + t2.length];
        System.arraycopy(t1, 0, r, 0, t1.length);
        System.arraycopy(t2, 0, r, t1.length, t2.length);
        return r;
    }

    public static String join(Collection<String> src, String delim) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : src) {
            if (first) {
                first = false;
            } else {
                sb.append(delim);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String join(String[] src, String delim) {
        return join(Arrays.asList(src), delim);
    }

    public static Collection<String> splitQuotedEscape(String src) {
        List<String> results = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (char ch : src.toCharArray()) {
            if (ch == ' ' && !escaped) {
                String s = sb.toString();
                if (!s.isEmpty()) {
                    results.add(s);
                    sb = new StringBuilder();
                }
            } else if (ch == '\"') {
                escaped ^= true;
            } else {
                sb.append(ch);
            }
        }

        String s = sb.toString();
        if (!s.isEmpty()) {
            results.add(s);
        }

        return results;
    }

    public static int sum(int[] arr) {
        int sum = 0;
        for (int i : arr) {
            sum += i;
        }
        return sum;
    }

    public static int roundUp(int v, int quant) {
        if ((v % quant) == 0) {
            return v;
        } else {
            return ((v / quant) + 1)*quant;
        }
    }

    public static String throwableToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    public static int[] unmarshalIntArray(String src) {
        String[] ss = src.split("=");
        int[] arr = new int[ss.length];
        int cnt = 0;
        for (String s : ss) {
            arr[cnt] = Integer.valueOf(s.trim());
            cnt++;
        }
        return arr;
    }

    public static String marshalIntArray(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i : arr) {
            sb.append(i);
            sb.append("=");
        }
        return sb.toString();
    }

    /**
     * Warm up the CPU schedulers, bring all the CPUs online to get the
     * reasonable estimate of the system capacity. Some systems, notably embedded Linuxes,
     * power down the idle CPUs and so availableProcessors() may report lower CPU count
     * than would be present after the load-up.
     *
     * @return max CPU count
     */
    public static int figureOutHotCPUs() {
        ExecutorService service = Executors.newCachedThreadPool();

        int warmupTime = 1000;
        long lastChange = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        futures.add(service.submit(new BurningTask()));

        int max = 0;
        while (System.currentTimeMillis() - lastChange < warmupTime) {
            int cur = Runtime.getRuntime().availableProcessors();
            if (cur > max) {
                max = cur;
                lastChange = System.currentTimeMillis();
                futures.add(service.submit(new BurningTask()));
            }
        }

        for (Future<?> f : futures) {
            f.cancel(true);
        }

        service.shutdown();

        return max;
    }

    public static Charset guessConsoleEncoding() {
        // The reason for this method to exist is simple: we need the proper platform encoding for output.
        // We cannot use Console class directly, because we also need the access to the raw byte stream,
        // e.g. for pushing in a raw output from a forked VM invocation. Therefore, we are left with
        // reflectively poking out the Charset from Console, and use it for our own private output streams.

        // Try 1. Try to poke the System.console().
        try {
            Console console = System.console();
            if (console != null) {
                Field f = Console.class.getDeclaredField("cs");
                f.setAccessible(true);
                Object res = f.get(console);
                if (res instanceof Charset) {
                    return (Charset) res;
                }
                Method m = Console.class.getDeclaredMethod("encoding");
                m.setAccessible(true);
                res = m.invoke(null);
                if (res instanceof String) {
                    return Charset.forName((String) res);
                }
            }
        } catch (Exception e) {
            // fall-through
        }

        // Try 2. Try to poke stdout.
        // When System.console() is null, that is, an application is not attached to a console, the actual
        // charset of standard output should be extracted from System.out, not from System.console().
        // If we indeed have the console, but failed to poll its charset, it is still better to poke stdout.
        try {
            PrintStream out = System.out;
            if (out != null) {
                Field f = PrintStream.class.getDeclaredField("charOut");
                f.setAccessible(true);
                Object res = f.get(out);
                if (res instanceof OutputStreamWriter) {
                    String encoding = ((OutputStreamWriter) res).getEncoding();
                    if (encoding != null) {
                        return Charset.forName(encoding);
                    }
                }
            }
        } catch (Exception e) {
            // fall-through
        }

        // Try 3. Try to poll internal properties.
        String prop = System.getProperty("sun.stdout.encoding");
        if (prop != null) {
            try {
                return Charset.forName(prop);
            } catch (Exception e) {
                // fall-through
            }
        }

        // Try 4. Nothing left to do, except for returning a (possibly mismatched) default charset.
        return Charset.defaultCharset();
    }

    public static void reflow(PrintWriter pw, String src, int width, int indent) {
        StringTokenizer tokenizer = new StringTokenizer(src);
        int curWidth = indent;
        indent(pw, indent);
        while (tokenizer.hasMoreTokens()) {
            String next = tokenizer.nextToken();
            pw.print(next);
            pw.print(" ");
            curWidth += next.length() + 1;
            if (curWidth > width) {
                pw.println();
                indent(pw, indent);
                curWidth = 0;
            }
        }
        pw.println();
    }

    private static void indent(PrintWriter pw, int indent) {
        for (int i = 0; i < indent; i++) {
            pw.print(" ");
        }
    }

    public static Collection<String> rewrap(String lines) {
        Collection<String> result = new ArrayList<>();
        String[] words = lines.split("[ \n]");
        String line = "";
        int cols = 0;
        for (String w : words) {
            cols += w.length();
            line += w + " ";
            if (cols > 40) {
                result.add(line);
                line = "";
                cols = 0;
            }
        }
        if (!line.trim().isEmpty()) {
            result.add(line);
        }
        return result;
    }

    static class BurningTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()); // burn;
        }
    }

    public static void check(Class<?> klass, String... fieldNames) {
        for (String fieldName : fieldNames) {
            check(klass, fieldName);
        }
    }

    public static void check(Class<?> klass, String fieldName) {
        final long requiredGap = 128;
        long markerBegin = getOffset(klass, "markerBegin");
        long markerEnd = getOffset(klass, "markerEnd");
        long off = getOffset(klass, fieldName);
        if (markerEnd - off < requiredGap || off - markerBegin < requiredGap) {
            throw new IllegalStateException("Consistency check failed for " + fieldName + ", off = " + off + ", markerBegin = " + markerBegin + ", markerEnd = " + markerEnd);
        }
    }

    public static long getOffset(Class<?> klass, String fieldName) {
        do {
            try {
                Field f = klass.getDeclaredField(fieldName);
                return U.objectFieldOffset(f);
            } catch (NoSuchFieldException e) {
                // whatever, will try superclass
            }
            klass = klass.getSuperclass();
        } while (klass != null);
        throw new IllegalStateException("Can't find field \"" + fieldName + "\"");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").contains("indows");
    }

    public static String getCurrentJvm() {
        return System.getProperty("java.home") +
                File.separator +
                "bin" +
                File.separator +
                "java" +
                (isWindows() ? ".exe" : "");
    }

    public static String getCurrentJvmVersion() {
        return "JDK "
                + System.getProperty("java.version")
                + ", VM "
                + System.getProperty("java.vm.version");
    }

    public static String getCurrentOSVersion() {
        return System.getProperty("os.name")
                + ", "
                + System.getProperty("os.arch")
                + ", "
                + System.getProperty("os.version");
    }

    /**
     * Gets PID of the current JVM.
     *
     * @return PID.
     */
    public static long getPid() {
        final String DELIM = "@";

        String name = ManagementFactory.getRuntimeMXBean().getName();

        if (name != null) {
            int idx = name.indexOf(DELIM);

            if (idx != -1) {
                String str = name.substring(0, name.indexOf(DELIM));
                try {
                    return Long.valueOf(str);
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException("Process PID is not a number: " + str);
                }
            }
        }
        throw new IllegalStateException("Unsupported PID format: " + name);
    }

    public static Collection<String> tryWith(String... cmd) {
        Collection<String> messages = new ArrayList<>();
        try {
            Process p = new ProcessBuilder(cmd).start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), baos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), baos);

            errDrainer.start();
            outDrainer.start();

            int err = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            if (err != 0) {
                messages.add(baos.toString());
            }
        } catch (IOException ex) {
            return Collections.singleton(ex.getMessage());
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return messages;
    }

    public static Collection<String> runWith(List<String> cmd) {
        Collection<String> messages = new ArrayList<>();
        try {
            Process p = new ProcessBuilder(cmd).start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // drain streams, else we might lock up
            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), baos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), baos);

            errDrainer.start();
            outDrainer.start();

            int err = p.waitFor();

            errDrainer.join();
            outDrainer.join();

            messages.add(baos.toString());
        } catch (IOException ex) {
            return Collections.singleton(ex.getMessage());
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return messages;
    }

    /**
     * Adapts Iterator for Iterable.
     * Can be iterated only once!
     *
     * @param it iterator
     * @return iterable for given iterator
     */
    public static <T> Iterable<T> adaptForLoop(final Iterator<T> it) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return it;
            }
        };
    }

}
