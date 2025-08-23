package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;

import java.util.Map;
import java.util.Objects;

public final class InsertVectorBuilder extends OperationBuilder {
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

    public LogicalTimestamp getVectorId() {
        return vectorId;
    }

    public Map<Integer, LogicalTimestamp> getMap() {
        return map;
    }

    @Override
    public InsertVector build(LogicalTimestamp operationId) {
        return new InsertVector(operationId, this.vectorId, this.map);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InsertVectorBuilder that = (InsertVectorBuilder) o;
        return Objects.equals(vectorId, that.vectorId) && Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorId, map);
    }
}
