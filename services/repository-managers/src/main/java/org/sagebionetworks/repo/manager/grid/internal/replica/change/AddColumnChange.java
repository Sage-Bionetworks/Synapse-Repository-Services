package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;

public class AddColumnChange implements IntendedChange {

	private final LogicalTimestamp columnOrderArrId;
	private final LogicalTimestamp insertAfterId;
	private final ConValue columnIndex;

	public AddColumnChange(LogicalTimestamp columnOrderArrId, LogicalTimestamp insertAfterId, Long columnIndex) {
		ValidateArgument.required(columnIndex, "columnIndex");
		ValidateArgument.required(columnOrderArrId, "columnOrderArrId");
		ValidateArgument.required(insertAfterId, "insertAfterId");
		this.columnOrderArrId = columnOrderArrId;
		this.insertAfterId = insertAfterId;
		this.columnIndex = new ConValue(ConType.LONG, columnIndex);
	}

	public AddColumnChange(JSONObject json) {
		ValidateArgument.required(json, "json");
		this.columnOrderArrId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("c"));
		this.insertAfterId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("a"));
		this.columnIndex = ConValue.fromCompact(json.getJSONArray("i"));
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.add_column;
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("c", LogicalTimestampCompactSerializable.serialize(columnOrderArrId));
		json.put("a", LogicalTimestampCompactSerializable.serialize(insertAfterId));
		json.put("i", columnIndex.toCompact());
		return json;
	}

	public LogicalTimestamp getColumnOrderArrId() {
		return columnOrderArrId;
	}

	public LogicalTimestamp getInsertAfterId() {
		return insertAfterId;
	}

	public ConValue getColumnIndex() {
		return columnIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnIndex, columnOrderArrId, insertAfterId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddColumnChange other = (AddColumnChange) obj;
		return Objects.equals(columnIndex, other.columnIndex)
				&& Objects.equals(columnOrderArrId, other.columnOrderArrId)
				&& Objects.equals(insertAfterId, other.insertAfterId);
	}

	@Override
	public String toString() {
		return "AddColumn [columnOrderArrId=" + columnOrderArrId + ", insertAfterId=" + insertAfterId + ", columnIndex="
				+ columnIndex + "]";
	}

}
