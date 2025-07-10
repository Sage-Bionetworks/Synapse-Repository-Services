package org.sagebionetworks.grid.db;

import java.util.List;

import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

public interface OperationDispatcher {
	
	void processAll(String sessionId, Long replicaId, List<Operation<?>> batch);

}
