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

import org.openjdk.jmh.results.BenchResult;
import org.openjdk.jmh.runner.ActionPlan;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Multimap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;

public final class BinaryLinkClient {

    private final Socket clientSocket;

    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private ForwardingPrintStream streamErr;
    private ForwardingPrintStream streamOut;

    public BinaryLinkClient(String hostName, int hostPort) throws IOException {
        this.clientSocket = new Socket(hostName, hostPort);
        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
        this.ois = new ObjectInputStream(clientSocket.getInputStream());
        this.streamErr = new ForwardingPrintStream(OutputFrame.Type.ERR);
        this.streamOut = new ForwardingPrintStream(OutputFrame.Type.OUT);
    }

    public Options requestOptions() throws IOException, ClassNotFoundException {
        oos.writeObject(new InfraFrame(InfraFrame.Type.OPTIONS_REQUEST));
        Object reply = ois.readObject();
        if (reply instanceof OptionsFrame) {
            return (((OptionsFrame) reply).getOpts());
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }

    public InvocationHandler getOutputFormatHandler() {
        return new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                oos.writeObject(new OutputFormatFrame(ClassConventions.getMethodName(method), args));
                oos.flush();
                return null; // expect null
            }
        };
    }

    public void close() throws IOException {
        oos.writeObject(new FinishingFrame());
        oos.flush();
        oos.close();
        streamErr.flush();
        streamErr.close();
        streamOut.flush();
        streamOut.close();
        clientSocket.close();
    }

    public void pushResults(Multimap<BenchmarkRecord, BenchResult> res) throws IOException {
        oos.writeObject(new ResultsFrame(res));
        oos.flush();
    }

    public ActionPlan requestPlan() throws IOException, ClassNotFoundException {
        oos.writeObject(new InfraFrame(InfraFrame.Type.ACTION_PLAN_REQUEST));
        oos.flush();

        Object reply = ois.readObject();
        if (reply instanceof ActionPlanFrame) {
            return ((ActionPlanFrame) reply).getActionPlan();
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }

    public void pushException(BenchmarkException error) throws IOException {
        oos.writeObject(new ExceptionFrame(error));
        oos.flush();
    }

    public PrintStream getErrStream() {
        return streamErr;
    }

    public PrintStream getOutStream() {
        return streamOut;
    }

    public OutputFormat getOutputFormatHook() {
        return (OutputFormat) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{OutputFormat.class},
                getOutputFormatHandler()
        );
    }

    class ForwardingPrintStream extends PrintStream {
        public ForwardingPrintStream(final OutputFrame.Type type) {
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    oos.writeObject(new OutputFrame(type, new byte[]{(byte) (b & 0xFF)}));
                    oos.flush();
                }

                @Override
                public void write(byte[] b) throws IOException {
                    oos.writeObject(new OutputFrame(type, Arrays.copyOf(b, b.length)));
                    oos.flush();
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    oos.writeObject(new OutputFrame(type, Arrays.copyOfRange(b, off, len + off)));
                    oos.flush();
                }
            });
        }
    }

}
