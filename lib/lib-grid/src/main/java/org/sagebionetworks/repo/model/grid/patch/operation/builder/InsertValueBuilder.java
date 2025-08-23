package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

import java.util.Objects;

public final class InsertValueBuilder extends OperationBuilder {
    private LogicalTimestamp valueId;
    private LogicalTimestamp referenceId;


    public InsertValueBuilder setValueId(LogicalTimestamp arrayId) {
        this.valueId = arrayId;
        return this;
    }

    public InsertValueBuilder setReferenceId(LogicalTimestamp referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public LogicalTimestamp getValueId() {
        return valueId;
    }

    public LogicalTimestamp getReferenceId() {
        return referenceId;
    }

    @Override
    public InsertValue build(LogicalTimestamp operationId) {
        return new InsertValue(operationId, valueId, referenceId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InsertValueBuilder that = (InsertValueBuilder) o;
        return Objects.equals(valueId, that.valueId) && Objects.equals(referenceId, that.referenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueId, referenceId);
    }
}
