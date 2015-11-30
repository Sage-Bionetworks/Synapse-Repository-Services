package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
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
	MigrationTypeCounts getTypeCounts(Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Get the paginated row metadata for one type.
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	RowMetadataResult getRowMetadaForType(Long userId, MigrationType type, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get the paginated row metadata for one type and id range.
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	RowMetadataResult getRowMetadaByRangeForType(Long userId, MigrationType type, long minId, long maxId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * This method is called on the destination stack to compare compare its metadata with the source stack metadata
	 * @param userId
	 * @param valueOf
	 * @param list
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	RowMetadataResult getRowMetadataDeltaForType(Long userId,	MigrationType valueOf, List<Long> list) throws DatastoreException, NotFoundException;

	/**
	 * Start the backup of the provided list of Migration type IDs.
	 * @param userId
	 * @param type
	 * @param list
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus startBackup(Long userId, MigrationType type,	List<Long> list) throws DatastoreException, NotFoundException;

	/**
	 * Start the restore of the provided file.
	 * @param userId
	 * @param type
	 * @param fileName
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus startRestore(Long userId, MigrationType type, String fileName) throws DatastoreException, NotFoundException;

	/**
	 * Delete the migration objects identified in tha passed list.
	 * @param userId
	 * @param valueOf
	 * @param list
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws Exception 
	 */
	MigrationTypeCount delete(Long userId, MigrationType valueOf, List<Long> list) throws DatastoreException, NotFoundException, Exception;

	/**
	 * Get the status of either a restore or backup deamon
	 * @param userId
	 * @param daemonId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	BackupRestoreStatus getStatus(Long userId, String daemonId) throws DatastoreException, NotFoundException;

	/**
	 * The list of primary migration types represents types that either stand-alone or are the owner's of other types.
	 * Migration is driven off this list as secondary types are migrated with their primary owners.
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	MigrationTypeList getPrimaryTypes(Long userId) throws DatastoreException, NotFoundException;
	
	/**
	 * A checksum for a range of ids and a migration type
	 * @throws NotFoundException 
	 */
	MigrationTypeChecksum getChecksumForIdRange(Long userId, MigrationType type, long minId, long maxId) throws NotFoundException;

}
