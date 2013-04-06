package org.sagebionetworks.repo.model.dbo.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;

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
	 * @param offest
	 * @return
	 */
	public QueryResults<RowMetadata> listRowMetadata(MigratableTableType type, long limit, long offest);
	
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
	 * Backup the given row IDs to the passed output stream.
	 * @param type
	 * @param rowIds
	 * @param out
	 */
	public void backupToStream(MigratableTableType type, List<String> rowIds, OutputStream out);
	

	/**
	 * Restore rows from an input stream.
	 * @param type
	 * @param in
	 */
	public void restoreFromStream(MigratableTableType type, InputStream in);
	
	/**
	 * Delete objects by their IDs
	 * @param type
	 * @param idList
	 */
	public int deleteObjectsById(MigratableTableType type, List<String> idList);
	
}
