package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationTypeNames;
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
	 * Get type count for a Migration type
	 */
	MigrationTypeCount getTypeCount(Long userId, MigrationType type);

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
	 * The list of primary migration type names
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	MigrationTypeNames getPrimaryTypeNames(Long userId) throws DatastoreException, NotFoundException;

	/**
	 * The list of migrations types
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	MigrationTypeList getMigrationTypes(Long userId) throws DatastoreException, NotFoundException;

	/**
	 * The list of migration type names
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	MigrationTypeNames getMigrationTypeNames(Long userId) throws DatastoreException, NotFoundException;

	/**
	 * A checksum for a range of ids and a migration type
	 * @throws NotFoundException 
	 */
	MigrationRangeChecksum getChecksumForIdRange(Long userId, MigrationType type, String salt, long minId, long maxId) throws NotFoundException;
	
	/**
	 * A checksum for a type (table)
	 * 
	 * @param userId
	 * @param type
	 * @return
	 */
	MigrationTypeChecksum getChecksumForType(Long userId, MigrationType type) throws NotFoundException;

}
