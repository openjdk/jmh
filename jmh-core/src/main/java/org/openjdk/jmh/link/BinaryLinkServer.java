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

import org.openjdk.jmh.output.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Accepts the OutputFormat calls from the network and forwards those to given local OutputFormat
 */
public class BinaryLinkServer {

    private final Options opts;
    private final OutputFormat out;
    private final Map<String, Method> methods;
    private final Set<String> forbidden;
    private final Set<String> finishing;
    private final Acceptor acceptor;
    private final List<Handler> registeredHandlers;

    public BinaryLinkServer(Options opts, OutputFormat out) throws IOException {
        this.opts = opts;
        this.out = out;
        this.methods = new HashMap<String, Method>();
        this.forbidden = new HashSet<String>();
        this.finishing = new HashSet<String>();

        // enumerate methods
        for (Method m : out.getClass().getMethods()) {

            // start/end run callbacks are banned, since their effects are enforced by parent instead
            if (m.getName().equals("startRun")) { forbidden.add(ClassConventions.getMethodName(m)); }
            if (m.getName().equals("endRun"))   { forbidden.add(ClassConventions.getMethodName(m)); }

            // receiving close has a special meaning
            if (m.getName().equals("close"))    { finishing.add(ClassConventions.getMethodName(m)); }

            Method prev = methods.put(ClassConventions.getMethodName(m), m);
            if (prev != null) {
                out.println("WARNING: Duplicate methods: " + m + " vs. " + prev);
                throw new IllegalStateException("WARNING: Duplicate methods: " + m + " vs. " + prev);
            }
        }

        registeredHandlers = Collections.synchronizedList(new ArrayList<Handler>());

        acceptor = new Acceptor();
        acceptor.start();
    }

    public void terminate() {
        acceptor.close();

        for (Handler r : registeredHandlers) {
            r.close();
        }

        try {
            acceptor.join();
            for (Handler r : registeredHandlers) {
                r.join();
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void waitFinish() {
        for (Iterator<Handler> iterator = registeredHandlers.iterator(); iterator.hasNext(); ) {
            Handler r = iterator.next();
            try {
                r.join();
                iterator.remove();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private final class Acceptor extends Thread {

        private final ServerSocket server;

        public Acceptor() throws IOException {
            server = new ServerSocket(0);
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Socket clientSocket = server.accept();
                    Handler r = new Handler(clientSocket);
                    registeredHandlers.add(r);
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
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to resolve local host", e);
            }
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
                    if (obj instanceof CallInfo) {
                        CallInfo frame = (CallInfo) obj;

                        Method m = methods.get(frame.method);

                        if (finishing.contains(frame.method)) {
                            break;
                        }

                        if (forbidden.contains(frame.method)) {
                            continue;
                        }

                        if (m == null) {
                            out.println("WARNING: Unknown method to forward: " + frame.method);
                            continue;
                        }

                        m.invoke(out, frame.args);
                    }

                    if (obj instanceof InfraRequest) {
                        switch (((InfraRequest) obj).getType()) {
                            case OPTIONS_REQUEST:
                                oos.writeObject(new OptionsReply(opts));
                                oos.flush();
                                break;
                            default:
                                throw new IllegalStateException("Unknown infrastructure request: " + obj);
                        }
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

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

}
