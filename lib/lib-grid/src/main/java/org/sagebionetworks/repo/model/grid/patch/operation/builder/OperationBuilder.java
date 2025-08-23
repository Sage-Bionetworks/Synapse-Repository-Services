package org.sagebionetworks.repo.model.grid.patch.operation.builder;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

public abstract class OperationBuilder {


    /**
     * Build an operation of the provided type.
     *
     * @return
     */
    public abstract Operation build(LogicalTimestamp operationId);
}

