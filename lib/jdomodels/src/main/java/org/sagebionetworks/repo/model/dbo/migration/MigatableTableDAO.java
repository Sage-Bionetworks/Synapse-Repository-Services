package org.sagebionetworks.repo.model.dbo.migration;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;
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
public interface MigatableTableDAO {
	
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
	public <D extends DatabaseObject<D>> List<D> getBackupBatch(Class<? extends D> clazz, List<Long> rowIds);

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
	
}
