package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public class RowObject {

	private RowData data;
	private RowMetadata metadata;
	/**
	 * The ID of the object that contains both 'data' and 'metadata'
	 */
	private LogicalTimestamp objectId;

	public RowData getData() {
		return data;
	}

	public RowObject setData(RowData data) {
		this.data = data;
		return this;
	}

	public RowMetadata getMetadata() {
		return metadata;
	}

	public RowObject setMetadata(RowMetadata metadata) {
		this.metadata = metadata;
		return this;
	}

	public RowObject setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public ValidationResults getRowValidationResults() {
		return metadata != null ? metadata.getRowValidationResults() : null;
	}

	public RowValidation getRowValidation() {
		return metadata != null ? metadata.getRowValidation() : null;
	}
	
	public SynapseRow getSynapseRow() {
		return metadata != null ? metadata.getSynapseRow() : null;
	}


	@Override
	public int hashCode() {
		return Objects.hash(data, metadata, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowObject other = (RowObject) obj;
		return Objects.equals(data, other.data) && Objects.equals(metadata, other.metadata)
				&& Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "RowObject [data=" + data + ", metadata=" + metadata + ", objectId=" + objectId + "]";
	}



}
