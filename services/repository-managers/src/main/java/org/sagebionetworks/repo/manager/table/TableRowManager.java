package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 *
 */
public interface TableRowManager {
	
	/**
	 * Append a set of rows to a table.
	 * 
	 * @param user
	 * @param delta
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws IOException 
	 */
	public RowReferenceSet appendRows(UserInfo user, String tableId, List<ColumnModel> models, RowSet delta) throws DatastoreException, NotFoundException, IOException;
	
	
	/**
	 * Update the status of a table if the etag matches the expected value.
	 * 
	 * @param expectedEtag
	 * @param newStatus
	 * @return
	 * @throws ConflictingUpdateException Thrown when the current etag of the table does not match the passed etag. 
	 */
	public TableStatus updateTableStatus(String expectedEtag, TableStatus newStatus) throws ConflictingUpdateException;


	/**
	 * Get the current ColumnModel list for a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<ColumnModel> getColumnModelsForTable(String tableId);

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);


	/**
	 * Get a specific RowSet.
	 * 
	 * @param tableId
	 * @param rowVersion
	 * @return
	 */
	public RowSet getRowSet(String tableId, Long rowVersion);

}
