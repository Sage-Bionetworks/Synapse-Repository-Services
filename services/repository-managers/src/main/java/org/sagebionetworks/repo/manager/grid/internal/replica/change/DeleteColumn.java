package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class DeleteColumn implements IntendedChange {

	private final LogicalTimestamp getColumnOrderArrId;
	private final LogicalTimestamp toDeleteId;

	public DeleteColumn(LogicalTimestamp getColumnOrderArrId, LogicalTimestamp toDeleteId) {
		super();
		this.getColumnOrderArrId = getColumnOrderArrId;
		this.toDeleteId = toDeleteId;
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

	public LogicalTimestamp getGetColumnOrderArrId() {
		return getColumnOrderArrId;
	}

	public LogicalTimestamp getToDeleteId() {
		return toDeleteId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getColumnOrderArrId, toDeleteId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeleteColumn other = (DeleteColumn) obj;
		return Objects.equals(getColumnOrderArrId, other.getColumnOrderArrId)
				&& Objects.equals(toDeleteId, other.toDeleteId);
	}

	@Override
	public String toString() {
		return "DeleteColumn [getColumnOrderArrId=" + getColumnOrderArrId + ", toDeleteId=" + toDeleteId + "]";
	}

}
