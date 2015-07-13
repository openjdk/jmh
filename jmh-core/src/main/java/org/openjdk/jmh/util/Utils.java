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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Utils {

    private static final Unsafe U;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private Utils() {

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
        List<String> results = new ArrayList<String>();

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

        List<Future<?>> futures = new ArrayList<Future<?>>();
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
        // We cannot use Console class directly, because we also need the access to the raw byte stream,
        // e.g. for pushing in a raw output from a forked VM invocation. Therefore, we are left with
        // reflectively poking out the Charset from Console, and use it for our own private output streams.

        try {
            Field f = Console.class.getDeclaredField("cs");
            f.setAccessible(true);
            Console console = System.console();
            if (console != null) {
                Object res = f.get(console);
                if (res instanceof Charset) {
                    return (Charset) res;
                }
            }
        } catch (NoSuchFieldException e) {
            // fall-through
        } catch (IllegalAccessException e) {
            // fall-through
        }

        try {
            Method m = Console.class.getDeclaredMethod("encoding");
            m.setAccessible(true);
            Object res = m.invoke(null);
            if (res instanceof String) {
                return Charset.forName((String) res);
            }
        } catch (NoSuchMethodException e) {
            // fall-through
        } catch (InvocationTargetException e) {
            // fall-through
        } catch (IllegalAccessException e) {
            // fall-through
        } catch (IllegalCharsetNameException e) {
            // fall-through
        } catch (UnsupportedCharsetException e) {
            // fall-through
        }

        return Charset.defaultCharset();
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
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print("JDK ");
        pw.print(System.getProperty("java.version"));
        pw.print(", VM ");
        pw.print(System.getProperty("java.vm.version"));
        return sw.toString();
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
        Collection<String> messages = new ArrayList<String>();
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
        Collection<String> messages = new ArrayList<String>();
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

}
