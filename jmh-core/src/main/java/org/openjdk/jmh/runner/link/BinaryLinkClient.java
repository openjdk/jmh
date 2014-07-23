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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.ActionPlan;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Multimap;

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

    private final Object lock;

    private final Socket clientSocket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private final ForwardingPrintStream streamErr;
    private final ForwardingPrintStream streamOut;

    public BinaryLinkClient(String hostName, int hostPort) throws IOException {
        this.lock = new Object();
        this.clientSocket = new Socket(hostName, hostPort);
        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
        this.ois = new ObjectInputStream(clientSocket.getInputStream());
        this.streamErr = new ForwardingPrintStream(OutputFrame.Type.ERR);
        this.streamOut = new ForwardingPrintStream(OutputFrame.Type.OUT);
    }

    private void pushFrame(Serializable frame) throws IOException {
        synchronized (lock) {
            oos.writeObject(frame);
            oos.flush();
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            FileUtils.safelyClose(streamErr);
            FileUtils.safelyClose(streamOut);
            oos.writeObject(new FinishingFrame());
            FileUtils.safelyClose(ois);
            FileUtils.safelyClose(oos);
            clientSocket.close();
        }
    }

    public Options requestOptions() throws IOException, ClassNotFoundException {
        synchronized (lock) {
            pushFrame(new InfraFrame(InfraFrame.Type.OPTIONS_REQUEST));

            Object reply = ois.readObject();
            if (reply instanceof OptionsFrame) {
                return (((OptionsFrame) reply).getOpts());
            } else {
                throw new IllegalStateException("Got the erroneous reply: " + reply);
            }
        }
    }

    public ActionPlan requestPlan() throws IOException, ClassNotFoundException {
        synchronized (lock) {
            pushFrame(new InfraFrame(InfraFrame.Type.ACTION_PLAN_REQUEST));

            Object reply = ois.readObject();
            if (reply instanceof ActionPlanFrame) {
                return ((ActionPlanFrame) reply).getActionPlan();
            } else {
                throw new IllegalStateException("Got the erroneous reply: " + reply);
            }
        }
    }

    public void pushResults(Multimap<BenchmarkParams, BenchmarkResult> res) throws IOException {
        pushFrame(new ResultsFrame(res));
    }

    public void pushException(BenchmarkException error) throws IOException {
        pushFrame(new ExceptionFrame(error));
    }

    public PrintStream getOutStream() {
        return streamOut;
    }

    public PrintStream getErrStream() {
        return streamErr;
    }

    public OutputFormat getOutputFormatHook() {
        return (OutputFormat) Proxy.newProxyInstance(
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
