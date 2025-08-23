package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

public class UpdateMetadataChange implements IntendedChange {

	private LogicalTimestamp rowObjectId;
	private LogicalTimestamp rowMetadataId;
	private JSONObject validationState;

	public UpdateMetadataChange(JSONObject json) {
		JSONArray o = json.optJSONArray("o");
		rowObjectId = o != null ? LogicalTimestampCompactSerializable.deserialize(o) : null;
		JSONArray m = json.optJSONArray("m");
		rowMetadataId = m != null ? LogicalTimestampCompactSerializable.deserialize(m) : null;
		validationState = json.optJSONObject("state");
	}

	public UpdateMetadataChange() {
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		if (rowObjectId != null) {
			json.put("o", LogicalTimestampCompactSerializable.serialize(rowObjectId));
		}
		if (rowMetadataId != null) {
			json.put("m", LogicalTimestampCompactSerializable.serialize(rowMetadataId));
		}
		if (validationState != null) {
			json.put("state", validationState);
		}
		return json;
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_row_metadata;
	}

	public LogicalTimestamp getRowObjectId() {
		return rowObjectId;
	}

	public UpdateMetadataChange setRowObjectId(LogicalTimestamp rowObjectId) {
		this.rowObjectId = rowObjectId;
		return this;
	}

	public LogicalTimestamp getRowMetadataId() {
		return rowMetadataId;
	}

	public UpdateMetadataChange setRowMetadataId(LogicalTimestamp rowMetadataId) {
		this.rowMetadataId = rowMetadataId;
		return this;
	}

	public JSONObject getValidationState() {
		return validationState;
	}

	public UpdateMetadataChange setValidationState(JSONObject validationState) {
		this.validationState = validationState;
		return this;
	}

	public String validationStateAsJson() {
		return validationState != null ? validationState.toString() : null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rowMetadataId, rowObjectId, validationStateAsJson());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateMetadataChange other = (UpdateMetadataChange) obj;
		return Objects.equals(rowMetadataId, other.rowMetadataId) && Objects.equals(rowObjectId, other.rowObjectId)
				&& Objects.equals(validationStateAsJson(), other.validationStateAsJson());
	}

	@Override
	public String toString() {
		return "UpdateMetadataChange [rowObjectId=" + rowObjectId + ", rowMetadataId=" + rowMetadataId
				+ ", validationState=" + validationState + "]";
	}

}
