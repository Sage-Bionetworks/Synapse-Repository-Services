package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.util.ValidateArgument;

import java.util.Map;

public final class InsertObjectBuilder extends OperationBuilder<InsertObject, InsertObjectBuilder> {
    private LogicalTimestamp objectId;
    private Map<String, LogicalTimestamp> map;

    public InsertObjectBuilder withObjectId(LogicalTimestamp objectId) {
        this.objectId = objectId;
        return this;
    }

    public InsertObjectBuilder withMap(Map<String, LogicalTimestamp> map) {
        this.map = map;
        return this;
    }

    @Override
    public InsertObject build(LogicalTimestamp operationId) {
        // If this is invalid, throw an exception or do nothing? The serializer catches invalid patches.
        // Or maybe the 'PatchBuilder' should know to skip empty inserts?
        ValidateArgument.required(operationId, "operationId");
        ValidateArgument.required(this.objectId, "objectId");
        ValidateArgument.required(this.map, "map");
        if (this.map.isEmpty()) {
            // Writing an empty map creates an invalid patch that cannot be parsed by json-joy.
            // This is a requirement of the patch format.
            ValidateArgument.failRequirement("InsertObject must have a non-empty map");
        }

        return new InsertObject(operationId, this.objectId, this.map);
    }
}
