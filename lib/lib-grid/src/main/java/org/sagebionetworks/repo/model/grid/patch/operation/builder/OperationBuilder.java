package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

public abstract class OperationBuilder<T extends Operation<T>> {


    /**
     * Build an operation of the provided type.
     *
     * @return
     */
    public abstract T build(LogicalTimestamp operationId);
}

