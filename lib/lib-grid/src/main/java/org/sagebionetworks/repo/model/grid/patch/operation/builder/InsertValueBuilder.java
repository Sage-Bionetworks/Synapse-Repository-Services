package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

public final class InsertValueBuilder extends OperationBuilder<InsertValue, InsertValueBuilder> {
    private LogicalTimestamp valueId;
    private LogicalTimestamp referenceId;


    public InsertValueBuilder withValueId(LogicalTimestamp arrayId) {
        this.valueId = arrayId;
        return this;
    }

    public InsertValueBuilder withReferenceId(LogicalTimestamp referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    @Override
    public InsertValue build(LogicalTimestamp operationId) {
        return new InsertValue(operationId, valueId, referenceId);
    }
}
