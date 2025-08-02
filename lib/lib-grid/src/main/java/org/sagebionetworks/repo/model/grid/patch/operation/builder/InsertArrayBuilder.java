package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;

import java.util.List;

public final class InsertArrayBuilder extends OperationBuilder<InsertArray, InsertArrayBuilder> {
    private LogicalTimestamp arrayId;
    private LogicalTimestamp referenceId;
    private List<LogicalTimestamp> elementIds;


    public InsertArrayBuilder withArrayId(LogicalTimestamp arrayId) {
        this.arrayId = arrayId;
        return this;
    }

    public InsertArrayBuilder withReferenceId(LogicalTimestamp referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public InsertArrayBuilder withElementIds(List<LogicalTimestamp> elementIds) {
        this.elementIds = elementIds;
        return this;
    }


    @Override
    public InsertArray build(LogicalTimestamp operationId) {
        return new InsertArray(operationId, arrayId, referenceId, elementIds);
    }
}
