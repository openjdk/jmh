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
package org.openjdk.jmh.runner.link;

import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.ActionPlan;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accepts the binary data from the forked VM and pushes it to parent VM
 * as appropriate. This server assumes there is only the one and only
 * client at any given point of time.
 */
public final class BinaryLinkServer {

    private static final int BUFFER_SIZE = Integer.getInteger("jmh.link.bufferSize", 64*1024);

    private final Options opts;
    private final OutputFormat out;
    private final Map<String, Method> methods;
    private final Set<String> forbidden;
    private final Acceptor acceptor;
    private final AtomicReference<Handler> handler;
    private final AtomicReference<List<IterationResult>> results;
    private final AtomicReference<BenchmarkResultMetaData> metadata;
    private final AtomicReference<BenchmarkException> exception;
    private final AtomicReference<ActionPlan> plan;
    private volatile long clientPid;

    public BinaryLinkServer(Options opts, OutputFormat out) throws IOException {
        this.opts = opts;
        this.out = out;
        this.methods = new HashMap<>();
        this.forbidden = new HashSet<>();

        // enumerate methods
        for (Method m : OutputFormat.class.getMethods()) {

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

        handler = new AtomicReference<>();
        metadata = new AtomicReference<>();
        results = new AtomicReference<List<IterationResult>>(new ArrayList<IterationResult>());
        exception = new AtomicReference<>();
        plan = new AtomicReference<>();
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

    public List<IterationResult> getResults() {
        List<IterationResult> res = results.getAndSet(new ArrayList<IterationResult>());
        if (res != null) {
            return res;
        } else {
            throw new IllegalStateException("Acquiring the null result");
        }
    }

    public BenchmarkResultMetaData getMetadata() {
        return metadata.getAndSet(null);
    }

    public void setPlan(ActionPlan actionPlan) {
        this.plan.set(actionPlan);
    }

    private InetAddress getListenAddress() {
        // Try to use user-provided override first.
        String addr = System.getProperty("jmh.link.address");
        if (addr != null) {
            try {
                return InetAddress.getByName(addr);
            } catch (UnknownHostException e) {
                // override failed, notify user
                throw new IllegalStateException("Can not initialize binary link.", e);
            }
        }

        // Auto-detection should try to use JDK 7+ method first, it is more reliable.
        try {
            Method m = InetAddress.class.getMethod("getLoopbackAddress");
            return (InetAddress) m.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
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

    private int getListenPort() {
        return Integer.getInteger("jmh.link.port", 0);
    }

    public long getClientPid() {
        return clientPid;
    }

    private final class Acceptor extends Thread {

        private final ServerSocket server;
        private final InetAddress listenAddress;

        public Acceptor() throws IOException {
            listenAddress = getListenAddress();
            server = new ServerSocket(getListenPort(), 50, listenAddress);
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
            return listenAddress.getHostAddress();
        }

        public int getPort() {
            // Poll the actual listen port, in case it is ephemeral
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

            // eager OOS initialization, let the other party read the stream header
            oos = new ObjectOutputStream(new BufferedOutputStream(os, BUFFER_SIZE));
            oos.flush();
        }

        @Override
        public void run() {
            try {
                // late OIS initialization, otherwise we'll block reading the header
                ois = new ObjectInputStream(new BufferedInputStream(is, BUFFER_SIZE));

                Object obj;
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof OutputFormatFrame) {
                        handleOutputFormat((OutputFormatFrame) obj);
                    }
                    if (obj instanceof InfraFrame) {
                        handleInfra((InfraFrame) obj);
                    }
                    if (obj instanceof HandshakeInitFrame) {
                        handleHandshake((HandshakeInitFrame) obj);
                    }
                    if (obj instanceof ResultsFrame) {
                        handleResults((ResultsFrame) obj);
                    }
                    if (obj instanceof ExceptionFrame) {
                        handleException((ExceptionFrame) obj);
                    }
                    if (obj instanceof OutputFrame) {
                        handleOutput((OutputFrame) obj);
                    }
                    if (obj instanceof ResultMetadataFrame) {
                        handleResultMetadata((ResultMetadataFrame) obj);
                    }
                    if (obj instanceof FinishingFrame) {
                        // close the streams
                        break;
                    }
                }
            } catch (EOFException e) {
                // ignore
            } catch (Exception e) {
                out.println("<binary link had failed, forked VM corrupted the stream? Use " + VerboseMode.EXTRA + " verbose to print exception>");
                if (opts.verbosity().orElse(Defaults.VERBOSITY).equalsOrHigherThan(VerboseMode.EXTRA)) {
                    out.println(Utils.throwableToString(e));
                }
            } finally {
                close();
            }
        }

        private void handleResultMetadata(ResultMetadataFrame obj) {
            metadata.set(obj.getMD());
        }

        private void handleOutput(OutputFrame obj) {
            try {
                switch (obj.getType()) {
                    case OUT:
                        System.out.write(obj.getData());
                        break;
                    case ERR:
                        System.err.write(obj.getData());
                        break;
                }
            } catch (IOException e) {
                // swallow
            }
        }

        private void handleException(ExceptionFrame obj) {
            exception.set(obj.getError());
        }

        private void handleResults(ResultsFrame obj) {
            results.get().add(obj.getRes());
        }

        private void handleHandshake(HandshakeInitFrame obj) throws IOException {
            clientPid = obj.getPid();
            oos.writeObject(new HandshakeResponseFrame(opts));
            oos.flush();
        }

        private void handleInfra(InfraFrame req) throws IOException {
            switch (req.getType()) {
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
