package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

public class InsertObject implements Operation<InsertObject> {

	private final LogicalTimestamp operationId;
	private final LogicalTimestamp objectId;
	private final Map<String, LogicalTimestamp> map;

	public InsertObject(LogicalTimestamp operationId, LogicalTimestamp objectId, Map<String, LogicalTimestamp> map) {
		ValidateArgument.required(operationId, "operationId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(map, "map");
		if (map.isEmpty()) {
			// Writing an empty map creates an invalid patch that cannot be parsed by json-joy.
			// This is a requirement of the patch format.
			ValidateArgument.failRequirement("InsertObject must have a non-empty map");
		}
		this.operationId = operationId;
		this.objectId = objectId;
		this.map = map;
	}

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


	public LogicalTimestamp getObjectId() {
		return objectId;
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
