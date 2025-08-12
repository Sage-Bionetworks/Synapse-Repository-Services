package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;

public final class NewObjectBuilder extends OperationBuilder<NewObject> {
    @Override
    public NewObject build(LogicalTimestamp operationId) {
        return new NewObject(operationId);
    }
}
