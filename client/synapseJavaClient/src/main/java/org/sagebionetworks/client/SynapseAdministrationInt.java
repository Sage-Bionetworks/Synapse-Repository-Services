package org.sagebionetworks.client;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
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
public interface SynapseAdministrationInt extends SynapseInt {

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
	 * Get the counts for all types
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCounts getTypeCounts() throws SynapseException, JSONObjectAdapterException;
	
	/**
	 * Get the primary migration types
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeList getPrimaryTypes() throws SynapseException, JSONObjectAdapterException;
	
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
}
