package org.sagebionetworks.repo.model.grid.encoding;

import java.util.Objects;

public class IndexedNodeHeader {
    private final int nodeType;
    private final long length;

    public IndexedNodeHeader(int nodeType, long length) {
        this.nodeType = nodeType;
        this.length = length;
    }

    public int getNodeType() {
        return nodeType;
    }

    public long getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IndexedNodeHeader that = (IndexedNodeHeader) o;
        return nodeType == that.nodeType && length == that.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeType, length);
    }

    @Override
    public String toString() {
        return "IndexedNodeHeader{" +
                "nodeType=" + nodeType +
                ", length=" + length +
                '}';
    }
}
