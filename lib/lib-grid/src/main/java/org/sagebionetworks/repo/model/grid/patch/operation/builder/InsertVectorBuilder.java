package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.util.ValidateArgument;

import java.util.Map;

public final class InsertVectorBuilder extends OperationBuilder<InsertVector, InsertVectorBuilder> {
    private LogicalTimestamp vectorId;
    private Map<Integer, LogicalTimestamp> map;

    public InsertVectorBuilder withVectorId(LogicalTimestamp objectId) {
        this.vectorId = objectId;
        return this;
    }

    public InsertVectorBuilder withMap(Map<Integer, LogicalTimestamp> map) {
        this.map = map;
        return this;
    }

    @Override
    public InsertVector build(LogicalTimestamp operationId) {
        // If this is invalid, throw an exception or do nothing? The serializer catches invalid patches.
        // Or maybe the 'PatchBuilder' should know to skip empty inserts?
        ValidateArgument.required(operationId, "operationId");
        ValidateArgument.required(this.vectorId, "vectorId");
        ValidateArgument.required(this.map, "map");
        if (this.map.isEmpty()) {
            // Writing an empty map creates an invalid patch that cannot be parsed by json-joy.
            // This is a requirement of the patch format.
            ValidateArgument.failRequirement("InsertVector must have a non-empty map");
        }

        return new InsertVector(operationId, this.vectorId, this.map);
    }
}
