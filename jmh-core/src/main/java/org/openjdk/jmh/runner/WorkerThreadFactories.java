/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class WorkerThreadFactories {

    static class PlatformThreadFactory implements ThreadFactory {

        private final AtomicInteger counter;
        private final String prefix;
        private final ThreadFactory factory;

        public PlatformThreadFactory(String prefix) {
            this.counter = new AtomicInteger();
            this.prefix = prefix;
            this.factory = Executors.defaultThreadFactory();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = factory.newThread(r);
            thread.setName(prefix + "-jmh-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    static ThreadFactory platformWorkerFactory(String prefix) {
        return new PlatformThreadFactory(prefix);
    }

    static ThreadFactory virtualWorkerFactory(String prefix) {
        // This API is only available in JDK 21+. Use reflection to make the code compilable with lower JDKs.
        try {
            Method m = Class.forName("java.lang.Thread").getMethod("ofVirtual");
            Object threadBuilder = m.invoke(null);
            Class<?> threadBuilderClazz = Class.forName("java.lang.Thread$Builder");
            m = threadBuilderClazz.getMethod("name", String.class, long.class);
            m.invoke(threadBuilder, prefix + "-jmh-worker-", 1L);
            m = threadBuilderClazz.getMethod("factory");
            return (ThreadFactory) m.invoke(threadBuilder);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException | NullPointerException e) {
            throw new RuntimeException("Cannot instantiate VirtualThreadFactory", e);
        }
    }

}
