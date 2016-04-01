package org.sagebionetworks.client;


import java.util.List;

import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for the Synapse Administration client.
 * 
 * @author jmhill
 *
 */
public interface SynapseAdminClient extends SynapseClient {

	/**
	 * Update the current stack status.
	 * @param updated
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public StackStatus updateCurrentStackStatus(StackStatus updated) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Get one page of metatdata for the given MigrationType
	 * 
	 * @param migrationType
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadata(MigrationType migrationType, Long limit, Long offset) throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * 
	 * @param type
	 * @param minId
	 * @param maxId
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadataByRange(MigrationType type, Long minId, Long maxId, Long limit, Long offset) throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Get the counts for all types
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCounts getTypeCounts() throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Get the counts for one type
	 * @param type
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCount getTypeCount(MigrationType type) throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Get the primary migration types
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeList getPrimaryTypes() throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Get the list of migration types
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeList getMigrationTypes() throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Delete a list of IDs
	 * 
	 * @param migrationType
	 * @param ids
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public MigrationTypeCount deleteMigratableObject(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Start a backup daemon task
	 * @param migrationType
	 * @param ids
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startBackup(MigrationType migrationType, IdList ids) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Start a restore daemon task
	 * @param migrationType
	 * @param req
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus startRestore(MigrationType migrationType, RestoreSubmission req) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Get the status of a daemon backup/restore task
	 * @param daemonId
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 */
	public BackupRestoreStatus getStatus(String daemonId) throws JSONObjectAdapterException, SynapseException;
	
	/**
	 * Get checksum for migration type and range of ids
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public MigrationRangeChecksum getChecksumForIdRange(MigrationType type, String salt, Long minId, Long maxId) throws SynapseException, JSONObjectAdapterException;
	

	/**
	 * Get checksum for migration type and range of ids
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	public MigrationTypeChecksum getChecksumForType(MigrationType type) throws SynapseException, JSONObjectAdapterException;
	

	/**
	 * Re-fires all changes messages with a change number greater than or equal to the given change number.
	 * @param startChangeNumber 
	 * @param limit - Limit the number of change messages fired.
	 * @return The last change number fired.
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException, JSONObjectAdapterException;

	/**
	 * Get the current change number.
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Creates a user with the specified state
	 * Minimally requires a username
	 * 
	 * @return The ID of the user
	 */
	public long createUser(NewIntegrationTestUser user) throws SynapseException, JSONObjectAdapterException;
	
	
	/**
	 * Deletes a user by ID
	 */
	public void deleteUser(Long id) throws SynapseException, JSONObjectAdapterException;

	/**
	 * Clears the specified dynamo table.
	 */
	public void clearDynamoTable(String tableName, String hashKeyName,
			String rangeKeyName) throws SynapseException;

	/**
	 * Clears the Synapse DOI table. Note this does not clear the DOIs registered outside Synapse.
	 */
	public void clearDoi() throws SynapseException;

	/**
	 * Gets everything in the trash can.
	 */
	public PaginatedResults<TrashedEntity> viewTrash(long offset, long limit)
			throws SynapseException;

	/**
	 * Purges everything in the trash can. All the entities in the trash will be permanently deleted.
	 */
	public void purgeTrash() throws SynapseException;

	public BackupRestoreStatus getDaemonStatus(String daemonId)
			throws SynapseException, JSONObjectAdapterException;

	/**
	 * List change messages.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 */
	public ChangeMessages listMessages(Long startChangeNumber, ObjectType type,
			Long limit) throws SynapseException, JSONObjectAdapterException;

	/**
	 * List change messages.
	 * @param queueName - The name of the queue to publishe the messages to.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 */
	public PublishResults publishChangeMessages(String queueName,
			Long startChangeNumber, ObjectType type, Long limit)
			throws SynapseException, JSONObjectAdapterException;

	/**
	 * Force a rebuild of a table's caches and indices
	 * 
	 * @param tableId
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public void rebuildTableCacheAndIndex(String tableId) throws SynapseException, JSONObjectAdapterException;

	/**
	 * Clear all semaphore locks.
	 * 
	 * @throws SynapseException
	 */
	void clearAllLocks() throws SynapseException;

	/**
	 * Don't return unless someone makes this call with release set to true. Only used for testing to emulate long
	 * running calls
	 * 
	 * @param release
	 * @throws SynapseException
	 */
	public void waitForTesting(boolean release) throws SynapseException;

	/**
	 * Create or update change messages
	 */
	public ChangeMessages createOrUpdateChangeMessages(ChangeMessages batch)
			throws SynapseException ;

	/**
	 * Force the server to throw a specific exception and return the status code
	 * 
	 * @param exception
	 * @param inTransaction
	 * @param inBeforeCommit
	 * @throws SynapseException
	 */
	public int throwException(String exceptionClassName, boolean inTransaction, boolean inBeforeCommit) throws SynapseException;
}
