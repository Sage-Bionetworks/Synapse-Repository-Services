package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertObject implements Operation<InsertObject> {

	private LogicalTimestamp operationId;
	private LogicalTimestamp objectId;
	private Map<String, LogicalTimestamp> map;

	@Override
	public OperationType getType() {
		return OperationType.ins_obj;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	@Override
	public long getSpan() {
		return 1;
	}

	public Map<String, LogicalTimestamp> getMap() {
		return map;
	}

	public InsertObject setMap(Map<String, LogicalTimestamp> map) {
		this.map = map;
		return this;
	}

	public InsertObject setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
	}

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public InsertObject setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(map, objectId, operationId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsertObject other = (InsertObject) obj;
		return Objects.equals(map, other.map) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(operationId, other.operationId);
	}

	@Override
	public String toString() {
		return "InsertObject [operationId=" + operationId + ", objectId=" + objectId + ", map=" + map + "]";
	}

}
