package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class SynapseRow {

	/**
	 * The ID of the constant that contains the JSON array with the 'rowId',
	 * 'versionNumber' and 'etag'.
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

	public List<LogicalTimestamp> getConstantIds() {
		return constantId == null ? Collections.emptyList() : List.of(constantId);
	}

	public SynapseRow setFromJSON(String json) {
		if (json == null) {
			return this;
		}
		return setFromJSONArray(new JSONArray(json));
	}

	public SynapseRow setFromJSONArray(JSONArray jsonArray) {
		if (jsonArray == null) {
			return this;
		}
		this.rowId = jsonArray.isNull(0) ? null : jsonArray.getLong(0);
		this.versionNumber = jsonArray.isNull(1) ? null : jsonArray.getLong(1);
		this.etag = jsonArray.isNull(2) ? null : jsonArray.getString(2);
		return this;
	}

	public String toJSON() {
		return toJSONArray().toString();
	}

	public JSONArray toJSONArray() {
		return new JSONArray().put(this.rowId != null ? this.rowId : JSONObject.NULL)
				.put(this.versionNumber != null ? this.versionNumber : JSONObject.NULL)
				.put(this.etag != null ? this.etag : JSONObject.NULL);
	}

	public ConValue toConValue() {
		return new ConValue(ConType.JSON_ARRAY, toJSONArray());
	}
	
	public SynapseRow setFromConValue(ConValue value) {
		if(value == null) {
			return this;
		}
		if(!ConType.JSON_ARRAY.equals(value.getType())) {
			throw new IllegalArgumentException("Expected a ContType.JSON_ARRAY");
		}
		return setFromJSONArray((JSONArray) value.getValue());
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
		return "SynapseRow [constantId=" + constantId + ", rowId=" + rowId + ", versionNumber=" + versionNumber
				+ ", etag=" + etag + "]";
	}

}
