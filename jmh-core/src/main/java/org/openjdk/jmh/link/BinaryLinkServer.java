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
package org.openjdk.jmh.link;

import org.openjdk.jmh.link.frames.ActionPlanFrame;
import org.openjdk.jmh.link.frames.ExceptionFrame;
import org.openjdk.jmh.link.frames.FinishingFrame;
import org.openjdk.jmh.link.frames.InfraFrame;
import org.openjdk.jmh.link.frames.OptionsFrame;
import org.openjdk.jmh.link.frames.OutputFormatFrame;
import org.openjdk.jmh.link.frames.ResultsFrame;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.ActionPlan;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.internal.HashMultimap;
import org.openjdk.jmh.util.internal.Multimap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accepts the binary data from the forked VM and pushes it to parent VM
 * as appropriate. This server assumes there is only the one and only
 * client at any given point of time.
 */
public final class BinaryLinkServer {

    private final Options opts;
    private final OutputFormat out;
    private final Map<String, Method> methods;
    private final Set<String> forbidden;
    private final Acceptor acceptor;
    private final AtomicReference<Handler> handler;
    private final AtomicReference<Multimap<BenchmarkRecord, BenchResult>> results;
    private final AtomicReference<BenchmarkException> exception;
    private final AtomicReference<ActionPlan> plan;

    public BinaryLinkServer(Options opts, OutputFormat out) throws IOException {
        this.opts = opts;
        this.out = out;
        this.methods = new HashMap<String, Method>();
        this.forbidden = new HashSet<String>();

        // enumerate methods
        for (Method m : out.getClass().getMethods()) {

            // start/end run callbacks are banned, since their effects are enforced by parent instead
            if (m.getName().equals("startRun")) { forbidden.add(ClassConventions.getMethodName(m)); }
            if (m.getName().equals("endRun"))   { forbidden.add(ClassConventions.getMethodName(m)); }

            Method prev = methods.put(ClassConventions.getMethodName(m), m);
            if (prev != null) {
                out.println("WARNING: Duplicate methods: " + m + " vs. " + prev);
                throw new IllegalStateException("WARNING: Duplicate methods: " + m + " vs. " + prev);
            }
        }

        acceptor = new Acceptor();
        acceptor.start();

        handler = new AtomicReference<Handler>();
        results = new AtomicReference<Multimap<BenchmarkRecord, BenchResult>>(new HashMultimap<BenchmarkRecord, BenchResult>());
        exception = new AtomicReference<BenchmarkException>();
        plan = new AtomicReference<ActionPlan>();
    }

    public void terminate() {
        acceptor.close();

        Handler h = handler.getAndSet(null);
        if (h != null) {
            h.close();
        }

        try {
            acceptor.join();
            if (h != null) {
                h.join();
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void waitFinish() {
        Handler h = handler.getAndSet(null);
        if (h != null) {
            try {
                h.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public BenchmarkException getException() {
        return exception.getAndSet(null);
    }

    public Multimap<BenchmarkRecord, BenchResult> getResults() {
        Multimap<BenchmarkRecord, BenchResult> res = results.getAndSet(new HashMultimap<BenchmarkRecord, BenchResult>());
        if (res != null) {
            return res;
        } else {
            throw new IllegalStateException("Acquiring the null result");
        }
    }

    public void setPlan(ActionPlan actionPlan) {
        this.plan.set(actionPlan);
    }

    private InetAddress getLoopback() {
        // Try to use JDK 7+ method first, it is more reliable.
        try {
            Method m = InetAddress.class.getMethod("getLoopbackAddress");
            return (InetAddress) m.invoke(null);
        } catch (InvocationTargetException e) {
            // shun
        } catch (NoSuchMethodException e) {
            // shun
        } catch (IllegalAccessException e) {
            // shun
        }

        // Otherwise open up the special loopback.
        //   (It can only fail for the obscure reason)
        try {
            return InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        } catch (UnknownHostException e) {
            // shun
        }

        // Last resort. Open the local host: this resolves
        // the machine name, and not reliable on mis-configured
        // hosts, but there is nothing else we can try.
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Can not find the address to bind to.", e);
        }
    }

    private final class Acceptor extends Thread {

        private final ServerSocket server;

        public Acceptor() throws IOException {
            server = new ServerSocket(0, 50, getLoopback());
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Socket clientSocket = server.accept();
                    Handler r = new Handler(clientSocket);
                    if (!handler.compareAndSet(null, r)) {
                        throw new IllegalStateException("The handler is already registered");
                    }
                    r.start();
                }
            } catch (SocketException e) {
                // assume this is "Socket closed", return
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                close();
            }
        }

        public String getHost() {
            return getLoopback().getHostAddress();
        }

        public int getPort() {
            return server.getLocalPort();
        }

        public void close() {
            try {
                server.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public String getHost() {
        return acceptor.getHost();
    }

    public int getPort() {
        return acceptor.getPort();
    }

    private final class Handler extends Thread {
        private final InputStream is;
        private final Socket socket;
        private ObjectInputStream ois;
        private final OutputStream os;
        private ObjectOutputStream oos;

        public Handler(Socket socket) throws IOException {
            this.socket = socket;
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                // late OIS initialization, otherwise we'll block reading the header
                ois = new ObjectInputStream(is);
                oos = new ObjectOutputStream(os);

                Object obj;
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof OutputFormatFrame) {
                        handleOutputFormat((OutputFormatFrame) obj);
                    }
                    if (obj instanceof InfraFrame) {
                        handleInfra((InfraFrame) obj);
                    }
                    if (obj instanceof ResultsFrame) {
                        handleResults((ResultsFrame)obj);
                    }
                    if (obj instanceof ExceptionFrame) {
                        handleException((ExceptionFrame)obj);
                    }
                    if (obj instanceof FinishingFrame) {
                        // close the streams
                        break;
                    }
                }
            } catch (ObjectStreamException e) {
                throw new IllegalStateException(e);
            } catch (InterruptedIOException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } finally {
                close();
            }
        }

        private void handleException(ExceptionFrame obj) {
            exception.set(obj.getError());
        }

        private void handleResults(ResultsFrame obj) {
            results.get().merge(obj.getRes());
        }

        private void handleInfra(InfraFrame req) throws IOException {
            switch (req.getType()) {
                case OPTIONS_REQUEST:
                    oos.writeObject(new OptionsFrame(opts));
                    oos.flush();
                    break;
                case ACTION_PLAN_REQUEST:
                    oos.writeObject(new ActionPlanFrame(plan.get()));
                    oos.flush();
                    break;
                default:
                    throw new IllegalStateException("Unknown infrastructure request: " + req);
            }
        }

        private boolean handleOutputFormat(OutputFormatFrame frame) throws IllegalAccessException, InvocationTargetException {
            Method m = methods.get(frame.method);

            if (m == null) {
                out.println("WARNING: Unknown method to forward: " + frame.method);
                return true;
            }

            if (forbidden.contains(frame.method)) {
                return true;
            }

            m.invoke(out, frame.args);
            return false;
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

}
