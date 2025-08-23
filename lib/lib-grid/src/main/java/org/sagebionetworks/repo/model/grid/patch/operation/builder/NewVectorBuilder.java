package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;

public final class NewVectorBuilder extends OperationBuilder {
    @Override
    public NewVector build(LogicalTimestamp operationId) {
        return new NewVector(operationId);
    }
}
