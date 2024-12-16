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

/**
 * Thread parameters.
 *
 * <p>Thread parameters handle the infrastructure info about the threading, including but
 * not limited to the number of threads, thread indicies, group information, etc. Some
 * of that info duplicates what is available in {@link org.openjdk.jmh.infra.BenchmarkParams}.</p>
 */
public final class ThreadParams extends ThreadParamsL2 {
    byte b3_00, b3_01, b3_02, b3_03, b3_04, b3_05, b3_06, b3_07, b3_08, b3_09, b3_0a, b3_0b, b3_0c, b3_0d, b3_0e, b3_0f;
    long b3_10, b3_11, b3_12, b3_13, b3_14, b3_15, b3_16, b3_17, b3_18, b3_19, b3_1a, b3_1b, b3_1c, b3_1d, b3_1e, b3_1f;
    long b3_20, b3_21, b3_22, b3_23, b3_24, b3_25, b3_26, b3_27, b3_28, b3_29, b3_2a, b3_2b, b3_2c, b3_2d;

    public ThreadParams(int threadIdx, int threadCount, int groupIdx, int groupCount, int subgroupIdx, int subgroupCount,
                        int groupThreadIdx, int groupThreadCount, int subgroupThreadIdx, int subgroupThreadCount) {
        super(threadIdx, threadCount, groupIdx, groupCount, subgroupIdx, subgroupCount,
                groupThreadIdx, groupThreadCount, subgroupThreadIdx, subgroupThreadCount);
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

abstract class ThreadParamsL1 {
    byte b1_00, b1_01, b1_02, b1_03, b1_04, b1_05, b1_06, b1_07, b1_08, b1_09, b1_0a, b1_0b, b1_0c, b1_0d, b1_0e, b1_0f;
    long b1_10, b1_11, b1_12, b1_13, b1_14, b1_15, b1_16, b1_17, b1_18, b1_19, b1_1a, b1_1b, b1_1c, b1_1d, b1_1e, b1_1f;
    long b1_20, b1_21, b1_22, b1_23, b1_24, b1_25, b1_26, b1_27, b1_28, b1_29, b1_2a, b1_2b, b1_2c, b1_2d;
}
