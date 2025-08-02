package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

public class SynapseRow {

	/**
	 * The ID of the object that contains: 'rowId', 'versionNmber', and 'etag'.
	 */
	private LogicalTimestamp objectId;
	/**
	 * The metadata is first read from the DB as constant IDs. This map is used to
	 * hold those IDs temporarily, until they can be resolved with a secondary
	 * constant lookup.
	 */
	private Map<String, LogicalTimestamp> tempConstantMap;
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

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public SynapseRow setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	public SynapseRow setTempObject(String json) {
		JSONObject jsonOb = new JSONObject(json);
		if (jsonOb.length() > 0) {
			this.tempConstantMap = new HashMap<>(jsonOb.length());
			jsonOb.keySet().stream().forEach(k -> {
				this.tempConstantMap.put(k, LogicalTimestampCompactSerializable.deserialize(jsonOb.getJSONArray(k)));
			});
		}
		return this;
	}

	public Collection<LogicalTimestamp> listConstantsIds() {
		return tempConstantMap == null ? Collections.emptyList() : tempConstantMap.values();
	}

	public void resovleConstants(Map<LogicalTimestamp, ConstantNode> constants) {
		if (this.tempConstantMap != null) {
			LogicalTimestamp rowIdId = this.tempConstantMap.get("rowId");
			if (rowIdId != null) {
				this.rowId = Long.parseLong(constants.get(rowIdId).getValue().toString());
			}
			LogicalTimestamp versionId = this.tempConstantMap.get("versionNumber");
			if (versionId != null) {
				this.versionNumber = Long.parseLong(constants.get(versionId).getValue().toString());
			}
			LogicalTimestamp etagId = this.tempConstantMap.get("etag");
			if (etagId != null) {
				this.etag = (String) constants.get(etagId).getValue();
			}
			this.tempConstantMap = null;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, objectId, rowId, versionNumber);
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
		return Objects.equals(etag, other.etag) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(rowId, other.rowId) && Objects.equals(versionNumber, other.versionNumber);
	}

	@Override
	public String toString() {
		return "SynapseRow [objectId=" + objectId + ", rowId=" + rowId + ", versionNumber=" + versionNumber + ", etag="
				+ etag + "]";
	}

}
