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
package org.openjdk.jmh.infra;

import org.openjdk.jmh.util.Utils;

/**
 * Thread parameters.
 *
 * <p>Thread parameters handle the infrastructure info about the threading, including but
 * not limited to the number of threads, thread indicies, group information, etc. Some
 * of that info duplicates what is available in {@link org.openjdk.jmh.infra.BenchmarkParams}.</p>
 */
public final class ThreadParams extends ThreadParamsL4 {
    public ThreadParams(int threadIdx, int threadCount, int groupIdx, int groupCount, int subgroupIdx, int subgroupCount,
                        int groupThreadIdx, int groupThreadCount, int subgroupThreadIdx, int subgroupThreadCount) {
        super(threadIdx, threadCount, groupIdx, groupCount, subgroupIdx, subgroupCount,
                groupThreadIdx, groupThreadCount, subgroupThreadIdx, subgroupThreadCount);
    }

    static {
        Utils.check(ThreadParams.class, "threadIdx", "threadCount");
        Utils.check(ThreadParams.class, "groupIdx", "groupCount");
        Utils.check(ThreadParams.class, "subgroupIdx", "subgroupCount");
        Utils.check(ThreadParams.class, "groupThreadIdx", "groupThreadCount");
        Utils.check(ThreadParams.class, "subgroupThreadIdx", "subgroupThreadCount");
    }

    /**
     * Answers the number of groups in the run.
     *
     * <p>When running the symmetric benchmark, each thread occupies its own group,
     * and therefore number of groups equals the thread count.</p>
     *
     * <p>This is a convenience method, similar info can be figured out by dividing
     * the number of threads ({@link #getThreadCount()}) by the number of threads per
     * group ({@link #getGroupThreadCount()}).</p>
     *
     * @return number of groups
     * @see #getThreadCount()
     */
    public int getGroupCount() {
        return groupCount;
    }

    /**
     * Answers the thread group index.
     *
     * <p>Group indices are having the range of [0..G-1], where G is the number
     * of thread groups in the run. When running the symmetric benchmark, each
     * thread occupies its own group, and therefore the group index equals to
     * the thread index.</p>
     *
     * @return thread group index
     * @see #getGroupCount()
     */
    public int getGroupIndex() {
        return groupIdx;
    }

    /**
     * Answers the number of distinct workloads (subgroups) in the current group.
     *
     * <p>When running the symmetric benchmark, each thread occupies its own group,
     * and therefore number of subgroups equals to one.</p>
     *
     * @return number of subgroups
     * @see #getThreadCount()
     */
    public int getSubgroupCount() {
        return subgroupCount;
    }

    /**
     * Answers the subgroup index.
     *
     * <p>Subgroup index enumerates the distinct workloads (subgroups) in current
     * group. The index the range of [0..S-1], where S is the number of subgroups
     * in current group. When running the symmetric benchmark, there is only
     * a single workload in the group, and therefore the subgroup index is zero.</p>
     *
     * @return subgroup index
     * @see #getSubgroupCount()
     */
    public int getSubgroupIndex() {
        return subgroupIdx;
    }

    /**
     * Answers the number of threads participating in the run.
     *
     * <p>This is a convenience method, similar info can be queried directly from
     * {@link org.openjdk.jmh.infra.BenchmarkParams#getThreads()}</p>
     *
     * @return number of threads
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Answers the thread index.
     *
     * <p>Thread indices are in range [0..N-1], where N is the number of threads
     * participating in the run.</p>
     *
     * @return thread index
     * @see #getThreadCount()
     */
    public int getThreadIndex() {
        return threadIdx;
    }

    /**
     * Answers the number of threads in the current group.
     *
     * <p>When running the symmetric benchmark, each thread occupies its own group,
     * and therefore number of subgroups equals to one.</p>
     *
     * <p>This is a convenience method, similar info can be figured out by summing
     * up the thread distribution from
     * {@link org.openjdk.jmh.infra.BenchmarkParams#getThreadGroups()}.</p>
     *
     * @return number of threads in the group
     * @see #getThreadCount()
     */
    public int getGroupThreadCount() {
        return groupThreadCount;
    }

    /**
     * Answers the thread sub-index in current group.
     *
     * <p>Subgroup index enumerates the thread within a group, and takes
     * the range of [0..T-1], where T is the number of threads in current
     * group. When running the symmetric benchmark, each thread occupies
     * its own group, and therefore the subgroup index is zero.</p>
     *
     * @return index of thread in the group
     * @see #getGroupThreadCount()
     */
    public int getGroupThreadIndex() {
        return groupThreadIdx;
    }

    /**
     * Answers the number of threads in the current subgroup.
     *
     * <p>When running the symmetric benchmark, each thread occupies its own group,
     * each thread implicitly occupies a single subgroup, and therefore, the number
     * of subgroups equals to one.</p>
     *
     * <p>This is a convenience method, similar info can be figured out with
     * querying {@link org.openjdk.jmh.infra.BenchmarkParams#getThreadGroups()} with
     * {@link #getSubgroupIndex()} used as index.</p>
     *
     * @return number of threads in subgroup
     * @see #getThreadCount()
     */
    public int getSubgroupThreadCount() {
        return subgroupThreadCount;
    }

    /**
     * Answers the thread sub-index in current subgroup.
     *
     * <p>Subgroup index enumerates the thread within a subgroup, and takes
     * the range of [0..T-1], where T is the number of threads in current
     * subgroup. When running the symmetric benchmark, each thread occupies
     * its own group, and therefore the subgroup index is zero.</p>
     *
     * @return index of thread in subgroup
     * @see #getSubgroupThreadCount()
     */
    public int getSubgroupThreadIndex() {
        return subgroupThreadIdx;
    }

}

abstract class ThreadParamsL0 {
    private int markerBegin;
}

abstract class ThreadParamsL1 extends ThreadParamsL0 {
    private boolean p001, p002, p003, p004, p005, p006, p007, p008;
    private boolean p011, p012, p013, p014, p015, p016, p017, p018;
    private boolean p021, p022, p023, p024, p025, p026, p027, p028;
    private boolean p031, p032, p033, p034, p035, p036, p037, p038;
    private boolean p041, p042, p043, p044, p045, p046, p047, p048;
    private boolean p051, p052, p053, p054, p055, p056, p057, p058;
    private boolean p061, p062, p063, p064, p065, p066, p067, p068;
    private boolean p071, p072, p073, p074, p075, p076, p077, p078;
    private boolean p101, p102, p103, p104, p105, p106, p107, p108;
    private boolean p111, p112, p113, p114, p115, p116, p117, p118;
    private boolean p121, p122, p123, p124, p125, p126, p127, p128;
    private boolean p131, p132, p133, p134, p135, p136, p137, p138;
    private boolean p141, p142, p143, p144, p145, p146, p147, p148;
    private boolean p151, p152, p153, p154, p155, p156, p157, p158;
    private boolean p161, p162, p163, p164, p165, p166, p167, p168;
    private boolean p171, p172, p173, p174, p175, p176, p177, p178;
}

abstract class ThreadParamsL2 extends ThreadParamsL1 {
    protected final int threadIdx, threadCount;
    protected final int groupIdx, groupCount;
    protected final int subgroupIdx, subgroupCount;
    protected final int groupThreadIdx, groupThreadCount;
    protected final int subgroupThreadIdx, subgroupThreadCount;

    public ThreadParamsL2(int threadIdx, int threadCount, int groupIdx, int groupCount, int subgroupIdx, int subgroupCount,
                          int groupThreadIdx, int groupThreadCount, int subgroupThreadIdx, int subgroupThreadCount) {
        this.threadIdx = threadIdx;
        this.threadCount = threadCount;
        this.groupIdx = groupIdx;
        this.groupCount = groupCount;
        this.subgroupIdx = subgroupIdx;
        this.subgroupCount = subgroupCount;
        this.groupThreadIdx = groupThreadIdx;
        this.groupThreadCount = groupThreadCount;
        this.subgroupThreadIdx = subgroupThreadIdx;
        this.subgroupThreadCount = subgroupThreadCount;
    }
}

abstract class ThreadParamsL3 extends ThreadParamsL2 {
    private boolean q001, q002, q003, q004, q005, q006, q007, q008;
    private boolean q011, q012, q013, q014, q015, q016, q017, q018;
    private boolean q021, q022, q023, q024, q025, q026, q027, q028;
    private boolean q031, q032, q033, q034, q035, q036, q037, q038;
    private boolean q041, q042, q043, q044, q045, q046, q047, q048;
    private boolean q051, q052, q053, q054, q055, q056, q057, q058;
    private boolean q061, q062, q063, q064, q065, q066, q067, q068;
    private boolean q071, q072, q073, q074, q075, q076, q077, q078;
    private boolean q101, q102, q103, q104, q105, q106, q107, q108;
    private boolean q111, q112, q113, q114, q115, q116, q117, q118;
    private boolean q121, q122, q123, q124, q125, q126, q127, q128;
    private boolean q131, q132, q133, q134, q135, q136, q137, q138;
    private boolean q141, q142, q143, q144, q145, q146, q147, q148;
    private boolean q151, q152, q153, q154, q155, q156, q157, q158;
    private boolean q161, q162, q163, q164, q165, q166, q167, q168;
    private boolean q171, q172, q173, q174, q175, q176, q177, q178;

    public ThreadParamsL3(int threadIdx, int threadCount, int groupIdx, int groupCount, int subgroupIdx, int subgroupCount,
                          int groupThreadIdx, int groupThreadCount, int subgroupThreadIdx, int subgroupThreadCount) {
        super(threadIdx, threadCount, groupIdx, groupCount, subgroupIdx, subgroupCount,
                groupThreadIdx, groupThreadCount, subgroupThreadIdx, subgroupThreadCount);
    }
}

abstract class ThreadParamsL4 extends ThreadParamsL3 {
    private int markerEnd;

    public ThreadParamsL4(int threadIdx, int threadCount, int groupIdx, int groupCount, int subgroupIdx, int subgroupCount,
                          int groupThreadIdx, int groupThreadCount, int subgroupThreadIdx, int subgroupThreadCount) {
        super(threadIdx, threadCount, groupIdx, groupCount, subgroupIdx, subgroupCount,
                groupThreadIdx, groupThreadCount, subgroupThreadIdx, subgroupThreadCount);
    }
}
