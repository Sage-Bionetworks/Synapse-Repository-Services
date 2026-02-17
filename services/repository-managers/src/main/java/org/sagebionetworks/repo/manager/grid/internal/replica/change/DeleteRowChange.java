package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class DeleteRowChange implements IntendedChange {

	private final LogicalTimestamp arrId;
	private final LogicalTimestamp rgaNodeId;

	public DeleteRowChange(LogicalTimestamp arrId, LogicalTimestamp rgaNodeId) {
		super();
		this.arrId = arrId;
		this.rgaNodeId = rgaNodeId;
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.delete_row;
	}

	@Override
	public JSONObject toJson() {
		return null;
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
		DeleteRowChange other = (DeleteRowChange) obj;
		return Objects.equals(arrId, other.arrId) && Objects.equals(rgaNodeId, other.rgaNodeId);
	}

	@Override
	public String toString() {
		return "DeleteRowChange [arrId=" + arrId + ", rgaNodeId=" + rgaNodeId + "]";
	}

}
