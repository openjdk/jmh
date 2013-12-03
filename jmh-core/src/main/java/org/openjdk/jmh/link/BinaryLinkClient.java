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

import org.openjdk.jmh.link.frames.BenchmarkRecordFrame;
import org.openjdk.jmh.link.frames.FinishingFrame;
import org.openjdk.jmh.link.frames.InfraFrame;
import org.openjdk.jmh.link.frames.OptionsFrame;
import org.openjdk.jmh.link.frames.OutputFormatFrame;
import org.openjdk.jmh.link.frames.ResultsFrame;
import org.openjdk.jmh.logic.results.BenchResult;
import org.openjdk.jmh.runner.BenchmarkRecord;
import org.openjdk.jmh.runner.options.Options;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;

public final class BinaryLinkClient {

    private final Socket clientSocket;

    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;

    public BinaryLinkClient(String hostName, int hostPort) throws IOException {
        this.clientSocket = new Socket(hostName, hostPort);
        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
        this.ois = new ObjectInputStream(clientSocket.getInputStream());
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
        clientSocket.close();
    }

    public void pushResults(BenchmarkRecord record, BenchResult result) throws IOException {
        oos.writeObject(new ResultsFrame(record, result));
        oos.flush();
    }

    public BenchmarkRecord requestNextWarmup() throws IOException, ClassNotFoundException {
        oos.writeObject(new InfraFrame(InfraFrame.Type.BULK_WARMUP_REQUEST));
        oos.flush();

        Object reply = ois.readObject();
        if (reply == null) {
            return null;
        } else if (reply instanceof BenchmarkRecordFrame) {
            return (((BenchmarkRecordFrame) reply).getBenchmark());
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }

    public BenchmarkRecord requestNextMeasurement() throws IOException, ClassNotFoundException {
        oos.writeObject(new InfraFrame(InfraFrame.Type.BENCHMARK_REQUEST));
        oos.flush();

        Object reply = ois.readObject();
        if (reply == null) {
            return null;
        } else if (reply instanceof BenchmarkRecordFrame) {
            return (((BenchmarkRecordFrame) reply).getBenchmark());
        } else {
            throw new IllegalStateException("Got the erroneous reply: " + reply);
        }
    }
}
