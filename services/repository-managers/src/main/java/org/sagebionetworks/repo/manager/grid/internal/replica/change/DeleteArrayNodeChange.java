package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class DeleteArrayNodeChange implements IntendedChange {

	private final LogicalTimestamp arrId;
	private final LogicalTimestamp rgaNodeId;

	public DeleteArrayNodeChange(LogicalTimestamp arrId, LogicalTimestamp rgaNodeId) {
		ValidateArgument.required(arrId, "arrId");
		ValidateArgument.required(rgaNodeId, "rgaNodeId");
		this.arrId = arrId;
		this.rgaNodeId = rgaNodeId;
	}

	public DeleteArrayNodeChange(JSONObject json) {
		ValidateArgument.required(json, "json");
		this.arrId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("a"));
		this.rgaNodeId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("r"));
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.delete_array_node;
	}
	
	public LogicalTimestamp getArrId() {
		return arrId;
	}

	public LogicalTimestamp getRgaNodeId() {
		return rgaNodeId;
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("a", LogicalTimestampCompactSerializable.serialize(arrId));
		json.put("r", LogicalTimestampCompactSerializable.serialize(rgaNodeId));
		return json;
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrId, rgaNodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeleteArrayNodeChange other = (DeleteArrayNodeChange) obj;
		return Objects.equals(arrId, other.arrId) && Objects.equals(rgaNodeId, other.rgaNodeId);
	}

	@Override
	public String toString() {
		return "DeleteRowChange [arrId=" + arrId + ", rgaNodeId=" + rgaNodeId + "]";
	}

}
