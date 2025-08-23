package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;

public final class NewArrayBuilder extends OperationBuilder {
    @Override
    public NewArray build(LogicalTimestamp operationId) {
        return new NewArray(operationId);
    }
}
