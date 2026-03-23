package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.Collection;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface GridReplicaValidationManager {

	/**
	 * Validate the row changes associated with the provided vector IDs.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param connectionId
	 * @param changedVectorIds
	 */
	void validateChanges(String sessionId, Long replicaId, Collection<LogicalTimestamp> changedVectorIds);

	/**
	 * Validates the changes across all rows.
	 * @param sessionId
	 * @param replicaId
	 */
	void validateAllRows(String sessionId, Long replicaId);

}
