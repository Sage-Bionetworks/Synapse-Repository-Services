package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.Collection;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

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

	/**
	 * Force a full revalidation of every row in the session. Used when the
	 * session's bound JSON schema changed.
	 *
	 * @param sessionId
	 * @throws RecoverableMessageException if the session's VALIDATION replica
	 *             connection is still being established; the caller should retry.
	 */
	void validateAfterSchemaChange(String sessionId) throws RecoverableMessageException;

}
