package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;

import java.util.Map;

public final class InsertObjectBuilder extends OperationBuilder<InsertObject> {
    private LogicalTimestamp objectId;
    private Map<String, LogicalTimestamp> map;

    public InsertObjectBuilder setObjectId(LogicalTimestamp objectId) {
        this.objectId = objectId;
        return this;
    }

    public InsertObjectBuilder setMap(Map<String, LogicalTimestamp> map) {
        this.map = map;
        return this;
    }

    @Override
    public InsertObject build(LogicalTimestamp operationId) {
        return new InsertObject(operationId, this.objectId, this.map);
    }
}
