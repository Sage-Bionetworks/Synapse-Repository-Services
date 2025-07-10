package org.sagebionetworks.grid.db;

import java.util.List;

import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public interface OperationHandler<T extends Operation<T>> {
	
	OperationType getOperationType();
	
	/**
	 * Handle a batch of Operation of the 
	 * @param batch
	 */
	void handleBatch(String sessionId, Long replicaId, List<T> batch);

}
