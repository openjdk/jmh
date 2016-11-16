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
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;

public final class BinaryLinkClient {

    private static final int RESET_EACH = Integer.getInteger("jmh.link.resetEach", 100);
    private static final int BUFFER_SIZE = Integer.getInteger("jmh.link.bufferSize", 64*1024);

    private final Object lock;

    private final Socket clientSocket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private final ForwardingPrintStream streamErr;
    private final ForwardingPrintStream streamOut;
    private final OutputFormat outputFormat;
    private volatile boolean failed;
    private int resetToGo;

    public BinaryLinkClient(String hostName, int hostPort) throws IOException {
        this.lock = new Object();
        this.clientSocket = new Socket(hostName, hostPort);

        // Initialize the OOS first, and flush, letting the other party read the stream header.
        this.oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream(), BUFFER_SIZE));
        this.oos.flush();

        this.ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream(), BUFFER_SIZE));

        this.streamErr = new ForwardingPrintStream(OutputFrame.Type.ERR);
        this.streamOut = new ForwardingPrintStream(OutputFrame.Type.OUT);
        this.outputFormat = (OutputFormat) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{OutputFormat.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        pushFrame(new OutputFormatFrame(ClassConventions.getMethodName(method), args));
                        return null; // expect null
                    }
                }
        );
    }

    private void pushFrame(Serializable frame) throws IOException {
        if (failed) {
            throw new IOException("Link had failed already");
        }

        // It is important to reset the OOS to avoid garbage buildup in internal identity
        // tables. However, we cannot do that after each frame since the huge referenced
        // objects like benchmark and iteration parameters will be duplicated on the receiver
        // side. This is why we reset only each RESET_EACH frames.
        //
        // It is as much as important to flush the stream to let the other party know we
        // pushed something out.

        synchronized (lock) {
            try {
                if (resetToGo-- < 0) {
                    oos.reset();
                    resetToGo = RESET_EACH;
                }

                oos.writeObject(frame);
                oos.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }
    }

    private Object readFrame() throws IOException, ClassNotFoundException {
        try {
            return ois.readObject();
        } catch (ClassNotFoundException | IOException ex) {
            failed = true;
            throw ex;
        }
    }

    public void close() throws IOException {
        // BinaryLinkClient (BLC) should not acquire the BLC lock while dealing with
        // ForwardingPrintStream (FPS): if there is a pending operation in FPS,
        // and it writes something out, it will acquire the BLC lock after acquiring
        // FPS lock => deadlock. Let FPS figure this one out on its own.
        FileUtils.safelyClose(streamErr);
        FileUtils.safelyClose(streamOut);

        synchronized (lock) {
            oos.writeObject(new FinishingFrame());
            FileUtils.safelyClose(ois);
            FileUtils.safelyClose(oos);
            clientSocket.close();
        }
    }

    public Options handshake() throws IOException, ClassNotFoundException {
        synchronized (lock) {
            pushFrame(new HandshakeInitFrame(Utils.getPid()));

            Object reply = readFrame();
            if (reply instanceof HandshakeResponseFrame) {
                return (((HandshakeResponseFrame) reply).getOpts());
            } else {
                throw new IllegalStateException("Got the erroneous reply: " + reply);
            }
        }
    }

    public ActionPlan requestPlan() throws IOException, ClassNotFoundException {
        synchronized (lock) {
            pushFrame(new InfraFrame(InfraFrame.Type.ACTION_PLAN_REQUEST));

            Object reply = readFrame();
            if (reply instanceof ActionPlanFrame) {
                return ((ActionPlanFrame) reply).getActionPlan();
            } else {
                throw new IllegalStateException("Got the erroneous reply: " + reply);
            }
        }
    }

    public void pushResults(IterationResult res) throws IOException {
        pushFrame(new ResultsFrame(res));
    }

    public void pushException(BenchmarkException error) throws IOException {
        pushFrame(new ExceptionFrame(error));
    }

    public void pushResultMetadata(BenchmarkResultMetaData res) throws IOException {
        pushFrame(new ResultMetadataFrame(res));
    }

    public PrintStream getOutStream() {
        return streamOut;
    }

    public PrintStream getErrStream() {
        return streamErr;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    class ForwardingPrintStream extends PrintStream {
        public ForwardingPrintStream(final OutputFrame.Type type) {
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    pushFrame(new OutputFrame(type, new byte[]{(byte) (b & 0xFF)}));
                }

                @Override
                public void write(byte[] b) throws IOException {
                    pushFrame(new OutputFrame(type, Arrays.copyOf(b, b.length)));
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    pushFrame(new OutputFrame(type, Arrays.copyOfRange(b, off, len + off)));
                }
            });
        }
    }

}
