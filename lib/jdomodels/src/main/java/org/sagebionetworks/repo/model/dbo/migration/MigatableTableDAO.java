package org.sagebionetworks.repo.model.dbo.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

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
	public long getCount(MigratableTableType type);
	
	/**
	 * List all row metadata in a paginated format. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param limit
	 * @param offset
	 * @return
	 */
	public QueryResults<RowMetadata> listRowMetadata(MigratableTableType type, long limit, long offset);
	
	/**
	 * Given a list of ID return the RowMetadata for each row that exist in the table.
	 * This method is used to detect changes between multiple stacks.  Only return values for IDs that
	 * exist in table.  Any missing RowMetadata in the result will be interpreted as a row that does not
	 * exist in table.
	 * 
	 * @param idList
	 * @return
	 */
	public List<RowMetadata> listDeltaRowMetadata(MigratableTableType type, List<String> idList);
	
	/**
	 * Get a batch of objects to backup.
	 * @param clazz
	 * @param rowIds
	 * @return
	 */
	public <T extends DatabaseObject<T>> List<T> getBackupBatch(Class<? extends T> clazz, List<String> rowIds);

	/**
	 * Create or update a batch.
	 * @param batch - batch of objects to create or update.
	 */
	public <T extends DatabaseObject<T>> void createOrUpdateBatch(List<T> batch);
	
	/**
	 * Delete objects by their IDs
	 * @param type
	 * @param idList
	 */
	public int deleteObjectsById(MigratableTableType type, List<String> idList);
	
}
