package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.OperationBuilder;

public interface PatchBuilder {

	/**
	 * Add a new operation to be applied to a patch.
	 * 
	 * @param <T>
	 * @param builder
	 */
	<T extends Operation<T>> LogicalTimestamp addOperationBuilder(OperationBuilder<T> builder);
}
