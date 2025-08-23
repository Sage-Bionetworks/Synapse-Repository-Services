package org.sagebionetworks.grid.db;

import java.util.Optional;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface ConstantProvider {
	
	/**
	 * Find an existing constant with the provided value if it exists.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param constant
	 * @return
	 */
	Optional<LogicalTimestamp> findExistingConstant(String sessionIdString, Long replicaId, String jsonValue);

}
