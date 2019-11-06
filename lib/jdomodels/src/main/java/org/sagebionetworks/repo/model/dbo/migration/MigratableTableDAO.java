package org.sagebionetworks.repo.model.dbo.migration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RangeChecksum;

/**
 * An abstraction for a Data Access Object (DAO) that can be used to migrate an single database table.
 * 
 * For performance reasons each method should be implemented as a single database call.
 * 
 * @author John
 *
 */
public interface MigratableTableDAO extends MigrationTypeProvider {
	
	/**
	 * The total number of rows in the table.
	 * @return
	 */
	public long getCount(MigrationType type);
	
	/**
	 * The current max(id) for a table
	 */
	public long getMaxId(MigrationType type);
	
	/**
	 * The current min(id) for a table
	 */
	public long getMinId(MigrationType type);
	
	/**
	 * MigrationTypeCount for given migration type
	 */
	public MigrationTypeCount getMigrationTypeCount(MigrationType type);
	
	/**
	 * A checksum on etag or id for a range of ids
	 */
	public String getChecksumForIdRange(MigrationType type, String salt, long minId, long maxId);
	
	/**
	 * A table checksum (CHECKSUM TABLE statement)
	 */
	public String getChecksumForType(MigrationType type);
		
	/**
	 * Stream over all of the database object for the given within the provided ID range.
	 * 
	 * @param migrationType
	 * @param minimumId Smallest ID in the range (inclusive).
	 * @param maximumId Largest ID in range (exclusive).
	 * @param batchSize
	 * @return
	 */
	public Iterable<MigratableDatabaseObject<?, ?>> streamDatabaseObjects(MigrationType migrationType, Long minimumId,
			Long maximumId, Long batchSize);
	
	
	/**
	 * Get the MigratableObjectType from 
	 * @param type
	 * @return
	 */
	public MigratableDatabaseObject getObjectForType(MigrationType type);
	
	/**
	 * The list of primary migration types represents types that either stand-alone or are the owner's of other types.
	 * Migration is driven off this list as secondary types are migrated with their primary owners.
	 * @return
	 */
	public List<MigrationType> getPrimaryMigrationTypes();
	
	/**
	 * Run a method with foreign key and uniqueness constraints checks off.
	 * The global state of the database changed to disable checks  while the passed callable is running.
	 * The key checking will unconditionally be re-enabled after the callable finishes.
	 * @param call
	 * @return
	 * @throws Exception
	 */
	public <T> T runWithKeyChecksIgnored(Callable<T> call) throws Exception;
	
	/**
	 * Checks if the migration type has been registered
	 * @param type
	 * @return
	 */
	public boolean isMigrationTypeRegistered(MigrationType type);
	
	/**
	 * List all non-restricted foreign keys in the schema.
	 * @return
	 */
	public List<ForeignKeyInfo> listNonRestrictedForeignKeys();


	/**
	 * Map each secondary table name to all of the table names within its primary group.
	 * Secondary tables can have restricted foreign keys to any table within their primary group.
	 * @return
	 */
	Map<String, Set<String>> mapSecondaryTablesToPrimaryGroups();

	/**
	 * Create or update a batch of database objects
	 * @param batch
	 * @return
	 * @throws Exception 
	 */
	public List<Long> createOrUpdate(MigrationType type, List<DatabaseObject<?>> batch);

	/**
	 * Delete all rows of the given type and row ID range.
	 * @param type
	 * @param minimumId inclusive
	 * @param maximumId exclusive
	 */
	public int deleteByRange(MigrationType type, long minimumId, long maximumId);

	/**
	 * Calculate the ID ranges with the optimal number of rows for the given type.
	 * @param migrationType
	 * @param minimumId
	 * @param maximumId
	 * @param optimalNumberOfRows
	 * @return
	 */
	public List<IdRange> calculateRangesForType(MigrationType migrationType, long minimumId, long maximumId, long optimalNumberOfRows);

	/**
	 * Get the SQL used for primary cardinality.
	 * @param node
	 * @return
	 */
	public String getPrimaryCardinalitySql(MigrationType node);

	/**
	 * Calculate a batch of checksums for the given type and ID range.
	 * @param request
	 * @return
	 */
	public List<RangeChecksum> calculateBatchChecksums(BatchChecksumRequest request);

}
