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

import org.openjdk.jmh.runner.link.BinaryLinkClient;
import org.openjdk.jmh.runner.options.Options;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main program entry point for forked JVM instance
 */
class ForkedMain {

    private static final AtomicBoolean hangupFuse = new AtomicBoolean();
    private static final AtomicReference<BinaryLinkClient> linkRef = new AtomicReference<>();

    private static volatile boolean gracefullyFinished;
    private static volatile Throwable exception;
    private static volatile PrintStream nakedErr;

    /**
     * Application main entry point
     *
     * @param argv Command line arguments
     */
    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            throw new IllegalArgumentException("Expected two arguments for forked VM");
        } else {
            // arm the hangup thread
            Runtime.getRuntime().addShutdownHook(new HangupThread());

            // init the shutdown thread
            ShutdownTimeoutThread shutdownThread = new ShutdownTimeoutThread();

            try {
                // This assumes the exact order of arguments:
                //   1) host name to back-connect
                //   2) host port to back-connect
                String hostName = argv[0];
                int hostPort = Integer.valueOf(argv[1]);

                // establish the link to host VM and pull the options
                BinaryLinkClient link = new BinaryLinkClient(hostName, hostPort);
                linkRef.set(link);

                Options options = link.handshake();

                // dump outputs into binary link
                nakedErr = System.err;
                System.setErr(link.getErrStream());
                System.setOut(link.getOutStream());

                // run!
                ForkedRunner runner = new ForkedRunner(options, link);
                runner.run();

                gracefullyFinished = true;
            } catch (Throwable ex) {
                exception = ex;
                gracefullyFinished = false;
            } finally {
                // arm the shutdown timer
                shutdownThread.start();
            }

            if (!gracefullyFinished) {
                System.exit(1);
            }
        }
    }

    /**
     * Report our latest status to the host VM, and say goodbye.
     */
    static void hangup() {
        // hangup fires only once
        if (!hangupFuse.compareAndSet(false, true)) return;

        if (!gracefullyFinished) {
            Throwable ex = exception;
            if (ex == null) {
                ex = new IllegalStateException(
                        "<failure: VM prematurely exited before JMH had finished with it, " +
                                "explicit System.exit was called?>");
            }

            String msg = ex.getMessage();

            BinaryLinkClient link = linkRef.get();
            if (link != null) {
                try {
                    link.getOutputFormat().println(msg);
                    link.pushException(new BenchmarkException(ex));
                } catch (Exception e) {
                    // last resort
                    ex.printStackTrace(nakedErr);
                }
            } else {
                // last resort
                ex.printStackTrace(nakedErr);
            }
        }

        BinaryLinkClient link = linkRef.getAndSet(null);
        if (link != null) {
            try {
                link.close();
            } catch (IOException e) {
                // swallow
            }
        }
    }

    /**
     * Hangup thread will detach us from the host VM properly, in three cases:
     *   - normal shutdown
     *   - shutdown with benchmark exception
     *   - any System.exit call
     *
     * The need to intercept System.exit calls is the reason to register ourselves
     * as the shutdown hook. Additionally, this thread runs only when all non-daemon
     * threads are stopped, and therefore the stray user threads would be reported
     * by shutdown timeout thread over still alive binary link.
     */
    private static class HangupThread extends Thread {
        @Override
        public void run() {
            hangup();
        }
    }

    /**
     * Shutdown timeout thread will forcefully exit the VM in two cases:
     *   - stray non-daemon thread prevents the VM from exiting
     *   - all user threads have finished, but we are stuck in some shutdown hook or finalizer
     *
     * In all other "normal" cases, VM will exit before the timeout expires.
     */
    private static class ShutdownTimeoutThread extends Thread {
        private static final int TIMEOUT = Integer.getInteger("jmh.shutdownTimeout", 30);
        private static final int TIMEOUT_STEP = Integer.getInteger("jmh.shutdownTimeout.step", 5);
        private static final String LINE_SEPARATOR = System.getProperty("line.separator");

        public ShutdownTimeoutThread() {
            setName("JMH-Shutdown-Timeout");
            setDaemon(true);
        }

        @Override
        public void run() {
            long start = System.nanoTime();

            long waitMore;
            do {
                try {
                    TimeUnit.SECONDS.sleep(TIMEOUT_STEP);
                } catch (InterruptedException e) {
                    return;
                }

                waitMore = TimeUnit.SECONDS.toNanos(TIMEOUT) - (System.nanoTime() - start);

                String msg = getMessage(waitMore);

                BinaryLinkClient link = linkRef.get();
                if (link != null) {
                    link.getOutputFormat().println(msg);
                } else {
                    // last resort
                    nakedErr.println(msg);
                }
            } while (waitMore > 0);

            String msg = "<shutdown timeout of " + TIMEOUT + " seconds expired, forcing forked VM to exit>";
            BinaryLinkClient link = linkRef.get();
            if (link != null) {
                link.getOutputFormat().println(msg);
            } else {
                // last resort
                nakedErr.println(msg);
            }

            // aggressively try to hangup, and HALT
            hangup();
            Runtime.getRuntime().halt(0);
        }

        private String getMessage(long waitMore) {
            StringBuilder sb = new StringBuilder();
            sb.append("<JMH had finished, but forked VM did not exit, are there stray running threads? Waiting ")
                    .append(TimeUnit.NANOSECONDS.toSeconds(waitMore)).append(" seconds more...>");
            sb.append(LINE_SEPARATOR);
            sb.append(LINE_SEPARATOR);

            sb.append("Non-finished threads:");
            sb.append(LINE_SEPARATOR);
            sb.append(LINE_SEPARATOR);

            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                Thread thread = e.getKey();
                StackTraceElement[] els = e.getValue();

                if (thread.isDaemon()) continue;
                if (!thread.isAlive()) continue;

                sb.append(thread);
                sb.append(LINE_SEPARATOR);

                for (StackTraceElement el : els) {
                    sb.append("  at ");
                    sb.append(el);
                    sb.append(LINE_SEPARATOR);
                }

                sb.append(LINE_SEPARATOR);
            }

            return sb.toString();
        }
    }

}
