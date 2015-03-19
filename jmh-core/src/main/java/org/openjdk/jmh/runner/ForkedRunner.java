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

import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.link.BinaryLinkClient;
import org.openjdk.jmh.runner.options.Options;

import java.io.IOException;

/**
 * Runner frontend class. Responsible for running micro benchmarks in forked JVM.
 */
class ForkedRunner extends BaseRunner {

    private final BinaryLinkClient link;

    public ForkedRunner(Options options, BinaryLinkClient link) {
        super(options, link.getOutputFormat());
        this.link = link;
    }

    public void run() throws IOException, ClassNotFoundException {
        ActionPlan actionPlan = link.requestPlan();

        try {
            IterationResultAcceptor acceptor = new IterationResultAcceptor() {
                @Override
                public void accept(IterationResult iterationData) {
                    try {
                        link.pushResults(iterationData);
                    } catch (IOException e) {
                        // link had probably failed
                        throw new SavedIOException(e);
                    }
                }

                @Override
                public void acceptMeta(BenchmarkResultMetaData md) {
                    try {
                        link.pushResultMetadata(md);
                    } catch (IOException e) {
                        // link had probably failed
                        throw new SavedIOException(e);
                    }
                }
            };

            runBenchmarksForked(actionPlan, acceptor);
        } catch (BenchmarkException be) {
            link.pushException(be);
        } catch (SavedIOException ioe) {
            throw ioe.getCause();
        }

        out.flush();
        out.close();
    }

    static class SavedIOException extends RuntimeException {
        private final IOException e;

        public SavedIOException(IOException e) {
            super(e);
            this.e = e;
        }

        public IOException getCause() {
            return e;
        }
    }

}
