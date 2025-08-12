package org.sagebionetworks.grid.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;

public interface OperationDispatcher {

	/**
	 * Process all operations in batches.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param batch
	 * @return Map of actual changes applied by IndexType.
	 */
	Map<IndexType, Set<LogicalTimestamp>> processAll(String sessionId, Long replicaId, List<Operation<?>> batch);

}
