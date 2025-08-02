package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

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
