package org.sagebionetworks.repo.model.dbo.migration;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
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
	public long getCount();
	
	/**
	 * List all row metadata in a paginated format. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param limit
	 * @param offest
	 * @return
	 */
	public <T extends DatabaseObject<?>> QueryResults<RowMetadata> listRowMetadata(Class<? extends T> clazz, long limit, long offest);
	
	/**
	 * Given a list of ID return the RowMetadata for each row that exist in the table.
	 * This method is used to detect changes between multiple stacks.  Only return values for IDs that
	 * exist in table.  Any missing RowMetadata in the result will be interpreted as a row that does not
	 * exist in table.
	 * 
	 * @param idList
	 * @return
	 */
	public List<RowMetadata> listDeltaRowMetadata(List<String> idList);
	
	/**
	 * Given a list of ID return a list of DatabaseObject to be backed up.
	 * 
	 * @param clazz
	 * @param idList
	 * @return
	 */
	public <T extends DatabaseObject<T>> List<T> createListToBackup(Class<? extends T> clazz, List<String> idList);
	
	/**
	 * Givne a list of DatabaseObject batch creatt/update the rows in the database table.
	 * @param toRestore
	 * @return
	 */
	public <T extends DatabaseObject<T>> int batchCreateOrUpdateRows(List<T> toRestore);
	
}
