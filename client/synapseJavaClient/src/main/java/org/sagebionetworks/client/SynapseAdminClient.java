package org.sagebionetworks.client;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationTypeNames;
import org.sagebionetworks.repo.model.status.StackStatus;
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
	 * @throws SynapseException
	 */
	public StackStatus updateCurrentStackStatus(StackStatus updated) throws SynapseException;
	
	/**
	 * Get the counts for all types
	 * @return
	 * @throws SynapseException
	 */
	public MigrationTypeCounts getTypeCounts() throws SynapseException;
	
	/**
	 * Get the counts for one type
	 * @param type
	 * @return
	 * @throws SynapseException
	 */
	public MigrationTypeCount getTypeCount(MigrationType type) throws SynapseException;
	
	/**
	 * Get the primary migration types
	 * @return
	 * @throws SynapseException
	 */
	public MigrationTypeList getPrimaryTypes() throws SynapseException;
	
	/**
	 * Get the list of migration types
	 * @return
	 * @throws SynapseException
	 */
	public MigrationTypeList getMigrationTypes() throws SynapseException;

	/**
	 * Get the list of primary type names
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeNames getPrimaryTypeNames() throws SynapseException;

	/**
	 * Get the list of migration type names
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeNames getMigrationTypeNames() throws SynapseException;
		
	/**
	 * Get checksum for migration type and range of ids
	 * @throws SynapseException 
	 */
	public MigrationRangeChecksum getChecksumForIdRange(MigrationType type, String salt, Long minId, Long maxId) throws SynapseException;
	

	/**
	 * Get checksum for migration type and range of ids
	 * @throws SynapseException 
	 */
	public MigrationTypeChecksum getChecksumForType(MigrationType type) throws SynapseException;
	

	/**
	 * Re-fires all changes messages with a change number greater than or equal to the given change number.
	 * @param startChangeNumber 
	 * @param limit - Limit the number of change messages fired.
	 * @return The last change number fired.
	 * @throws SynapseException
	 */
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException;

	/**
	 * Get the current change number.
	 * @return
	 * @throws SynapseException
	 */
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException;
	
	/**
	 * Creates a user with the specified state
	 * Minimally requires a username
	 * 
	 * @return The ID of the user
	 */
	public long createUser(NewIntegrationTestUser user) throws SynapseException;
	
	
	/**
	 * Deletes a user by ID
	 */
	public void deleteUser(Long id) throws SynapseException;

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
	
	/**
	 * Purges trash without children trash items in the trash can.
	 * @param numDaysInTrash number of days the trash items must have been in the trash can
	 * @param limit number of trash items to delete
	 * @throws SynapseException
	 */
	public void purgeTrashLeaves(long numDaysInTrash, long limit) throws SynapseException;
	
	/**
	 * List change messages.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 */
	public ChangeMessages listMessages(Long startChangeNumber, ObjectType type,
			Long limit) throws SynapseException;

	/**
	 * List change messages.
	 * @param queueName - The name of the queue to publishe the messages to.
	 * @param startChangeNumber - The change number to start from.
	 * @param type - (optional) when included, only messages of this type will be listed.
	 * @param limit - (optional) limit the number of messages to fetch.
	 */
	public PublishResults publishChangeMessages(String queueName,
			Long startChangeNumber, ObjectType type, Long limit)
			throws SynapseException;

	/**
	 * Force a rebuild of a table's caches and indices
	 * 
	 * @param tableId
	 * @throws SynapseException
	 */
	public void rebuildTableCacheAndIndex(String tableId) throws SynapseException;

	/**
	 * Clear all semaphore locks.
	 * 
	 * @throws SynapseException
	 */
	void clearAllLocks() throws SynapseException;

	/**
	 * Create or update change messages
	 */
	public ChangeMessages createOrUpdateChangeMessages(ChangeMessages batch)
			throws SynapseException ;

	/**
	 * Start a new Asynchronous Job
	 * @param jobBody
	 * @return
	 * @throws SynapseException 
	 */
	public AsynchronousJobStatus startAdminAsynchronousJob(AsyncMigrationRequest migReq)
			throws SynapseException;

	/**
	 * Get the status of an Asynchronous Job from its ID.
	 * @param jobId
	 * @return
	 * @throws SynapseException 
	 */
	public AsynchronousJobStatus getAdminAsynchronousJobStatus(String jobId) throws SynapseException;
	
	/**
	 * Create an export of the ID generator.
	 * @return
	 * @throws SynapseException 
	 */
	public IdGeneratorExport createIdGeneratorExport() throws SynapseException;

}
