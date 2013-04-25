package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.migration.TypeCount;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the migration.
 * 
 * @author jmhill
 *
 */
public interface MigrationService {

	/**
	 * Get all of the type counts.
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	MigrationTypeCounts getTypeCounts(String userId) throws DatastoreException, NotFoundException;

	/**
	 * Get the pagainated row metadta for one type.
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	RowMetadataResult getRowMetadaForType(String userId, MigrationType type, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * This method is called on the destination stack to compare compare its metadata with the source stack metadata
	 * @param userId
	 * @param valueOf
	 * @param list
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	RowMetadataResult getRowMetadataDeltaForType(String userId,	MigrationType valueOf, List<String> list) throws DatastoreException, NotFoundException;

	/**
	 * Start the backup of the provided list of Migration type IDs.
	 * @param userId
	 * @param type
	 * @param list
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus startBackup(String userId, MigrationType type,	List<String> list) throws DatastoreException, NotFoundException;

	/**
	 * Start the restore of the provided file.
	 * @param userId
	 * @param type
	 * @param fileName
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus startRestore(String userId, MigrationType type, String fileName) throws DatastoreException, NotFoundException;

	/**
	 * Delete the migration objects identified in tha passed list.
	 * @param userId
	 * @param valueOf
	 * @param list
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	TypeCount delete(String userId, MigrationType valueOf, List<String> list) throws DatastoreException, NotFoundException;

	/**
	 * Get the status of either a restore or backup deamon
	 * @param userId
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus getStatus(String userId, String daemonId) throws DatastoreException, NotFoundException;

}
