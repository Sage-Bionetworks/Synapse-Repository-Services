package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

// The abstract base builder
public abstract class OperationBuilder<T extends Operation<T>, B extends OperationBuilder<T, B>> {
    // Abstract method to be implemented by each concrete builder
    public abstract T build(LogicalTimestamp operationId);
}

