package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class AddColumn implements IntendedChange {

	private final LogicalTimestamp columnOrderArrId;
	private final LogicalTimestamp insertAfterId;
	private final ConValue columnIndex;

	public AddColumn(LogicalTimestamp columnOrderArrId, LogicalTimestamp insertAfterId, Long columnIndex) {
		ValidateArgument.required(columnIndex, "columnIndex");
		ValidateArgument.required(columnOrderArrId, "columnOrderArrId");
		ValidateArgument.required(insertAfterId, "insertAfterId");
		this.columnOrderArrId = columnOrderArrId;
		this.insertAfterId = insertAfterId;
		this.columnIndex = new ConValue(ConType.LONG, columnIndex);
	}

	@Override
	public IntendedChangeType getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject toJson() {
		// TODO Auto-generated method stub
		return null;
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
		AddColumn other = (AddColumn) obj;
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
