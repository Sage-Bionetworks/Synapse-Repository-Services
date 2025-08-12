package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface OperationBuilder<T extends Operation<T>> {

	/**
	 * Build an operation of the provided type.
	 * 
	 * @return
	 */
	T build(LogicalTimestamp operationId);

}
