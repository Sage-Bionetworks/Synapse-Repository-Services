package org.sagebionetworks.repo.model.grid.encoding;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class SnapshotFileIndex {
    private final Map<IndexedNodeCodecMapper, Map<LogicalTimestamp, NodePointer>> entriesByType;
    private final LogicalTimestamp rootNodeId;
    private final ClockTable clockTable;

    public SnapshotFileIndex(LogicalTimestamp rootNodeId, ClockTable clockTable, Map<IndexedNodeCodecMapper, Map<LogicalTimestamp, NodePointer>> entriesByType) {
        this.rootNodeId = rootNodeId;
        this.clockTable = clockTable;
        this.entriesByType = entriesByType;
    }

    public Map<LogicalTimestamp, NodePointer> getEntriesForType(IndexedNodeCodecMapper type) {
        return Collections.unmodifiableMap(entriesByType.get(type));
    }

    public int getTotalNodeCount() {
        return entriesByType.values().stream().mapToInt(Map::size).sum();
    }

    public ClockTable getClockTable() {
        return clockTable;
    }

    public LogicalTimestamp getRootNodeId() {
        return rootNodeId;
    }

    public NodePointer getPointer(IndexedNodeCodecMapper type, LogicalTimestamp nodeId) {
        ValidateArgument.required(type, "type");
        ValidateArgument.required(nodeId, "nodeId");
        return entriesByType.get(type).get(nodeId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotFileIndex that = (SnapshotFileIndex) o;
        return Objects.equals(entriesByType, that.entriesByType) && Objects.equals(rootNodeId, that.rootNodeId) && Objects.equals(clockTable, that.clockTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entriesByType, rootNodeId, clockTable);
    }

    @Override
    public String toString() {
        return "SnapshotFileIndex{" +
                "entriesByType=" + entriesByType +
                ", rootNodeId=" + rootNodeId +
                ", clockTable=" + clockTable +
                '}';
    }

    /**
     * An entry in the index representing a single node.
     */
    public static class NodePointer {
        private final long byteOffset;
        private final int binaryLength;

        public NodePointer(long byteOffset, int binaryLength) {
            this.byteOffset = byteOffset;
            this.binaryLength = binaryLength;
        }

        public long byteOffset() {
            return byteOffset;
        }

        public int binaryLength() {
            return binaryLength;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodePointer nodePointer = (NodePointer) o;
            return byteOffset == nodePointer.byteOffset && binaryLength == nodePointer.binaryLength;
        }

        @Override
        public int hashCode() {
            return Objects.hash(byteOffset, binaryLength);
        }

        @Override
        public String toString() {
            return "Entry{byteOffset=" + byteOffset + ", binaryLength=" + binaryLength + "}";
        }
    }
}
