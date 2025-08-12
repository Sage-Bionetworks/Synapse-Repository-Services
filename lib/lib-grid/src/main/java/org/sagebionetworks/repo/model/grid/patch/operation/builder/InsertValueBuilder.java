package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

public final class InsertValueBuilder extends OperationBuilder<InsertValue> {
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

    @Override
    public InsertValue build(LogicalTimestamp operationId) {
        return new InsertValue(operationId, valueId, referenceId);
    }
}
