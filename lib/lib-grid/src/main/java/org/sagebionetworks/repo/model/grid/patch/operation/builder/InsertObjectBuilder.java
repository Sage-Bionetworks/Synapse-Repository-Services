package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;

import java.util.Map;
import java.util.Objects;

public final class InsertObjectBuilder extends OperationBuilder {
    private LogicalTimestamp objectId;
    private Map<String, LogicalTimestamp> map;

    public InsertObjectBuilder setObjectId(LogicalTimestamp objectId) {
        this.objectId = objectId;
        return this;
    }

    public LogicalTimestamp getObjectId() {
        return objectId;
    }

    public InsertObjectBuilder setMap(Map<String, LogicalTimestamp> map) {
        this.map = map;
        return this;
    }

    public Map<String, LogicalTimestamp> getMap() {
        return map;
    }

    @Override
    public InsertObject build(LogicalTimestamp operationId) {
        return new InsertObject(operationId, this.objectId, this.map);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InsertObjectBuilder that = (InsertObjectBuilder) o;
        return Objects.equals(objectId, that.objectId) && Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, map);
    }

	@Override
	public String toString() {
		return "InsertObjectBuilder [objectId=" + objectId + ", map=" + map + "]";
	}
    
    
}
