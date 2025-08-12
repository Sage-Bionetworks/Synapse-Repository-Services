package org.sagebionetworks.repo.model.grid.patch.operation.builder;


import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewValue;

public final class NewValueBuilder extends OperationBuilder<NewValue> {

    @Override
    public NewValue build(LogicalTimestamp operationId) {
        return new NewValue(operationId);
    }
}
