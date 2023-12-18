/*
 * Copyright 2023 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.connectors.base.source.meta.split;

import com.ververica.cdc.connectors.base.source.meta.offset.Offset;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The split to describe the change log of database table(s). */
public class StreamSplit extends SourceSplitBase {

    private final Offset startingOffset;
    private final Offset endingOffset;
    private final List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos;
    private final Map<TableId, TableChange> tableSchemas;
    private final int totalFinishedSplitSize;
    @Nullable transient byte[] serializedFormCache;
    private final boolean isSuspended;

    public StreamSplit(
            String splitId,
            Offset startingOffset,
            Offset endingOffset,
            List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos,
            Map<TableId, TableChange> tableSchemas,
            int totalFinishedSplitSize,
            boolean isSuspended) {
        super(splitId);
        this.startingOffset = startingOffset;
        this.endingOffset = endingOffset;
        this.finishedSnapshotSplitInfos = finishedSnapshotSplitInfos;
        this.tableSchemas = tableSchemas;
        this.totalFinishedSplitSize = totalFinishedSplitSize;
        this.isSuspended = isSuspended;
    }

    public StreamSplit(
            String splitId,
            Offset startingOffset,
            Offset endingOffset,
            List<FinishedSnapshotSplitInfo> finishedSnapshotSplitInfos,
            Map<TableId, TableChange> tableSchemas,
            int totalFinishedSplitSize) {
        super(splitId);
        this.startingOffset = startingOffset;
        this.endingOffset = endingOffset;
        this.finishedSnapshotSplitInfos = finishedSnapshotSplitInfos;
        this.tableSchemas = tableSchemas;
        this.totalFinishedSplitSize = totalFinishedSplitSize;
        this.isSuspended = false;
    }

    public Offset getStartingOffset() {
        return startingOffset;
    }

    public Offset getEndingOffset() {
        return endingOffset;
    }

    public List<FinishedSnapshotSplitInfo> getFinishedSnapshotSplitInfos() {
        return finishedSnapshotSplitInfos;
    }

    @Override
    public Map<TableId, TableChange> getTableSchemas() {
        return tableSchemas;
    }

    public int getTotalFinishedSplitSize() {
        return totalFinishedSplitSize;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public boolean isCompletedSplit() {
        return totalFinishedSplitSize == finishedSnapshotSplitInfos.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StreamSplit)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        StreamSplit that = (StreamSplit) o;
        return totalFinishedSplitSize == that.totalFinishedSplitSize
                && Objects.equals(isSuspended, that.isSuspended)
                && Objects.equals(startingOffset, that.startingOffset)
                && Objects.equals(endingOffset, that.endingOffset)
                && Objects.equals(finishedSnapshotSplitInfos, that.finishedSnapshotSplitInfos)
                && Objects.equals(tableSchemas, that.tableSchemas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                startingOffset,
                endingOffset,
                finishedSnapshotSplitInfos,
                tableSchemas,
                totalFinishedSplitSize,
                isSuspended);
    }

    @Override
    public String toString() {
        return "StreamSplit{"
                + "splitId='"
                + splitId
                + '\''
                + ", offset="
                + startingOffset
                + ", endOffset="
                + endingOffset
                + ", isSuspended="
                + isSuspended
                + '}';
    }

    // -------------------------------------------------------------------
    // factory utils to build new StreamSplit instance
    // -------------------------------------------------------------------
    public static StreamSplit appendFinishedSplitInfos(
            StreamSplit streamSplit, List<FinishedSnapshotSplitInfo> splitInfos) {
        splitInfos.addAll(streamSplit.getFinishedSnapshotSplitInfos());
        return new StreamSplit(
                streamSplit.splitId,
                streamSplit.getStartingOffset(),
                streamSplit.getEndingOffset(),
                splitInfos,
                streamSplit.getTableSchemas(),
                streamSplit.getTotalFinishedSplitSize(),
                streamSplit.isSuspended());
    }

    public static StreamSplit fillTableSchemas(
            StreamSplit streamSplit, Map<TableId, TableChange> tableSchemas) {
        tableSchemas.putAll(streamSplit.getTableSchemas());
        return new StreamSplit(
                streamSplit.splitId,
                streamSplit.getStartingOffset(),
                streamSplit.getEndingOffset(),
                streamSplit.getFinishedSnapshotSplitInfos(),
                tableSchemas,
                streamSplit.getTotalFinishedSplitSize(),
                streamSplit.isSuspended());
    }

    public static StreamSplit toNormalStreamSplit(
            StreamSplit suspendedStreamSplit, int totalFinishedSplitSize) {
        return new StreamSplit(
                suspendedStreamSplit.splitId,
                suspendedStreamSplit.getStartingOffset(),
                suspendedStreamSplit.getEndingOffset(),
                suspendedStreamSplit.getFinishedSnapshotSplitInfos(),
                suspendedStreamSplit.getTableSchemas(),
                totalFinishedSplitSize,
                false);
    }

    public static StreamSplit toSuspendedStreamSplit(StreamSplit normalStreamSplit) {
        return new StreamSplit(
                normalStreamSplit.splitId,
                normalStreamSplit.getStartingOffset(),
                normalStreamSplit.getEndingOffset(),
                new ArrayList<>(),
                new HashMap<>(),
                normalStreamSplit.getTotalFinishedSplitSize(),
                true);
    }
}
