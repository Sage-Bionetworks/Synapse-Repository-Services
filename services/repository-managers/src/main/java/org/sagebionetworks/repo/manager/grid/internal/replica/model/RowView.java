package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.CrdtId;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public class RowView {

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

	public ConValue getCell(int selectedColumnIndex) {
		RowData data = rowObject != null ? rowObject.getData() : null;
		return data == null ? null : data.getCell(selectedColumnIndex);
	}

	public RowMetadata getRowMetadata() {
		return rowObject != null ? rowObject.getMetadata() : null;
	}

	public LogicalTimestamp getRowMetadataNodeId() {
		return getRowMetadata() != null? getRowMetadata().getObjectId(): null;
	}

	public SynapseRow getSynapseRow() {
		return rowObject != null ? rowObject.getSynapseRow() : null;
	}
	
	/**
	 * Get the compact logical timestamp string for this row, or null if any link is null.
	 */
	public String getRowId() {
		if (rowObject == null || rowObject.getData() == null || rowObject.getData().getVectorId() == null) {
			return null;
		}
		return rowObject.getData().getVectorId().toCompact();
	}
	
	public static CrdtId createCrdtIdFromLogical(LogicalTimestamp timestamp) {
		return new CrdtId().setRep(timestamp.getReplicaId()).setSeq(timestamp.getSequenceNumber());
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
