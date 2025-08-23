package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public class RowMetadata {

	private SynapseRow synapseRow;
	private RowValidation rowValidation;
	/**
	 * The ID of the object that contains both 'synapseRow' and 'rowValidation'.
	 */
	private LogicalTimestamp objectId;

	public SynapseRow getSynapseRow() {
		return synapseRow;
	}

	public RowMetadata setSynapseRow(SynapseRow synapseRow) {
		this.synapseRow = synapseRow;
		return this;
	}

	public RowValidation getRowValidation() {
		return rowValidation;
	}

	public RowMetadata setRowValidation(RowValidation rowValidation) {
		this.rowValidation = rowValidation;
		return this;
	}

	public ValidationResults getRowValidationResults() {
		return rowValidation != null ? rowValidation.getValidationResults() : null;
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
		return Objects.hash(objectId, rowValidation, synapseRow);
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
		return Objects.equals(objectId, other.objectId) && Objects.equals(rowValidation, other.rowValidation)
				&& Objects.equals(synapseRow, other.synapseRow);
	}

	@Override
	public String toString() {
		return "RowMetadata [synapseRow=" + synapseRow + ", rowValidation=" + rowValidation + ", objectId=" + objectId
				+ "]";
	}

}
