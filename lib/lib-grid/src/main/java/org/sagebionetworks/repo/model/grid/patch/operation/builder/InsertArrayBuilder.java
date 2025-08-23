package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;

import java.util.List;
import java.util.Objects;

public final class InsertArrayBuilder extends OperationBuilder {

    private LogicalTimestamp arrayId;
    private LogicalTimestamp referenceId;
    private List<LogicalTimestamp> elementIds;


    public InsertArrayBuilder setArrayId(LogicalTimestamp arrayId) {
        this.arrayId = arrayId;
        return this;
    }

    public LogicalTimestamp getArrayId() {
        return arrayId;
    }

    public InsertArrayBuilder setReferenceId(LogicalTimestamp referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public LogicalTimestamp getReferenceId() {
        return referenceId;
    }

    public InsertArrayBuilder setElementIds(List<LogicalTimestamp> elementIds) {
        this.elementIds = elementIds;
        return this;
    }

    public List<LogicalTimestamp> getElementIds() {
        return elementIds;
    }

    @Override
    public InsertArray build(LogicalTimestamp operationId) {
        return new InsertArray(operationId, arrayId, referenceId, elementIds);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InsertArrayBuilder that = (InsertArrayBuilder) o;
        return Objects.equals(arrayId, that.arrayId) && Objects.equals(referenceId, that.referenceId) && Objects.equals(elementIds, that.elementIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayId, referenceId, elementIds);
    }
}
