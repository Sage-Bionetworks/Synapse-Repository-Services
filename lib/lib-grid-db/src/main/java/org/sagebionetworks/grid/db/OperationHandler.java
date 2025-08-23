package org.sagebionetworks.grid.db;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public interface OperationHandler<T extends Operation> {

	/**
	 * The type of operation that a specific implementation can handle.
	 * 
	 * @return
	 */
	OperationType getOperationType();

	/**
	 * Handle a batch of Operation of the same type
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param batch
	 * @return Set of LogicalTimestamps for nodes that were actually changed
	 */
	Set<LogicalTimestamp> handleBatch(String sessionId, Long replicaId, List<T> batch);

}
