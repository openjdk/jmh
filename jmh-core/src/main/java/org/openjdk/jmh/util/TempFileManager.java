/*
 * Copyright (c) 2016, Red Hat Inc.
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
package org.openjdk.jmh.util;

import java.io.File;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class TempFileManager {

    private final ReferenceQueue<TempFile> rq;
    private final Set<TempFileReference> refs;

    public TempFileManager() {
        rq = new ReferenceQueue<>();
        refs = new HashSet<>();
    }

    public TempFile create(String suffix) throws IOException {
        purge();
        File file = File.createTempFile("jmh", suffix);
        file.deleteOnExit();
        TempFile tf = new TempFile(file);
        refs.add(new TempFileReference(tf, rq));
        return tf;
    }

    public void purge() {
        TempFileReference ref;
        while ((ref = (TempFileReference) rq.poll()) != null) {
            if (ref.file != null) {
                ref.file.delete();
            }
            refs.remove(ref);
        }
    }

    private static class TempFileReference extends WeakReference<TempFile> {
        final File file;

        TempFileReference(TempFile referent, ReferenceQueue<? super TempFile> q) {
            super(referent, q);
            file = referent.file();
        }
    }

}
