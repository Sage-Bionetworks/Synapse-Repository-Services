package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public class RowView implements HasConstantIds {

	/**
	 * The ID of the array node that represents this row. To insert a new row after
	 * this row, this ID would be used as the "reference" ID of the new array node.
	 */
	private LogicalTimestamp arrNodeId;
	/**
	 * The current index position of this row. This index will change if new rows
	 * are inserted or deleted before this node in the array.
	 */
	private Long rowIndex;
	/**
	 * This object contains both the row's data and metadata.
	 */
	private RowObject rowObject;

	public Long getRowIndex() {
		return rowIndex;
	}

	public RowView setRowIndex(Long rowIndex) {
		this.rowIndex = rowIndex;
		return this;
	}

	public RowObject getRowObject() {
		return rowObject;
	}

	public RowView setRowObject(RowObject rowObject) {
		this.rowObject = rowObject;
		return this;
	}

	public LogicalTimestamp getArrNodeId() {
		return arrNodeId;
	}

	public RowView setArrNodeId(LogicalTimestamp arrNodeId) {
		this.arrNodeId = arrNodeId;
		return this;
	}

	public ValidationResults getRowValidationResults() {
		return rowObject != null ? rowObject.getRowValidationResults() : null;
	}

	public RowValidation getRowValidation() {
		return rowObject != null ? rowObject.getRowValidation() : null;
	}

	public JSONArray getCells() {
		return rowObject != null ? rowObject.getCells() : null;
	}

	public RowMetadata getRowMetadata() {
		return rowObject != null ? rowObject.getMetadata() : null;
	}

	public SynapseRow getSynapseRow() {
		return rowObject != null ? rowObject.getSynapseRow() : null;
	}

	@Override
	public List<LogicalTimestamp> getConstantIds() {
		List<LogicalTimestamp> results = new ArrayList<>();
		RowValidation rowValidation = getRowValidation();
		if (rowValidation != null) {
			results.addAll(rowValidation.getConstantIds());
		}
		SynapseRow synapseRow = getSynapseRow();
		if (synapseRow != null) {
			results.addAll(synapseRow.getConstantIds());
		}
		return results;
	}

	@Override
	public void applyConstants(Map<LogicalTimestamp, ConstantNode> constants) {
		RowValidation rowValidation = getRowValidation();
		if (rowValidation != null) {
			rowValidation.applyConstants(constants);
		}
		SynapseRow synapseRow = getSynapseRow();
		if (synapseRow != null) {
			synapseRow.applyConstants(constants);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrNodeId, rowIndex, rowObject);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowView other = (RowView) obj;
		return Objects.equals(arrNodeId, other.arrNodeId) && Objects.equals(rowIndex, other.rowIndex)
				&& Objects.equals(rowObject, other.rowObject);
	}

	@Override
	public String toString() {
		return "RowView [arrNodeId=" + arrNodeId + ", rowIndex=" + rowIndex + ", rowObject=" + rowObject + "]";
	}

}
