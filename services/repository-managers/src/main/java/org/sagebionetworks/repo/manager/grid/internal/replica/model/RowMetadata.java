package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class RowMetadata {

	private SynapseRow synapseRow;
	private RowValidation rowValiation;
	/**
	 * The ID of the object that contains both 'synapseRow' and 'rowValiation'.
	 */
	private LogicalTimestamp objectId;

	public SynapseRow getSynapseRow() {
		return synapseRow;
	}

	public RowMetadata setSynapseRow(SynapseRow synapseRow) {
		this.synapseRow = synapseRow;
		return this;
	}

	public RowValidation getRowValiation() {
		return rowValiation;
	}

	public RowMetadata setRowValiation(RowValidation rowValiation) {
		this.rowValiation = rowValiation;
		return this;
	}

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public RowMetadata setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, rowValiation, synapseRow);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowMetadata other = (RowMetadata) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(rowValiation, other.rowValiation)
				&& Objects.equals(synapseRow, other.synapseRow);
	}

	@Override
	public String toString() {
		return "RowMetadata [synapseRow=" + synapseRow + ", rowValiation=" + rowValiation + ", objectId=" + objectId
				+ "]";
	}

}
