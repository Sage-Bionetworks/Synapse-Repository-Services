package org.sagebionetworks.repo.model.dbo.migration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;

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
	 * List all row metadata in a paginated format. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param limit
	 * @param offset
	 * @return
	 */
	public RowMetadataResult listRowMetadata(MigrationType type, long limit, long offset);
	
	/**
	 * List row metadata in a paginated format for a given id range. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param minId
	 * @param maxId
	 * @param limit
	 * @param offset
	 * @return
	 */
	RowMetadataResult listRowMetadataByRange(MigrationType type, long minId, long maxId, long limit, long offset);
	
	/**
	 * Given a list of ID return the RowMetadata for each row that exist in the table.
	 * This method is used to detect changes between multiple stacks.  Only return values for IDs that
	 * exist in table.  Any missing RowMetadata in the result will be interpreted as a row that does not
	 * exist in table.
	 * 
	 * @param idList
	 * @return
	 */
	public List<RowMetadata> listDeltaRowMetadata(MigrationType type, List<Long> idList);
	
	/**
	 * Get a batch of objects to backup.
	 * @param clazz
	 * @param rowIds
	 * @return
	 */
	@Deprecated
	public <D extends DatabaseObject<D>> List<D> getBackupBatch(Class<? extends D> clazz, List<Long> rowIds);
	
	/**
	 * Stream over all DatabaseObject with backupIds matching the given IDs.
	 * @param clazz
	 * @param rowIds
	 * @param batchSize the maximum number of objects that should be loaded at a time.
	 * @return
	 */
	public Iterable<MigratableDatabaseObject<?,?>> streamDatabaseObjects(MigrationType type, List<Long> rowIds, long batchSize);
	
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
	 * Create or update a batch.
	 * @param batch - batch of objects to create or update.
	 */
	public <D extends DatabaseObject<D>> List<Long> createOrUpdateBatch(List<D> batch);
	
	/**
	 * Delete objects by their IDs
	 * @param type
	 * @param idList
	 */
	@Deprecated
	public int deleteObjectsById(MigrationType type, List<Long> idList);
	
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
	 * Run a method with foreign key constraints off.
	 * The global state of the database will be set to not check foreign key constraints
	 * while the passed callable is running.
	 * The foreign key constraint checking will unconditionally be re-enabled after the callable finishes.
	 * @param call
	 * @return
	 * @throws Exception
	 */
	public <T> T runWithForeignKeyIgnored(Callable<T> call) throws Exception;
	
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
	 * Delete all rows for a given type with the given backup ids.
	 * @param type
	 * @param idList
	 * @return
	 */
	int deleteById(MigrationType type, List<Long> idList);

}
