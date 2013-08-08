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
package org.openjdk.jmh.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compiler control annotation is the VM-specific way to affect the compilation for
 * particular methods in the microbenchmarks.
 * <p/>
 * JMH interfaces with the JVM by the means of command-line switches,
 * These annotations only work with forking enabled. Non-forked runs will not be
 * able to pass the hints to the compiler.
 * <p/>
 * Also, these control annotations might get freely ignored by the compiler,
 * reduced to no-ops, or otherwise invalidated. Be cautious.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface CompilerControl {

    /**
     * The compilation mode.
     * @return mode
     */
    Mode value();

    /**
     * Compilation mode.
     */
    public enum Mode {

        /**
         * Insert the breakpoint into the generated compiled code.
         */
        BREAK("break"),

        /**
         * Print the method and it's profile.
         */
        PRINT("print"),

        /**
         * Exclude the method from the compilation.
         */
        EXCLUDE("exclude"),

        /**
         * Force inline.
         */
        INLINE("inline"),

        /**
         * Force skip inline.
         */
        DONT_INLINE("dontinline"),

        /**
         * Compile only this method, and nothing else.
         */
        COMPILE_ONLY("compileonly"),;

        private final String command;

        Mode(String command) {
            this.command = command;
        }

        public String command() {
            return command;
        }
    }

}
