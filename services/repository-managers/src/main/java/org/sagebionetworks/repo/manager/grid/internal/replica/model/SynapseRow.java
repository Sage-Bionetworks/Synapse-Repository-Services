package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class SynapseRow implements HasConstantIds {

	/**
	 * The ID of the constant that contains the JSON array with the 'rowId', 'versionNumber' and 'etag'.
	 */
	private LogicalTimestamp constantId;
	
	private Long rowId;
	private Long versionNumber;
	private String etag;

	public Long getRowId() {
		return rowId;
	}

	public SynapseRow setRowId(Long rowId) {
		this.rowId = rowId;
		return this;
	}

	public Long getVersionNumber() {
		return versionNumber;
	}

	public SynapseRow setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public SynapseRow setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public LogicalTimestamp getConstantId() {
		return constantId;
	}

	public SynapseRow setConstantId(LogicalTimestamp constantId) {
		this.constantId = constantId;
		return this;
	}

	@Override
	public List<LogicalTimestamp> getConstantIds() {
		return constantId == null ? Collections.emptyList() : List.of(constantId);
	}

	@Override
	public void applyConstants(Map<LogicalTimestamp, ConstantNode> constants) {
		if (this.constantId != null) {
			JSONArray jsonArray = (JSONArray) constants.get(constantId).getValue();
			
			this.rowId = jsonArray.isNull(0) ? null : jsonArray.getLong(0);
			this.versionNumber = jsonArray.isNull(1) ? null : jsonArray.getLong(1);
			this.etag = jsonArray.isNull(2) ? null : jsonArray.getString(2);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, constantId, rowId, versionNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SynapseRow other = (SynapseRow) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(constantId, other.constantId)
				&& Objects.equals(rowId, other.rowId) && Objects.equals(versionNumber, other.versionNumber);
	}

	@Override
	public String toString() {
		return "SynapseRow [constantId=" + constantId + ", rowId=" + rowId + ", versionNumber=" + versionNumber + ", etag="
				+ etag + "]";
	}

}
