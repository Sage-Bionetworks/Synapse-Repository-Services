package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;

public final class NewArrayBuilder extends OperationBuilder<NewArray, NewArrayBuilder> {
    @Override
    public NewArray build(LogicalTimestamp operationId) {
        return new NewArray(operationId);
    }
}
