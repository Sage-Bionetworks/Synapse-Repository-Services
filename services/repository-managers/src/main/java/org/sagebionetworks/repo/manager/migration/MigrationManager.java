package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;

/**
 * Abstraction for the V2 migration manager.
 * 
 * @author John
 *
 */
public interface MigrationManager {

	/**
	 * The total number of rows in the table.
	 * @return
	 */
	public long getCount(UserInfo user, MigrationType type);
	
	/**
	 * The max(id) of the table
	 * @return
	 */
	public long getMaxId(UserInfo user, MigrationType type);
	
	/**
	 * List all row metadata in a paginated format. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param limit
	 * @param offset
	 * @return
	 */
	public RowMetadataResult getRowMetadaForType(UserInfo user, MigrationType type, long limit, long offset);
	
	/**
	 * Given a list of ID return the RowMetadata for each row that exist in the table.
	 * This method is used to detect changes between multiple stacks.  Only return values for IDs that
	 * exist in table.  Any missing RowMetadata in the result will be interpreted as a row that does not
	 * exist in table.
	 * 
	 * @param idList
	 * @return
	 */
	public RowMetadataResult getRowMetadataDeltaForType(UserInfo user, MigrationType type, List<Long> idList);
	
	/**
	 * Get a batch of objects to backup.
	 * @param clazz
	 * @param rowIds
	 * @return
	 */
	public void writeBackupBatch(UserInfo user, MigrationType type, List<Long> rowIds, OutputStream out);

	/**
	 * Create or update a batch.
	 * @param batch - batch of objects to create or update.
	 */
	public List<Long> createOrUpdateBatch(UserInfo user, MigrationType type, InputStream in);
	
	/**
	 * Delete objects by their IDs
	 * @param type
	 * @param idList
	 */
	public int deleteObjectsById(UserInfo user, MigrationType type, List<Long> idList);
	
	/**
	 * The list of primary migration types represents types that either stand-alone or are the owner's of other types.
	 * Migration is driven off this list as secondary types are migrated with their primary owners.
	 * @return
	 */
	public List<MigrationType> getPrimaryMigrationTypes(UserInfo user);
	
	/**
	 * If this object is the 'owner' of other object, then it is a primary type. All secondary types should be returned in their
	 * migration order.
	 * For example, if A owns B and B owns C (A->B->C) then A is the primary, and both B and C are secondary. For this case, return B followed by C.
	 * Both B and C must have a backup ID column that is a foreign key to the backup ID of A, as the IDs of A will drive the migration of B and C.
	 * @return
	 */
	public List<MigrationType> getSecondaryTypes(MigrationType type);
	
	/**
	 * This will clear all data in the database.
	 * @throws Exception 
	 */
	public void deleteAllData(UserInfo user) throws Exception;
	
}
