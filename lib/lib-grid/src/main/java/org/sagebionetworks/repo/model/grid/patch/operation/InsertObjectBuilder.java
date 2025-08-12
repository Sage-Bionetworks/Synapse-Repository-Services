package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertObjectBuilder implements OperationBuilder<InsertObject> {

	private LogicalTimestamp objectId;
	private Map<String, LogicalTimestamp> map;

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public InsertObjectBuilder setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	public Map<String, LogicalTimestamp> getMap() {
		return map;
	}

	public InsertObjectBuilder setMap(Map<String, LogicalTimestamp> map) {
		this.map = map;
		return this;
	}

	@Override
	public InsertObject build(LogicalTimestamp operationId) {
		return new InsertObject().setOperationId(operationId).setObjectId(objectId).setMap(map);
	}

	@Override
	public int hashCode() {
		return Objects.hash(map, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsertObjectBuilder other = (InsertObjectBuilder) obj;
		return Objects.equals(map, other.map) && Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "InsertObjectBuilder [objectId=" + objectId + ", map=" + map + "]";
	}

}
