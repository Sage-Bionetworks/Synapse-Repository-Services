package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;

import java.util.Map;

public final class InsertVectorBuilder extends OperationBuilder<InsertVector> {
    private LogicalTimestamp vectorId;
    private Map<Integer, LogicalTimestamp> map;

    public InsertVectorBuilder setVectorId(LogicalTimestamp objectId) {
        this.vectorId = objectId;
        return this;
    }

    public InsertVectorBuilder setMap(Map<Integer, LogicalTimestamp> map) {
        this.map = map;
        return this;
    }

    @Override
    public InsertVector build(LogicalTimestamp operationId) {
        return new InsertVector(operationId, this.vectorId, this.map);
    }
}
