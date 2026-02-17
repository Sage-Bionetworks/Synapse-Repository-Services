package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class RowData {

    private List<ConstantNode> nodes;
    private LogicalTimestamp vectorId;
    private JSONObject rowJsonDocument;

    public List<ConValue> getCells() {
        if (nodes == null) {
            return null;
        }
        return nodes.stream().map(ConstantNode::getConValue).collect(Collectors.toList());
    }

    public List<ConstantNode> getNodes() {
        return nodes;
    }

    public RowData setNodes(List<ConstantNode> nodes) {
        this.nodes = nodes;
        return this;
    }

    public LogicalTimestamp getVectorId() {
        return vectorId;
    }

    public RowData setVectorId(LogicalTimestamp vectorId) {
        this.vectorId = vectorId;
        return this;
    }

    public JSONObject getRowJsonDocument() {
        return rowJsonDocument;
    }

    public RowData setRowJsonDocument(JSONObject rowJsonDocument) {
        this.rowJsonDocument = rowJsonDocument;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowJsonDocument, vectorId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RowData other = (RowData) obj;
        if (vectorId == null) {
            if (other.vectorId != null)
                return false;
        } else if (!vectorId.equals(other.vectorId)) {
            return false;
        }
        if (rowJsonDocument == null) {
            if (other.rowJsonDocument != null)
                return false;
        } else if (!rowJsonDocument.similar(other.rowJsonDocument)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "RowData [nodes=" + nodes + ", vectorId=" + vectorId + ", rowJsonDocument=" + rowJsonDocument + "]";
    }

}
