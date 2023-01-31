/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jmh.validation;

import com.sun.jna.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AffinitySupport {

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isSupported() {
        return isLinux();
    }

    public static void bind(int cpu) {
        if (isLinux()) {
            Linux.bind(cpu);
        } else {
            throw new IllegalStateException("Not implemented");
        }
    }

    public static void tryBind() {
        if (isLinux()) {
            Linux.tryBind();
        } else {
            throw new IllegalStateException("Not implemented");
        }
    }

    public static List<String> prepare() {
        if (isLinux()) {
            return Linux.prepare();
        } else {
            throw new IllegalStateException("Not implemented");
        }
    }

    public static void tryInit() {
        if (isLinux()) {
            Linux.tryInit();
        }
    }

    static class Linux {
        private static volatile CLibrary INSTANCE;
        private static boolean BIND_TRIED;

        /*
           Unpacks the libraries, and replies additional options for forked VMs.
         */
        public static List<String> prepare() {
            System.setProperty("jnidispatch.preserve", "true");
            Native.load("c", CLibrary.class);

            File file = new File(System.getProperty("jnidispatch.path"));
            String bootLibraryPath = file.getParent();

            // Need to rename the file to the proper name, otherwise JNA would not discover it
            File proper = new File(bootLibraryPath + '/' + System.mapLibraryName("jnidispatch"));
            file.renameTo(proper);

            return Arrays.asList(
                    "-Djna.nounpack=true",    // Should not unpack itself, but use predefined path
                    "-Djna.nosys=true",       // Should load from explicit path
                    "-Djna.noclasspath=true", // Should load from explicit path
                    "-Djna.boot.library.path=" + bootLibraryPath,
                    "-Djna.platform.library.path=" + System.getProperty("jna.platform.library.path")
            );
        }

        public static void tryInit() {
            if (INSTANCE == null) {
                synchronized (Linux.class) {
                    if (INSTANCE == null) {
                        INSTANCE = Native.load("c", CLibrary.class);
                    }
                }
            }
        }

        public static void bind(int cpu) {
            tryInit();

            final cpu_set_t cpuset = new cpu_set_t();
            cpuset.set(cpu);

            set(cpuset);
        }

        public static void tryBind() {
            if (BIND_TRIED) return;

            synchronized (Linux.class) {
                if (BIND_TRIED) return;

                tryInit();

                cpu_set_t cs = new cpu_set_t();
                get(cs);
                set(cs);

                BIND_TRIED = true;
            }
        }

        private static void get(cpu_set_t cpuset) {
            if (INSTANCE.sched_getaffinity(0, cpu_set_t.SIZE_OF, cpuset) != 0) {
                throw new IllegalStateException("Failed: " + Native.getLastError());
            }
        }

        private static void set(cpu_set_t cpuset) {
            if (INSTANCE.sched_setaffinity(0, cpu_set_t.SIZE_OF, cpuset) != 0) {
                throw new IllegalStateException("Failed: " + Native.getLastError());
            }
        }

        interface CLibrary extends Library {
            int sched_getaffinity(int pid, int size, cpu_set_t cpuset);
            int sched_setaffinity(int pid, int size, cpu_set_t cpuset);
        }

        public static class cpu_set_t extends Structure {
            private static final int CPUSET_SIZE = 1024;
            private static final int NCPU_BITS = 8 * NativeLong.SIZE;
            private static final int SIZE_OF = (CPUSET_SIZE / NCPU_BITS) * NativeLong.SIZE;

            public NativeLong[] __bits = new NativeLong[CPUSET_SIZE / NCPU_BITS];

            public cpu_set_t() {
                for (int i = 0; i < __bits.length; i++) {
                    __bits[i] = new NativeLong(0);
                }
            }

            public void set(int cpu) {
                int cIdx = cpu / NCPU_BITS;
                long mask = 1L << (cpu % NCPU_BITS);
                NativeLong bit = __bits[cIdx];
                bit.setValue(bit.longValue() | mask);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Collections.singletonList("__bits");
            }
        }
    }

}
